package com.jarvis.mark39.ai

import com.jarvis.mark39.data.repository.SettingsRepository
import com.jarvis.mark39.data.repository.CustomSkillRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Primary LLM + automatic fallback: Gemini / Groq / OpenRouter.
 */
@Singleton
class LlmRouter @Inject constructor(
    private val settings: SettingsRepository,
    private val gemini: GeminiClient,
    private val groq: GroqClient,
    private val openRouter: OpenRouterClient,
    private val customSkills: CustomSkillRepository
) {
    data class ChatResult(val text: String, val provider: String)

    /**
     * Only treat clear provider failures as failures — never normal assistant prose.
     */
    private fun isFailure(s: String): Boolean {
        val t = s.trim()
        if (t.isBlank()) return true
        val lower = t.lowercase()

        // Explicit error prefixes from our clients
        if (lower.startsWith("groq error")) return true
        if (lower.startsWith("openrouter error")) return true
        if (lower.startsWith("error:")) return true
        if (lower.startsWith("all providers failed")) return true

        // Known failure phrases (whole-message style)
        val failSnippets = listOf(
            "api key not set",
            "api key is missing",
            "invalid or missing gemini",
            "model not available",
            "no longer available",
            "resource_exhausted",
            "rate limit",
            "quota exceeded",
            "http 401",
            "http 403",
            "http 404",
            "http 429",
            "http 500",
            "http 502",
            "http 503",
            "empty groq response",
            "empty response from openrouter",
            "empty response from",
            "network or gemini error",
            "content generation stopped. reason: max_tokens",
            "max_tokens"
        )
        // Only if the message is short (error) or starts like an error — not long essays
        if (t.length < 400) {
            for (snip in failSnippets) {
                if (lower.contains(snip)) return true
            }
        } else {
            // long text is almost never a transport error
            if (lower.startsWith("groq error") || lower.startsWith("openrouter error")) return true
        }
        return false
    }

    fun tokenBudget(userText: String): Int {
        if (!settings.isSkillSaveTokens()) return 4096
        val len = userText.trim().length
        val simple = userText.trim().lowercase().let { t ->
            t.length < 60 ||
                t in listOf("hi", "hello", "hey", "thanks", "thank you", "ok", "bye", "tnx", "ty") ||
                t.startsWith("open ") ||
                t.startsWith("go ") ||
                t.startsWith("volume")
        }
        return when {
            simple || len < 40 -> 512
            len < 120 -> 1024
            len < 500 -> 2048
            else -> 4096
        }
    }

    suspend fun systemPromptFor(userText: String): String {
        var base = settings.resolveSystemPrompt() + customSkills.promptAddon()
        if (settings.isSkillSaveTokens() && tokenBudget(userText) <= 1024) {
            base += "\n\n[Token-saver] Keep the reply short unless code or a full file is required."
        }
        if (settings.isSkillCoding() && looksLikeCodeRequest(userText)) {
            base += "\n\n[Coding skill ON] Prefer complete, runnable code in fenced blocks."
        }
        if (!settings.isSkillWebSearch()) {
            base += "\n\n[Web search skill OFF] Do not claim live web results."
        }
        if (settings.isSkillAgent()) {
            base += "\n\n[Agent skill ON] For multi-step tasks, outline brief steps then execute clearly."
        }
        if (settings.isSkillMultiLangCode()) {
            base += "\n\n[Multi-lang coding ON] Freely write Python, JavaScript/TypeScript, Go, Rust, C/C++, Java, SQL, shell, HTML/CSS — not only Kotlin. Pick the best language for the task unless the user specifies. The Android app itself is Kotlin; user projects can be any language."
        }
        if (settings.isSkillTranslate()) {
            base += "\n\n[Translate skill ON] When asked to translate, provide accurate natural translations and keep formatting."
        }
        return base
    }

    private fun looksLikeCodeRequest(t: String): Boolean {
        val s = t.lowercase()
        return listOf(
            "code", "python", "kotlin", "script", "function", "bot", "html",
            "api", "bug", "error", "implement", "class ", "compose", "gradle"
        ).any { s.contains(it) }
    }

    suspend fun chat(userText: String): ChatResult {
        val prompt = systemPromptFor(userText)
        val maxTok = tokenBudget(userText)
        var primary = settings.getLlmProvider()
        // Prefer Groq for short/fast turns when enabled
        if (settings.isPreferGroqFast() && settings.getGroqApiKey().isNotBlank()) {
            val short = userText.trim().length < 120 || tokenBudget(userText) <= 1024
            if (short) primary = "groq"
        }

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
                "Error: ${e.message ?: e.javaClass.simpleName}"
            }
            if (!isFailure(result)) {
                return ChatResult(result, provider)
            }
            attempts.add("$provider → ${result.take(100).replace("\n", " ")}")
        }

        if (attempts.isEmpty()) {
            return ChatResult(
                "No API keys set. Add Gemini, Groq, and/or OpenRouter in Settings.",
                "none"
            )
        }
        return ChatResult(
            "All providers failed:\n" + attempts.joinToString("\n") +
                "\n\nTips: check keys, try model gemini-2.5-flash, or disable a broken provider.",
            "none"
        )
    }

    private fun hasKey(provider: String): Boolean = when (provider) {
        "groq" -> settings.getGroqApiKey().isNotBlank()
        "openrouter" -> settings.getOpenRouterApiKey().isNotBlank()
        else -> settings.getGeminiApiKey().isNotBlank()
    }
}
