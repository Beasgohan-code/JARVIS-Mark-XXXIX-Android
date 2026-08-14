package com.jarvis.mark39.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.jarvis.mark39.data.repository.MemoryRepository
import com.jarvis.mark39.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini chat + vision client.
 * Device tools are handled by [VoiceCommandRouter] and [AgentLoop] (text actions),
 * not SDK FunctionDeclaration (API differs across generativeai versions).
 */
@Singleton
class GeminiClient @Inject constructor(
    private val settings: SettingsRepository,
    private val memoryRepository: MemoryRepository
) {
    private var model: GenerativeModel? = null
    private var chatSession: com.google.ai.client.generativeai.Chat? = null

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
                systemInstruction = content { text(SYSTEM_PROMPT) }
            )
            chatSession = model!!.startChat()
        }
        return model!!
    }

    fun resetChat() {
        model = null
        chatSession = null
    }

    suspend fun sendMessage(userText: String, extraContext: String = ""): String =
        withContext(Dispatchers.IO) {
            val m = ensureModel()
            val memoryCtx = memoryRepository.getContextForPrompt()
            val fullPrompt = buildString {
                if (memoryCtx.isNotBlank()) append(memoryCtx).append("\n\n")
                if (extraContext.isNotBlank()) append(extraContext).append("\n\n")
                append(userText)
            }
            val response = chatSession?.sendMessage(fullPrompt) ?: m.generateContent(fullPrompt)
            response.text?.trim().orEmpty().ifBlank { "I could not generate a response." }
        }

    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String = "Describe what you see and suggest useful actions."
    ): String = withContext(Dispatchers.IO) {
        val response = ensureModel().generateContent(
            content {
                image(bitmap)
                text(prompt)
            }
        )
        response.text?.trim().orEmpty()
    }

    suspend fun analyzeScreen(bitmap: Bitmap): String = analyzeImage(
        bitmap,
        "Android screen. Describe the UI, extract visible text, and suggest 2-3 actions."
    )

    suspend fun analyzeImages(bitmaps: List<Bitmap>, prompt: String): String =
        withContext(Dispatchers.IO) {
            if (bitmaps.isEmpty()) return@withContext "No images."
            val response = ensureModel().generateContent(
                content {
                    bitmaps.forEach { image(it) }
                    text(prompt)
                }
            )
            response.text?.trim().orEmpty()
        }

    companion object {
        private const val SYSTEM_PROMPT = """
You are JARVIS Mark XXXIX on Android — helpful, concise, slightly witty.
You help control the phone via the user's voice commands (home, open apps, volume, search, etc.).
Be clear and short when confirming actions.
""".trimIndent()
    }
}
