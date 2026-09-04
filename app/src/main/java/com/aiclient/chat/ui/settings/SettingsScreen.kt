package com.aiclient.chat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiclient.chat.R
import com.aiclient.chat.data.model.Models
import com.aiclient.chat.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onClearAllConversations: () -> Unit, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(stringResource(R.string.settings_api_key), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (uiState.hasApiKey) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(uiState.maskedApiKey, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearApiKey() }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.onboarding_api_key_hint)) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.setApiKey(apiKeyInput); apiKeyInput = "" }, enabled = apiKeyInput.isNotBlank()) {
                    Text(stringResource(R.string.confirm))
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.settings_default_model), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedButton(onClick = { modelMenuOpen = true }) {
                    Text(Models.AVAILABLE.find { it.id == uiState.defaultModel }?.displayName ?: uiState.defaultModel)
                }
                DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                    Models.AVAILABLE.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.displayName) },
                            onClick = { viewModel.setDefaultModel(model.id); modelMenuOpen = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.settings_system_prompt), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            var systemPromptText by remember(uiState.systemPrompt) { mutableStateOf(uiState.systemPrompt) }
            OutlinedTextField(
                value = systemPromptText,
                onValueChange = { systemPromptText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.setSystemPrompt(systemPromptText) }) { Text(stringResource(R.string.confirm)) }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow {
                val options = listOf(
                    AppThemeMode.SYSTEM to R.string.settings_theme_system,
                    AppThemeMode.LIGHT to R.string.settings_theme_light,
                    AppThemeMode.DARK to R.string.settings_theme_dark,
                )
                options.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.settings_font_size), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = uiState.fontScale,
                onValueChange = { viewModel.setFontScale(it) },
                valueRange = 0.85f..1.3f,
                steps = 4,
            )

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            OutlinedButton(onClick = { showClearAllConfirm = true }) {
                Text(stringResource(R.string.settings_clear_all), color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.settings_about),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_all)) },
            text = { Text(stringResource(R.string.delete_conversation_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearAllConfirm = false
                    onClearAllConversations()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
