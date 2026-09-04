package com.aiclient.chat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiclient.chat.R
import com.aiclient.chat.data.model.AiProvider
import com.aiclient.chat.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onClearAllConversations: () -> Unit,
    onAddProvider: () -> Unit,
    onEditProvider: (String) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<AiProvider?>(null) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.providers_title), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddProvider) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.providers_add))
                }
            }
            Spacer(Modifier.height(8.dp))

            if (uiState.providers.isEmpty()) {
                Text(
                    stringResource(R.string.providers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.providers.forEach { provider ->
                    ProviderRow(
                        provider = provider,
                        isDefault = provider.id == uiState.defaultProviderId,
                        maskedKey = viewModel.maskedApiKey(provider.id),
                        onClick = { onEditProvider(provider.id) },
                        onSetDefault = { viewModel.setDefaultProvider(provider.id) },
                        onDelete = { deleteCandidate = provider },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
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
            Button(onClick = { viewModel.setSystemPrompt(systemPromptText) }) {
                Text(stringResource(R.string.confirm))
            }

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

    deleteCandidate?.let { provider ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(provider.name) },
            text = { Text(stringResource(R.string.provider_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProvider(provider.id)
                    deleteCandidate = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun ProviderRow(
    provider: AiProvider,
    isDefault: Boolean,
    maskedKey: String,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium)
                    if (isDefault) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                stringResource(R.string.provider_default_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    provider.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (maskedKey.isNotBlank()) {
                    Text(
                        maskedKey,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!isDefault) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.provider_set_default)) },
                            onClick = { menuOpen = false; onSetDefault() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
