package com.jarvis.mark39.ai

import com.jarvis.mark39.data.repository.SettingsRepository
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ChatRequest(
    val model: String,
    val messages: List<ORMessage>,
    val temperature: Float = 0.7f
)

data class ORMessage(val role: String, val content: String)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ORMessage?
)

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): ChatResponse
}

@Singleton
class OpenRouterClient @Inject constructor(
    private val settings: SettingsRepository
) {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val key = settings.getOpenRouterApiKey()
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $key")
                    .addHeader("HTTP-Referer", "https://jarvis-mark39.app")
                    .addHeader("X-Title", "JARVIS Mark XXXIX")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private val api by lazy {
        Retrofit.Builder()
            .baseUrl("https://openrouter.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }

    suspend fun generate(module: String, prompt: String): String {
        val key = settings.getOpenRouterApiKey()
        if (key.isBlank()) return "OpenRouter API key not set."

        val model = when (module) {
            "web_search", "memory" -> "google/gemini-flash-1.5-8b"
            else -> "google/gemini-flash-1.5-8b"
        }

        return try {
            val response = api.chatCompletion(
                ChatRequest(
                    model = model,
                    messages = listOf(ORMessage("user", prompt))
                )
            )
            response.choices?.firstOrNull()?.message?.content ?: "No response from OpenRouter."
        } catch (e: Exception) {
            "OpenRouter error: ${e.message}"
        }
    }
}
