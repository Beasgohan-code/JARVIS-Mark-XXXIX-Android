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

@Singleton
class GeminiClient @Inject constructor(
    private val settings: SettingsRepository,
    private val memoryRepository: MemoryRepository
) {
    private var model: GenerativeModel? = null
    private var chatSession: com.google.ai.client.generativeai.Chat? = null
    private var boundModelName: String? = null

    private fun ensureModel(): GenerativeModel {
        val key = settings.getGeminiApiKey()
        require(key.isNotBlank()) { "Gemini API key is missing. Set it in Settings." }

        val modelName = settings.getGeminiModel().ifBlank { DEFAULT_MODEL }

        if (model == null || boundModelName != modelName) {
            model = GenerativeModel(
                modelName = modelName,
                apiKey = key,
                generationConfig = generationConfig {
                    temperature = 0.7f
                    maxOutputTokens = 8192
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
                ),
                systemInstruction = content { text(settings.resolveSystemPrompt()) }
            )
            chatSession = model!!.startChat()
            boundModelName = modelName
        }
        return model!!
    }

    fun resetChat() {
        model = null
        chatSession = null
        boundModelName = null
    }

    suspend fun sendMessage(userText: String, extraContext: String = ""): String =
        withContext(Dispatchers.IO) {
            try {
                val m = ensureModel()
                val memoryCtx = memoryRepository.getContextForPrompt()
                val fullPrompt = buildString {
                    if (memoryCtx.isNotBlank()) append(memoryCtx).append("\n\n")
                    if (extraContext.isNotBlank()) append(extraContext).append("\n\n")
                    append(userText)
                }
                val response = chatSession?.sendMessage(fullPrompt) ?: m.generateContent(fullPrompt)
                response.text?.trim().orEmpty().ifBlank { "I could not generate a response." }
            } catch (e: Exception) {
                throw RuntimeException(friendlyError(e), e)
            }
        }

    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String = "Describe what you see and suggest useful actions."
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = ensureModel().generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            response.text?.trim().orEmpty()
        } catch (e: Exception) {
            throw RuntimeException(friendlyError(e), e)
        }
    }

    suspend fun analyzeScreen(bitmap: Bitmap): String = analyzeImage(
        bitmap,
        "Android screen. Describe the UI, extract visible text, and suggest 2-3 actions."
    )

    suspend fun analyzeImages(bitmaps: List<Bitmap>, prompt: String): String =
        withContext(Dispatchers.IO) {
            if (bitmaps.isEmpty()) return@withContext "No images."
            try {
                val response = ensureModel().generateContent(
                    content {
                        bitmaps.forEach { image(it) }
                        text(prompt)
                    }
                )
                response.text?.trim().orEmpty()
            } catch (e: Exception) {
                throw RuntimeException(friendlyError(e), e)
            }
        }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro",
            "gemini-3.5-flash",
            "gemini-3.6-flash"
        )

                private val SYSTEM_PROMPT = SystemPrompts.defaultBody()

        fun friendlyError(e: Throwable): String {
            val raw = e.message.orEmpty() + " " + (e.cause?.message.orEmpty())
            return when {
                raw.contains("API key", ignoreCase = true) ||
                    raw.contains("API_KEY", ignoreCase = true) ||
                    raw.contains("401") || raw.contains("403") ->
                    "Invalid or missing Gemini API key. Check Settings."
                raw.contains("no longer available", ignoreCase = true) ||
                    raw.contains("NOT_FOUND") || raw.contains("404") ->
                    "Model not available. Open Settings and pick another model (e.g. gemini-2.5-flash)."
                raw.contains("RESOURCE_EXHAUSTED") || raw.contains("429") ->
                    "Rate limit hit. Wait a minute and try again."
                raw.contains("MAX_TOKENS", ignoreCase = true) ||
                    raw.contains("max tokens", ignoreCase = true) ->
                    "Reply hit the length limit. Ask for a shorter answer, or split the file into parts."
                raw.contains("SAFETY", ignoreCase = true) ->
                    "Response blocked by safety filters. Rephrase your request."
                raw.isBlank() -> "Network or Gemini error. Check internet and try again."
                else -> raw.take(280)
            }
        }
    }
}
