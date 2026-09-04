package com.aiclient.chat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.aiclient.chat.data.model.AuthScheme
import com.aiclient.chat.data.model.ProviderKind
import com.aiclient.chat.data.model.ProviderPreset
import com.aiclient.chat.data.model.ProviderPresets
import java.util.UUID

/**
 * Add- or edit-provider flow. With [initial] null this first asks the user to
 * pick a starting preset (Anthropic, Vertex, Azure Foundry, DeepSeek, OpenAI,
 * or a fully custom endpoint), then shows an editable form seeded from it —
 * every field, including the model ID, stays free text so any model on any
 * compatible endpoint can be used, not just Anthropic's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditorScreen(
    initial: AiProvider?,
    initialApiKey: String,
    isOnboarding: Boolean,
    onSave: (AiProvider, String) -> Unit,
    onCancel: () -> Unit,
) {
    var preset by remember { mutableStateOf<ProviderPreset?>(null) }

    if (initial == null && preset == null) {
        PresetPickerContent(isOnboarding = isOnboarding, onPick = { preset = it }, onCancel = onCancel)
        return
    }

    ProviderFormContent(
        initial = initial,
        preset = preset,
        initialApiKey = initialApiKey,
        isOnboarding = isOnboarding,
        onSave = onSave,
        onCancel = onCancel,
        onChangePreset = if (initial == null) ({ preset = null }) else null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetPickerContent(isOnboarding: Boolean, onPick: (ProviderPreset) -> Unit, onCancel: () -> Unit) {
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (isOnboarding) {
                Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
            ProviderPresets.ALL.forEach { preset ->
                Surface(
                    onClick = { onPick(preset) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(preset.displayName, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            preset.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (isOnboarding) {
        content()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.provider_choose_preset_title)) },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderFormContent(
    initial: AiProvider?,
    preset: ProviderPreset?,
    initialApiKey: String,
    isOnboarding: Boolean,
    onSave: (AiProvider, String) -> Unit,
    onCancel: () -> Unit,
    onChangePreset: (() -> Unit)?,
) {
    val kind = initial?.kind ?: preset?.kind ?: ProviderKind.ANTHROPIC
    var name by remember { mutableStateOf(initial?.name ?: preset?.displayName.orEmpty()) }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: preset?.baseUrlTemplate.orEmpty()) }
    var authHeaderName by remember { mutableStateOf(initial?.authHeaderName ?: preset?.authHeaderName ?: "Authorization") }
    var bearerPrefix by remember { mutableStateOf((initial?.authScheme ?: preset?.authScheme ?: AuthScheme.BEARER) == AuthScheme.BEARER) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initial?.model ?: preset?.defaultModel.orEmpty()) }

    val keyLabel = preset?.keyLabel ?: "API key"
    val keyHint = preset?.keyHint
    val baseUrlHint = preset?.baseUrlHint

    val formContent: @Composable () -> Unit = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (isOnboarding) {
                Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.provider_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.provider_base_url_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (!baseUrlHint.isNullOrBlank()) {
                Text(
                    baseUrlHint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text(stringResource(R.string.provider_model_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                stringResource(R.string.provider_model_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (!preset?.modelSuggestions.isNullOrEmpty()) {
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    preset!!.modelSuggestions.forEach { suggestion ->
                        Surface(
                            onClick = { model = suggestion },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(suggestion, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(keyLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                keyHint.takeUnless { it.isNullOrBlank() } ?: stringResource(R.string.provider_key_optional_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = authHeaderName,
                onValueChange = { authHeaderName = it },
                label = { Text("Auth header name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Checkbox(checked = bearerPrefix, onCheckedChange = { bearerPrefix = it })
                Text("Send as \"Bearer <key>\"", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    val provider = AiProvider(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name.ifBlank { preset?.displayName ?: "Provider" },
                        kind = kind,
                        baseUrl = baseUrl.trim(),
                        authHeaderName = authHeaderName.ifBlank { "Authorization" },
                        authScheme = if (bearerPrefix) AuthScheme.BEARER else AuthScheme.RAW,
                        model = model.trim(),
                    )
                    onSave(provider, apiKey.trim())
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isOnboarding) stringResource(R.string.onboarding_continue) else stringResource(R.string.save))
            }

            if (onChangePreset != null && isOnboarding) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.provider_choose_preset_title),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clickable { onChangePreset() }
                        .padding(8.dp),
                )
            }
        }
    }

    if (isOnboarding) {
        formContent()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initial == null) stringResource(R.string.providers_add) else name) },
                    navigationIcon = {
                        IconButton(onClick = onChangePreset ?: onCancel) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) { formContent() }
        }
    }
}
