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

    fun hasApiKey(): Boolean = getGeminiApiKey().isNotBlank()

    companion object {
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_OPENROUTER = "openrouter_api_key"
        private const val KEY_CONFIRM = "confirm_before_action"
    }
}
