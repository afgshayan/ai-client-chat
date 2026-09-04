package com.aiclient.chat.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aiclient.chat.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // API keys are sensitive, so they live in an encrypted keystore-backed
    // file rather than plain DataStore — one entry per configured provider.
    private val securePrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getApiKey(providerId: String): String? = securePrefs.getString(apiKeyPrefKey(providerId), null)

    fun setApiKey(providerId: String, key: String) {
        securePrefs.edit().putString(apiKeyPrefKey(providerId), key).apply()
    }

    fun clearApiKey(providerId: String) {
        securePrefs.edit().remove(apiKeyPrefKey(providerId)).apply()
    }

    private fun apiKeyPrefKey(providerId: String) = "api_key_$providerId"

    val defaultProviderId: Flow<String?> = context.dataStore.data.map { it[DEFAULT_PROVIDER_ID] }

    suspend fun setDefaultProviderId(providerId: String) {
        context.dataStore.edit { it[DEFAULT_PROVIDER_ID] = providerId }
    }

    val systemPrompt: Flow<String> = context.dataStore.data.map { it[SYSTEM_PROMPT] ?: "" }

    suspend fun setSystemPrompt(prompt: String) {
        context.dataStore.edit { it[SYSTEM_PROMPT] = prompt }
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map {
        AppThemeMode.entries.find { m -> m.name == it[THEME_MODE] } ?: AppThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    val fontScale: Flow<Float> = context.dataStore.data.map { it[FONT_SCALE] ?: 1.0f }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[FONT_SCALE] = scale }
    }

    companion object {
        private val DEFAULT_PROVIDER_ID = stringPreferencesKey("default_provider_id")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
    }
}
