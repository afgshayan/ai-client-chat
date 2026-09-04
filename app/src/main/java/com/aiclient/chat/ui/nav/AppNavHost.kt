package com.aiclient.chat.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aiclient.chat.ui.chat.ChatScreen
import com.aiclient.chat.ui.chat.ChatViewModel
import com.aiclient.chat.ui.settings.ProviderEditorScreen
import com.aiclient.chat.ui.settings.SettingsScreen
import com.aiclient.chat.ui.settings.SettingsViewModel
import com.aiclient.chat.ui.theme.AiClientChatTheme

private object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val ADD_PROVIDER = "provider_editor"
    const val EDIT_PROVIDER = "provider_editor/{providerId}"
}

@Composable
fun AiClientChatApp() {
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()

    AiClientChatTheme(themeMode = settingsState.themeMode) {
        if (settingsState.providers.isEmpty()) {
            ProviderEditorScreen(
                initial = null,
                initialApiKey = "",
                isOnboarding = true,
                onSave = { provider, apiKey -> settingsViewModel.saveProvider(provider, apiKey) },
                onCancel = {},
            )
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
                        onAddProvider = { navController.navigate(Routes.ADD_PROVIDER) },
                        onEditProvider = { id -> navController.navigate("provider_editor/$id") },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ADD_PROVIDER) {
                    ProviderEditorScreen(
                        initial = null,
                        initialApiKey = "",
                        isOnboarding = false,
                        onSave = { provider, apiKey ->
                            settingsViewModel.saveProvider(provider, apiKey)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                    )
                }
                composable(
                    Routes.EDIT_PROVIDER,
                    arguments = listOf(navArgument("providerId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val providerId = backStackEntry.arguments?.getString("providerId")
                    val provider = settingsState.providers.find { it.id == providerId }
                    val initialKey = remember(providerId) { providerId?.let { settingsViewModel.getApiKey(it) } ?: "" }
                    ProviderEditorScreen(
                        initial = provider,
                        initialApiKey = initialKey,
                        isOnboarding = false,
                        onSave = { updated, apiKey ->
                            settingsViewModel.saveProvider(updated, apiKey)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
