package com.aiclient.chat.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiclient.chat.AiClientApp
import com.aiclient.chat.data.model.AiProvider
import com.aiclient.chat.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val providers: List<AiProvider> = emptyList(),
    val defaultProviderId: String? = null,
    val systemPrompt: String = "",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val fontScale: Float = 1.0f,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<AiClientApp>()
    private val repo get() = app.chatRepository
    private val settings get() = app.settingsRepository

    val uiState: StateFlow<SettingsUiState> = combine(
        repo.observeProviders(),
        settings.defaultProviderId,
        settings.systemPrompt,
        settings.themeMode,
        settings.fontScale,
    ) { providers, defaultProviderId, prompt, theme, scale ->
        SettingsUiState(
            providers = providers,
            defaultProviderId = defaultProviderId,
            systemPrompt = prompt,
            themeMode = theme,
            fontScale = scale,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun getApiKey(providerId: String): String? = settings.getApiKey(providerId)

    fun maskedApiKey(providerId: String): String {
        val key = settings.getApiKey(providerId)
        if (key.isNullOrBlank()) return ""
        return if (key.length <= 8) "••••••••" else key.take(6) + "…" + key.takeLast(4)
    }

    fun saveProvider(provider: AiProvider, apiKey: String) = viewModelScope.launch {
        repo.saveProvider(provider)
        if (apiKey.isNotBlank()) settings.setApiKey(provider.id, apiKey) else settings.clearApiKey(provider.id)
        if (settings.defaultProviderId.first() == null) {
            settings.setDefaultProviderId(provider.id)
        }
    }

    fun deleteProvider(id: String) = viewModelScope.launch {
        repo.deleteProvider(id)
        settings.clearApiKey(id)
        if (settings.defaultProviderId.first() == id) {
            val remaining = repo.getProviders().firstOrNull { it.id != id }
            if (remaining != null) settings.setDefaultProviderId(remaining.id)
        }
    }

    fun setDefaultProvider(id: String) = viewModelScope.launch { settings.setDefaultProviderId(id) }

    fun setSystemPrompt(prompt: String) = viewModelScope.launch { settings.setSystemPrompt(prompt) }

    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setFontScale(scale: Float) = viewModelScope.launch { settings.setFontScale(scale) }
}
