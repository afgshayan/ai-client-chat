package com.aiclient.chat.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aiclient.chat.data.model.Models
import com.aiclient.chat.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // The API key is sensitive, so it lives in an encrypted keystore-backed
    // file rather than plain DataStore.
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

    fun getApiKey(): String? = securePrefs.getString(KEY_API_KEY, null)

    fun setApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun clearApiKey() {
        securePrefs.edit().remove(KEY_API_KEY).apply()
    }

    val defaultModel: Flow<String> = context.dataStore.data.map {
        it[DEFAULT_MODEL] ?: Models.DEFAULT
    }

    suspend fun setDefaultModel(modelId: String) {
        context.dataStore.edit { it[DEFAULT_MODEL] = modelId }
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

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[ONBOARDING_DONE] ?: false
    }

    suspend fun setOnboardingComplete(done: Boolean) {
        context.dataStore.edit { it[ONBOARDING_DONE] = done }
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private val DEFAULT_MODEL = stringPreferencesKey("default_model")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val FONT_SCALE = floatPreferencesKey("font_scale")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}
