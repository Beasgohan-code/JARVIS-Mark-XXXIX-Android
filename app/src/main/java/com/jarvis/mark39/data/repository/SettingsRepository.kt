package com.jarvis.mark39.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getGeminiApiKey(): String = prefs.getString(KEY_GEMINI, "") ?: ""
    fun setGeminiApiKey(key: String) = prefs.edit().putString(KEY_GEMINI, key.trim()).apply()

    fun getOpenRouterApiKey(): String = prefs.getString(KEY_OPENROUTER, "") ?: ""
    fun setOpenRouterApiKey(key: String) = prefs.edit().putString(KEY_OPENROUTER, key.trim()).apply()

    fun isConfirmBeforeAction(): Boolean = prefs.getBoolean(KEY_CONFIRM, true)
    fun setConfirmBeforeAction(value: Boolean) = prefs.edit().putBoolean(KEY_CONFIRM, value).apply()

    
    fun getGeminiModel(): String =
        prefs.getString("gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"

    fun setGeminiModel(model: String) {
        prefs.edit().putString("gemini_model", model).apply()
    }

    
    fun getStyleId(): String = prefs.getString("style_id", "JARVIS_CYAN") ?: "JARVIS_CYAN"
    fun setStyleId(id: String) = prefs.edit().putString("style_id", id).apply()

    fun isLightMode(): Boolean = prefs.getBoolean("light_mode", false)
    fun setLightMode(v: Boolean) = prefs.edit().putBoolean("light_mode", v).apply()

    fun getWallpaperId(): String = prefs.getString("wallpaper_id", "DEEP_SPACE") ?: "DEEP_SPACE"
    fun setWallpaperId(id: String) = prefs.edit().putString("wallpaper_id", id).apply()

    
    fun isAppLockEnabled(): Boolean = prefs.getBoolean("app_lock", false)
    fun setAppLockEnabled(v: Boolean) = prefs.edit().putBoolean("app_lock", v).apply()

    fun getAppPin(): String = prefs.getString("app_pin", "") ?: ""
    fun setAppPin(pin: String) = prefs.edit().putString("app_pin", pin).apply()

    fun isHideFromRecents(): Boolean = prefs.getBoolean("hide_recents", false)
    fun setHideFromRecents(v: Boolean) = prefs.edit().putBoolean("hide_recents", v).apply()

    fun isIncognitoMode(): Boolean = prefs.getBoolean("incognito", false)
    fun setIncognitoMode(v: Boolean) = prefs.edit().putBoolean("incognito", v).apply()

    
    fun getSystemPromptId(): String = prefs.getString("system_prompt_id", "jarvis_ultimate") ?: "jarvis_classic"
    fun setSystemPromptId(id: String) = prefs.edit().putString("system_prompt_id", id).apply()

    fun getCustomSystemPrompt(): String = prefs.getString("custom_system_prompt", "") ?: ""
    fun setCustomSystemPrompt(text: String) = prefs.edit().putString("custom_system_prompt", text).apply()

    fun resolveSystemPrompt(): String {
        val id = getSystemPromptId()
        val base = if (id == "custom") {
            val c = getCustomSystemPrompt().trim()
            if (c.isNotBlank()) c else com.jarvis.mark39.ai.SystemPrompts.defaultBody()
        } else {
            com.jarvis.mark39.ai.SystemPrompts.byId(id).body
        }
        val depth = com.jarvis.mark39.ai.SystemPrompts.DepthMode.entries
            .find { it.id == getDepthMode() }
            ?: com.jarvis.mark39.ai.SystemPrompts.DepthMode.BALANCED
        return base + "

" + com.jarvis.mark39.ai.SystemPrompts.depthAddon(depth)
    }

    
    /** gemini | openrouter */
    fun getLlmProvider(): String = prefs.getString("llm_provider", "gemini") ?: "gemini"
    fun setLlmProvider(v: String) = prefs.edit().putString("llm_provider", v).apply()

    fun getOpenRouterModel(): String =
        prefs.getString("or_model", "anthropic/claude-3.5-sonnet") ?: "anthropic/claude-3.5-sonnet"
    fun setOpenRouterModel(v: String) = prefs.edit().putString("or_model", v).apply()

    fun isSkillWebSearch(): Boolean = prefs.getBoolean("skill_web", true)
    fun setSkillWebSearch(v: Boolean) = prefs.edit().putBoolean("skill_web", v).apply()
    fun isSkillPhoneControl(): Boolean = prefs.getBoolean("skill_phone", true)
    fun setSkillPhoneControl(v: Boolean) = prefs.edit().putBoolean("skill_phone", v).apply()
    fun isSkillCoding(): Boolean = prefs.getBoolean("skill_coding", true)
    fun setSkillCoding(v: Boolean) = prefs.edit().putBoolean("skill_coding", v).apply()
    fun isSkillVision(): Boolean = prefs.getBoolean("skill_vision", true)
    fun setSkillVision(v: Boolean) = prefs.edit().putBoolean("skill_vision", v).apply()
    fun isSkillAgent(): Boolean = prefs.getBoolean("skill_agent", false)
    fun setSkillAgent(v: Boolean) = prefs.edit().putBoolean("skill_agent", v).apply()

    
    fun getGroqApiKey(): String = prefs.getString("groq_api_key", "") ?: ""
    fun setGroqApiKey(key: String) = prefs.edit().putString("groq_api_key", key.trim()).apply()

    fun getGroqModel(): String =
        prefs.getString("groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
    fun setGroqModel(v: String) = prefs.edit().putString("groq_model", v).apply()

    fun isSkillSaveTokens(): Boolean = prefs.getBoolean("skill_save_tokens", true)
    fun setSkillSaveTokens(v: Boolean) = prefs.edit().putBoolean("skill_save_tokens", v).apply()

    fun isFallbackEnabled(): Boolean = prefs.getBoolean("llm_fallback", true)
    fun setFallbackEnabled(v: Boolean) = prefs.edit().putBoolean("llm_fallback", v).apply()

    
    fun getDepthMode(): String = prefs.getString("depth_mode", "balanced") ?: "balanced"
    fun setDepthMode(v: String) = prefs.edit().putString("depth_mode", v).apply()

    fun hasApiKey(): Boolean =
        getGeminiApiKey().isNotBlank() ||
            getOpenRouterApiKey().isNotBlank() ||
            getGroqApiKey().isNotBlank()

    companion object {
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_OPENROUTER = "openrouter_api_key"
        private const val KEY_CONFIRM = "confirm_before_action"
    }
}
