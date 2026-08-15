package com.jarvis.mark39.ai

import com.jarvis.mark39.data.repository.SettingsRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqClient @Inject constructor(
    private val settings: SettingsRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun chat(
        userText: String,
        systemPrompt: String,
        maxTokens: Int = 1024
    ): String {
        val key = settings.getGroqApiKey()
        if (key.isBlank()) return "Groq API key not set."
        val model = settings.getGroqModel().ifBlank { "llama-3.3-70b-versatile" }

        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemPrompt))
            .put(JSONObject().put("role", "user").put("content", userText))

        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.7)
            .put("max_tokens", maxTokens)
            .toString()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return "Groq error HTTP ${resp.code}: ${raw.take(200)}"
                }
                val json = JSONObject(raw)
                json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    ?.ifBlank { "Empty Groq response." }
                    ?: "Empty Groq response."
            }
        } catch (e: Exception) {
            "Groq error: ${e.message}"
        }
    }
}
