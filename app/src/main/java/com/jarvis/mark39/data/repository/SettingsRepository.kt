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

    fun hasApiKey(): Boolean = getGeminiApiKey().isNotBlank()

    companion object {
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_OPENROUTER = "openrouter_api_key"
        private const val KEY_CONFIRM = "confirm_before_action"
    }
}
