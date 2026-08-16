package com.jarvis.mark39.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.mark39.ai.AgentLoop
import com.jarvis.mark39.ai.GeminiClient
import com.jarvis.mark39.ai.OpenRouterClient
import com.jarvis.mark39.ai.LlmRouter
import com.jarvis.mark39.ai.GroqClient
import com.jarvis.mark39.ai.ToolRegistry
import com.jarvis.mark39.ai.VoiceCommandRouter
import com.jarvis.mark39.data.local.SessionEntity
import com.jarvis.mark39.data.repository.ChatRepository
import com.jarvis.mark39.data.repository.MemoryRepository
import com.jarvis.mark39.data.repository.CustomSkillRepository
import com.jarvis.mark39.data.local.CustomSkillEntity
import com.jarvis.mark39.domain.model.MemoryItem
import com.jarvis.mark39.data.repository.SettingsRepository
import com.jarvis.mark39.domain.model.ChatMessage
import com.jarvis.mark39.domain.model.JarvisUiEvent
import com.jarvis.mark39.domain.model.MessageRole
import com.jarvis.mark39.domain.model.VoiceState
import com.jarvis.mark39.service.OverlayBubbleService
import com.jarvis.mark39.service.ScreenVisionPipeline
import com.jarvis.mark39.service.VoiceService
import com.jarvis.mark39.util.FileAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JarvisUiState(
    val voiceState: VoiceState = VoiceState.IDLE,
    val partialTranscript: String = "",
    val isProcessing: Boolean = false,
    val activityStatus: String? = null,
    val activityLog: List<String> = emptyList(),
    val lastProvider: String? = null,
    val error: String? = null,
    val hasApiKey: Boolean = false,
    val overlayActive: Boolean = false,
    val screenVisionActive: Boolean = false
)

@HiltViewModel
class JarvisViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val gemini: GeminiClient,
    private val openRouter: OpenRouterClient,
    private val llmRouter: LlmRouter,
    private val groq: GroqClient,
    private val chatRepository: ChatRepository,
    private val settings: SettingsRepository,
    private val memoryRepository: MemoryRepository,
    private val customSkillRepository: CustomSkillRepository,
    private val voiceService: VoiceService,
    private val agentLoop: AgentLoop,
    private val fileAnalyzer: FileAnalyzer,
    private val toolRegistry: ToolRegistry,
    private val voiceCommands: VoiceCommandRouter,
    private val screenVision: ScreenVisionPipeline
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository.observeMessages()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<SessionEntity>> = chatRepository.sessions
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSessionId: StateFlow<String> = chatRepository.currentSessionId

    val customSkills = customSkillRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(JarvisUiState(hasApiKey = settings.hasApiKey()))
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    private val _visionResult = MutableStateFlow("")
    val visionResult: StateFlow<String> = _visionResult.asStateFlow()

    @Volatile private var lastCameraFrame: Bitmap? = null

    init {
        viewModelScope.launch { chatRepository.ensureDefaultSession() }
        viewModelScope.launch {
            voiceService.isListening.collect { listening ->
                if (listening) {
                    _uiState.value = _uiState.value.copy(voiceState = VoiceState.LISTENING)
                }
            }
        }
        viewModelScope.launch {
            voiceService.partialText.collect { text ->
                _uiState.value = _uiState.value.copy(partialTranscript = text)
            }
        }
        viewModelScope.launch {
            screenVision.lastAnalysis.collect { text ->
                _visionResult.value = text
                chatRepository.addMessage(
                    ChatMessage(role = MessageRole.ASSISTANT, content = "🖥️ Screen: $text")
                )
            }
        }
        viewModelScope.launch {
            screenVision.isActive.collect { active ->
                _uiState.value = _uiState.value.copy(screenVisionActive = active)
            }
        }
    }

    fun onEvent(event: JarvisUiEvent) {
        when (event) {
            is JarvisUiEvent.SendText -> sendText(event.text)
            JarvisUiEvent.StartListening -> startVoice()
            JarvisUiEvent.StopListening -> voiceService.stopListening()
            is JarvisUiEvent.ShareFile -> handleSharedFile(event.uri)
            is JarvisUiEvent.RunTask -> runAgentTask(event.goal)
            JarvisUiEvent.ClearChat -> viewModelScope.launch { chatRepository.clear() }
        }
    }

    private fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            if (!settings.hasApiKey()) {
                _uiState.value = _uiState.value.copy(error = "Please set your Gemini API key in Settings.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isProcessing = true, error = null, voiceState = VoiceState.PROCESSING, activityStatus = "Thinking…", activityLog = (_uiState.value.activityLog + "Thinking…").takeLast(8)
            )
            chatRepository.addMessage(ChatMessage(role = MessageRole.USER, content = text))
            try {
                // 1) Fast local voice commands (phone control)
                val local = voiceCommands.tryHandle(text)
                val reply = if (local != null) {
                    _uiState.value = _uiState.value.copy(
                        activityStatus = "Ran command: ${text.take(40)}",
                        activityLog = (_uiState.value.activityLog + "Ran command → ${local.take(60)}").takeLast(8)
                    )
                    local
                } else {
                    // 2) Gemini + function calling for general / complex
                    run {
                        _uiState.value = _uiState.value.copy(activityStatus = "Connecting to AI…", activityLog = (_uiState.value.activityLog + "Connecting to AI…").takeLast(8))
                        val result = llmRouter.chat(text)
                        _uiState.value = _uiState.value.copy(
                            activityStatus = "Writing reply…", activityLog = (_uiState.value.activityLog + "Writing reply…").takeLast(8),
                            lastProvider = result.provider
                        )
                        result.text
                    }
                }
                chatRepository.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = reply))
                if (local == null) {
                    val prov = _uiState.value.lastProvider ?: "ai"
                    _uiState.value = _uiState.value.copy(
                        activityLog = (_uiState.value.activityLog + "Reply via $prov").takeLast(8)
                    )
                }
                _uiState.value = _uiState.value.copy(voiceState = VoiceState.SPEAKING)
                voiceService.speak(reply) {
                    _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                chatRepository.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = "Error: $msg"))
                _uiState.value = _uiState.value.copy(error = msg, voiceState = VoiceState.ERROR)
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false, activityStatus = null)
            }
        }
    }

    private fun startVoice() {
        if (!settings.hasApiKey()) {
            _uiState.value = _uiState.value.copy(error = "Set Gemini API key first.")
            return
        }
        _uiState.value = _uiState.value.copy(voiceState = VoiceState.LISTENING, error = null)
        voiceService.startListening { transcript ->
            if (transcript.isNotBlank()) sendText(transcript)
            else _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
        }
    }

    private fun handleSharedFile(uriString: String) {
        viewModelScope.launch {
            if (!settings.hasApiKey()) {
                chatRepository.addMessage(
                    ChatMessage(role = MessageRole.ASSISTANT, content = "Set Gemini API key to analyze files.")
                )
                return@launch
            }
            val uri = Uri.parse(uriString)
            chatRepository.addMessage(
                ChatMessage(
                    role = MessageRole.USER,
                    content = "Analyze this file",
                    attachments = listOf(uriString)
                )
            )
            _uiState.value = _uiState.value.copy(isProcessing = true, activityStatus = "Analyzing…")
            try {
                val result = fileAnalyzer.analyze(uri, gemini)
                chatRepository.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = result))
                voiceService.speak(result.take(400))
            } catch (e: Exception) {
                chatRepository.addMessage(
                    ChatMessage(role = MessageRole.ASSISTANT, content = "File analysis failed: ${e.message}")
                )
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false, activityStatus = null)
            }
        }
    }

    private fun runAgentTask(goal: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, activityStatus = "Analyzing…")
            chatRepository.addMessage(ChatMessage(role = MessageRole.USER, content = "Task: $goal"))
            try {
                val result = agentLoop.execute(goal)
                val body = buildString {
                    appendLine("Task ${result.status}")
                    result.steps.takeLast(8).forEach { appendLine("• $it") }
                    appendLine()
                    append(result.result ?: "")
                }
                chatRepository.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = body))
            } catch (e: Exception) {
                chatRepository.addMessage(
                    ChatMessage(role = MessageRole.ASSISTANT, content = "Agent failed: ${e.message}")
                )
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false, activityStatus = null)
            }
        }
    }

    // --- Vision ---
    fun onCameraFrame(bitmap: Bitmap) {
        lastCameraFrame = bitmap
    }

    fun analyzeLastCameraFrame() {
        val frame = lastCameraFrame ?: run {
            _visionResult.value = "No camera frame yet — wait a second and try again."
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, activityStatus = "Analyzing…")
            try {
                val result = gemini.analyzeImage(
                    frame,
                    "Real-time camera frame. Describe what you see and any actionable insights."
                )
                _visionResult.value = result
                chatRepository.addMessage(
                    ChatMessage(role = MessageRole.ASSISTANT, content = "📷 Camera: $result")
                )
            } catch (e: Exception) {
                _visionResult.value = "Camera analysis error: ${e.message}"
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false, activityStatus = null)
            }
        }
    }

    fun screenCaptureIntent(): Intent = screenVision.createCaptureIntent()

    fun startScreenVision(result: ActivityResult) {
        screenVision.startFromActivityResult(result)
        _uiState.value = _uiState.value.copy(screenVisionActive = true)
    }

    fun stopScreenVision() {
        screenVision.stop()
        _uiState.value = _uiState.value.copy(screenVisionActive = false)
    }

    // --- Overlay bubble ---
    fun canDrawOverlays(): Boolean =
        Settings.canDrawOverlays(appContext)

    fun overlayPermissionIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun startOverlayBubble() {
        if (!canDrawOverlays()) return
        appContext.startForegroundService(Intent(appContext, OverlayBubbleService::class.java))
        _uiState.value = _uiState.value.copy(overlayActive = true)
    }

    fun stopOverlayBubble() {
        appContext.stopService(Intent(appContext, OverlayBubbleService::class.java))
        _uiState.value = _uiState.value.copy(overlayActive = false)
    }

    
    
    fun createSession() {
        viewModelScope.launch {
            chatRepository.createSession()
            gemini.resetChat()
            groq.resetHistory()
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    fun switchSession(id: String) {
        viewModelScope.launch {
            chatRepository.switchSession(id)
            gemini.resetChat()
            groq.resetHistory()
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { chatRepository.deleteSession(id) }
    }

    fun newChatSession() {
        viewModelScope.launch {
            chatRepository.createSession()
            gemini.resetChat()
            groq.resetHistory()
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    
    fun exportChat(): String {
        val msgs = messages.value
        return buildString {
            appendLine("JARVIS Chat Export")
            appendLine("------------------")
            msgs.forEach { m ->
                appendLine("[${m.role}] ${m.content}")
            }
        }
    }

    
    suspend fun loadMemories(): List<MemoryItem> = memoryRepository.getRecent(100)

    suspend fun saveMemory(fact: String) {
        memoryRepository.storeFact(fact, "user")
    }

    suspend fun deleteMemory(id: String) {
        memoryRepository.delete(id)
    }

    private fun setActivity(status: String?) {
        val log = if (status != null) {
            (_uiState.value.activityLog + status).takeLast(8)
        } else _uiState.value.activityLog
        _uiState.value = _uiState.value.copy(activityStatus = status, activityLog = log)
    }

    suspend fun addCustomSkill(name: String, description: String, instructions: String) {
        customSkillRepository.add(name, description, instructions)
    }

    suspend fun deleteCustomSkill(id: String) = customSkillRepository.delete(id)

    suspend fun setCustomSkillEnabled(id: String, enabled: Boolean) =
        customSkillRepository.setEnabled(id, enabled)

    fun clearActivityLog() {
        _uiState.value = _uiState.value.copy(activityLog = emptyList(), activityStatus = null)
    }

    fun refreshApiKeyStatus() {
        _uiState.value = _uiState.value.copy(hasApiKey = settings.hasApiKey())
        if (settings.hasApiKey()) gemini.resetChat()
        groq.resetHistory()
    }

    override fun onCleared() {
        voiceService.release()
        super.onCleared()
    }
}