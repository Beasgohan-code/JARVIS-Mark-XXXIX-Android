package com.jarvis.mark39.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.FunctionDeclaration
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.jarvis.mark39.data.repository.MemoryRepository
import com.jarvis.mark39.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(
    private val settings: SettingsRepository,
    private val memoryRepository: MemoryRepository,
    private val toolRegistry: dagger.Lazy<ToolRegistry>
) {
    private var model: GenerativeModel? = null
    private var chatSession: com.google.ai.client.generativeai.Chat? = null

    private fun str(desc: String) = Schema.str(desc)
    private fun obj(props: Map<String, Schema>, required: List<String> = emptyList()) =
        Schema.obj(props, required = required)

    private fun buildFunctionDeclarations(): List<FunctionDeclaration> = listOf(
        FunctionDeclaration("web_search", "Search the web for current info", obj(mapOf("query" to str("Search query")), listOf("query"))),
        FunctionDeclaration("remember", "Store a fact in memory", obj(mapOf("fact" to str("Fact to store")), listOf("fact"))),
        FunctionDeclaration("recall", "Search memory", obj(mapOf("query" to str("Memory query")), listOf("query"))),
        FunctionDeclaration("launch_app", "Launch app by package name", obj(mapOf("package" to str("Package name")), listOf("package"))),
        FunctionDeclaration("open_app", "Open app by display name", obj(mapOf("name" to str("App name")), listOf("name"))),
        FunctionDeclaration("open_url", "Open URL", obj(mapOf("url" to str("URL")), listOf("url"))),
        FunctionDeclaration("list_apps", "List installed apps", obj(emptyMap())),
        FunctionDeclaration("device_home", "Press Home", obj(emptyMap())),
        FunctionDeclaration("device_back", "Press Back", obj(emptyMap())),
        FunctionDeclaration("device_recents", "Open Recents", obj(emptyMap())),
        FunctionDeclaration("device_notifications", "Open notification shade", obj(emptyMap())),
        FunctionDeclaration("device_quick_settings", "Open quick settings", obj(emptyMap())),
        FunctionDeclaration("device_lock", "Lock the screen", obj(emptyMap())),
        FunctionDeclaration("device_screenshot", "Take a screenshot", obj(emptyMap())),
        FunctionDeclaration("open_settings", "Open Settings", obj(mapOf("section" to str("wifi|bluetooth|accessibility|apps|display|sound")))),
        FunctionDeclaration("click_text", "Tap UI element containing text", obj(mapOf("text" to str("Visible text")), listOf("text"))),
        FunctionDeclaration("type_text", "Type into focused field", obj(mapOf("text" to str("Text to type")), listOf("text"))),
        FunctionDeclaration("scroll", "Scroll the screen", obj(mapOf("direction" to str("up|down")), listOf("direction"))),
        FunctionDeclaration("swipe", "Swipe gesture", obj(mapOf("direction" to str("up|down|left|right")), listOf("direction"))),
        FunctionDeclaration("read_screen", "Read visible text on screen", obj(emptyMap())),
        FunctionDeclaration("call", "Call a number", obj(mapOf("number" to str("Phone number")), listOf("number"))),
        FunctionDeclaration("dial", "Open dialer", obj(mapOf("number" to str("Optional number")))),
        FunctionDeclaration("sms", "Open SMS compose", obj(mapOf("number" to str("Number"), "body" to str("Message")))),
        FunctionDeclaration("maps", "Open maps search", obj(mapOf("query" to str("Place or address")), listOf("query"))),
        FunctionDeclaration("contacts", "Open contacts", obj(emptyMap())),
        FunctionDeclaration("camera", "Open camera", obj(emptyMap())),
        FunctionDeclaration("alarm", "Set an alarm", obj(mapOf("hour" to str("0-23"), "minute" to str("0-59"), "message" to str("Label")), listOf("hour", "minute"))),
        FunctionDeclaration("volume", "Volume control", obj(mapOf("action" to str("up|down|mute|0-100")), listOf("action"))),
        FunctionDeclaration("wifi_settings", "Open Wi-Fi settings", obj(emptyMap())),
        FunctionDeclaration("bluetooth_settings", "Open Bluetooth settings", obj(emptyMap())),
        FunctionDeclaration("share_text", "Share text", obj(mapOf("text" to str("Text")), listOf("text")))
    )

    private fun ensureModel(): GenerativeModel {
        val key = settings.getGeminiApiKey()
        require(key.isNotBlank()) { "Gemini API key is missing. Set it in Settings." }
        if (model == null) {
            model = GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = key,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 2048
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
                ),
                systemInstruction = content { text(SYSTEM_PROMPT) },
                tools = listOf(Tool(functionDeclarations = buildFunctionDeclarations()))
            )
            chatSession = model!!.startChat()
        }
        return model!!
    }

    fun resetChat() { model = null; chatSession = null }

    suspend fun sendMessage(userText: String, extraContext: String = "", maxToolRounds: Int = 6): String =
        withContext(Dispatchers.IO) {
            val m = ensureModel()
            val memoryCtx = memoryRepository.getContextForPrompt()
            val fullPrompt = buildString {
                if (memoryCtx.isNotBlank()) append(memoryCtx).append("\n\n")
                if (extraContext.isNotBlank()) append(extraContext).append("\n\n")
                append(userText)
            }
            var response = chatSession?.sendMessage(fullPrompt) ?: m.generateContent(fullPrompt)
            var rounds = 0
            while (rounds < maxToolRounds) {
                val functionCalls = response.functionCalls
                if (functionCalls.isEmpty()) break
                val functionResponses = functionCalls.map { call ->
                    val args = mutableMapOf<String, String>()
                    call.args.forEach { (k, v) -> args[k] = v.toString() }
                    val result = toolRegistry.get().execute(call.name, args)
                    FunctionResponsePart(call.name, JSONObject().put("result", result))
                }
                val responseContent = content { functionResponses.forEach { part(it) } }
                response = chatSession?.sendMessage(responseContent) ?: m.generateContent(responseContent)
                rounds++
            }
            response.text?.trim().orEmpty().ifBlank { "Done." }
        }

    suspend fun analyzeImage(bitmap: Bitmap, prompt: String = "Describe what you see and suggest useful actions."): String =
        withContext(Dispatchers.IO) {
            val response = ensureModel().generateContent(content { image(bitmap); text(prompt) })
            response.text?.trim().orEmpty()
        }

    suspend fun analyzeScreen(bitmap: Bitmap): String = analyzeImage(
        bitmap,
        "Android screen. Describe UI, extract text, suggest 2-3 actions JARVIS can take (click, open, scroll)."
    )

    suspend fun analyzeImages(bitmaps: List<Bitmap>, prompt: String): String =
        withContext(Dispatchers.IO) {
            if (bitmaps.isEmpty()) return@withContext "No images."
            val response = ensureModel().generateContent(content {
                bitmaps.forEach { image(it) }
                text(prompt)
            })
            response.text?.trim().orEmpty()
        }

    companion object {
        private const val SYSTEM_PROMPT = """
You are JARVIS Mark XXXIX on Android — helpful, concise, slightly witty.
You can control the phone with tools: open apps, Home/Back/Recents, notifications, click on-screen text, type, scroll, swipe, read screen, call/SMS/maps, volume, settings, web search, memory.
Prefer tools over guessing. For destructive actions (call, lock), confirm intent from the user phrase.
When the user speaks a command, execute it with tools then give a short spoken confirmation.
""".trimIndent()
    }
}
