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

/**
 * Groq OpenAI-compatible chat — ultra-fast inference.
 * Docs: https://console.groq.com/docs/models
 */
@Singleton
class GroqClient @Inject constructor(
    private val settings: SettingsRepository
) {
    data class Turn(val role: String, val content: String)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    /** In-memory short history for multi-turn on Groq path */
    private val history = ArrayDeque<Turn>()
    private val maxHistoryTurns = 12

    fun resetHistory() {
        history.clear()
    }

    companion object {
        /** Curated Groq models (id to label). */
        val MODELS: List<Pair<String, String>> = listOf(
            "llama-3.3-70b-versatile" to "Llama 3.3 70B (best quality)",
            "llama-3.1-8b-instant" to "Llama 3.1 8B Instant (fastest)",
            "llama-3.1-70b-versatile" to "Llama 3.1 70B",
            "gemma2-9b-it" to "Gemma 2 9B",
            "mixtral-8x7b-32768" to "Mixtral 8x7B (long context)",
            "qwen/qwen3-32b" to "Qwen3 32B",
            "openai/gpt-oss-120b" to "GPT-OSS 120B (if available)",
            "openai/gpt-oss-20b" to "GPT-OSS 20B (if available)"
        )
    }

    suspend fun chat(
        userText: String,
        systemPrompt: String,
        maxTokens: Int = 2048,
        useHistory: Boolean = true
    ): String {
        val key = settings.getGroqApiKey()
        if (key.isBlank()) return "Groq error: API key not set. Get one at console.groq.com"

        val model = settings.getGroqModel().ifBlank { "llama-3.3-70b-versatile" }
        val temperature = settings.getGroqTemperature()
        val tokens = maxTokens.coerceIn(256, settings.getGroqMaxTokens())

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))

        if (useHistory && settings.isGroqHistoryEnabled()) {
            history.forEach { turn ->
                messages.put(JSONObject().put("role", turn.role).put("content", turn.content))
            }
        }
        messages.put(JSONObject().put("role", "user").put("content", userText))

        val bodyJson = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", temperature)
            .put("max_tokens", tokens)

        if (settings.isGroqJsonMode()) {
            bodyJson.put("response_format", JSONObject().put("type", "json_object"))
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = try {
                        JSONObject(raw).optJSONObject("error")?.optString("message") ?: raw.take(200)
                    } catch (_: Exception) {
                        raw.take(200).ifBlank { "HTTP ${resp.code}" }
                    }
                    return "Groq error: HTTP ${resp.code} — $errMsg"
                }
                if (raw.isBlank()) return "Groq error: empty body"
                val json = JSONObject(raw)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                if (content.isNullOrBlank()) {
                    val err = json.optJSONObject("error")?.optString("message")
                    return "Groq error: ${err ?: "empty choices"}"
                }

                if (useHistory && settings.isGroqHistoryEnabled()) {
                    history.addLast(Turn("user", userText))
                    history.addLast(Turn("assistant", content))
                    while (history.size > maxHistoryTurns * 2) {
                        history.removeFirst()
                    }
                }
                content
            }
        } catch (e: Exception) {
            "Groq error: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}
