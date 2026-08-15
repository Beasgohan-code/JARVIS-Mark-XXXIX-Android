package com.jarvis.mark39.ai

import com.jarvis.mark39.data.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes chat to primary LLM with automatic fallback across
 * Gemini → Groq → OpenRouter (whichever keys are set).
 */
@Singleton
class LlmRouter @Inject constructor(
    private val settings: SettingsRepository,
    private val gemini: GeminiClient,
    private val groq: GroqClient,
    private val openRouter: OpenRouterClient
) {
    data class ChatResult(val text: String, val provider: String)

    private fun isFailure(s: String): Boolean {
        val t = s.lowercase()
        return t.startsWith("error") ||
            t.contains("api key") ||
            t.contains("not set") ||
            t.contains("not available") ||
            t.contains("rate limit") ||
            t.contains("resource_exhausted") ||
            t.contains("http 4") ||
            t.contains("http 5") ||
            t.contains("empty response") ||
            t.contains("network") ||
            t.contains("timeout") ||
            t.contains("openrouter error") ||
            t.contains("groq error") ||
            t.contains("model not available")
    }

    /** Heuristic: short / simple prompts get fewer max tokens. */
    fun tokenBudget(userText: String): Int {
        if (!settings.isSkillSaveTokens()) return 2048
        val len = userText.trim().length
        val simple = userText.trim().lowercase().let { t ->
            t.length < 80 ||
                t in listOf("hi", "hello", "hey", "thanks", "thank you", "ok", "bye") ||
                t.startsWith("what is ") && t.length < 60 ||
                t.startsWith("open ") ||
                t.startsWith("go ") ||
                t.startsWith("volume")
        }
        return when {
            simple || len < 40 -> 256
            len < 120 -> 512
            len < 400 -> 1024
            else -> 2048
        }
    }

    fun systemPromptFor(userText: String): String {
        var base = settings.resolveSystemPrompt()
        if (settings.isSkillSaveTokens() && tokenBudget(userText) <= 512) {
            base += "\n\n[Token-saver] Reply very briefly unless the user needs detail or code."
        }
        if (settings.isSkillCoding() && looksLikeCodeRequest(userText)) {
            base += "\n\n[Coding skill ON] Prefer complete, runnable code in fenced blocks."
        }
        if (!settings.isSkillWebSearch()) {
            base += "\n\n[Web search skill OFF] Do not claim live web results."
        }
        if (settings.isSkillAgent()) {
            base += "\n\n[Agent skill ON] Break complex tasks into short steps when useful."
        }
        return base
    }

    private fun looksLikeCodeRequest(t: String): Boolean {
        val s = t.lowercase()
        return listOf("code", "python", "kotlin", "script", "function", "bot", "html", "api", "bug", "error", "implement")
            .any { s.contains(it) }
    }

    /**
     * Try providers in order: user primary first, then the rest that have keys.
     */
    suspend fun chat(userText: String): ChatResult {
        val prompt = systemPromptFor(userText)
        val maxTok = tokenBudget(userText)
        val primary = settings.getLlmProvider()

        val order = linkedSetOf<String>()
        order.add(primary)
        if (settings.isFallbackEnabled()) {
            listOf("gemini", "groq", "openrouter").forEach { order.add(it) }
        }

        val attempts = mutableListOf<String>()
        for (provider in order) {
            if (!hasKey(provider)) continue
            val result = try {
                when (provider) {
                    "groq" -> groq.chat(userText, prompt, maxTok)
                    "openrouter" -> openRouter.chat(userText, prompt)
                    else -> gemini.sendMessage(userText)
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            if (!isFailure(result)) {
                return ChatResult(result, provider)
            }
            attempts.add("$provider → ${result.take(80)}")
        }

        if (attempts.isEmpty()) {
            return ChatResult(
                "No API keys set. Add Gemini, Groq, and/or OpenRouter keys in Settings.",
                "none"
            )
        }
        return ChatResult(
            "All providers failed:\n" + attempts.joinToString("\n"),
            "none"
        )
    }

    private fun hasKey(provider: String): Boolean = when (provider) {
        "groq" -> settings.getGroqApiKey().isNotBlank()
        "openrouter" -> settings.getOpenRouterApiKey().isNotBlank()
        else -> settings.getGeminiApiKey().isNotBlank()
    }
}
