package com.aiclient.chat.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiclient.chat.ui.chat.ChatScreen
import com.aiclient.chat.ui.chat.ChatViewModel
import com.aiclient.chat.ui.onboarding.OnboardingScreen
import com.aiclient.chat.ui.settings.SettingsScreen
import com.aiclient.chat.ui.settings.SettingsViewModel
import com.aiclient.chat.ui.theme.AiClientChatTheme

private object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AiClientChatApp() {
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    AiClientChatTheme(themeMode = settingsState.themeMode) {
        if (!settingsState.hasApiKey) {
            OnboardingScreen(onApiKeySubmitted = { key -> settingsViewModel.setApiKey(key) })
        } else {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.CHAT) {
                composable(Routes.CHAT) {
                    ChatScreen(
                        viewModel = chatViewModel,
                        fontScale = settingsState.fontScale,
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onClearAllConversations = { chatViewModel.deleteAllConversations() },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
