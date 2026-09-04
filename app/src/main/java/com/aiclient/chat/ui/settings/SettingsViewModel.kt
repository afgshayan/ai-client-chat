package com.aiclient.chat.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiclient.chat.AiClientApp
import com.aiclient.chat.data.model.Models
import com.aiclient.chat.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    val maskedApiKey: String = "",
    val defaultModel: String = Models.DEFAULT,
    val systemPrompt: String = "",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val fontScale: Float = 1.0f,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings get() = getApplication<AiClientApp>().settingsRepository

    private val apiKeyState = MutableStateFlow(settings.getApiKey())

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.defaultModel,
        settings.systemPrompt,
        settings.themeMode,
        settings.fontScale,
        apiKeyState,
    ) { model, prompt, theme, scale, apiKey ->
        SettingsUiState(
            hasApiKey = !apiKey.isNullOrBlank(),
            maskedApiKey = apiKey?.let { maskKey(it) } ?: "",
            defaultModel = model,
            systemPrompt = prompt,
            themeMode = theme,
            fontScale = scale,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setApiKey(key: String) {
        settings.setApiKey(key.trim())
        apiKeyState.value = key.trim()
    }

    fun clearApiKey() {
        settings.clearApiKey()
        apiKeyState.value = null
    }

    fun setDefaultModel(modelId: String) = viewModelScope.launch { settings.setDefaultModel(modelId) }

    fun setSystemPrompt(prompt: String) = viewModelScope.launch { settings.setSystemPrompt(prompt) }

    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setFontScale(scale: Float) = viewModelScope.launch { settings.setFontScale(scale) }

    private fun maskKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        return key.take(6) + "…" + key.takeLast(4)
    }
}
