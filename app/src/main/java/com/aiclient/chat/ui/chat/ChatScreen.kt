package com.aiclient.chat.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aiclient.chat.R
import com.aiclient.chat.data.model.ChatMessage
import com.aiclient.chat.data.model.Models
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    fontScale: Float,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showModelPicker by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var inputText by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addPendingImage(it) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversation?.id,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                onNewChat = {
                    viewModel.startNewConversation()
                    scope.launch { drawerState.close() }
                },
                onSelect = {
                    viewModel.selectConversation(it)
                    scope.launch { drawerState.close() }
                },
                onRename = { conversation, title -> viewModel.renameConversation(conversation.id, title) },
                onDelete = { viewModel.deleteConversation(it.id) },
                onTogglePin = { viewModel.togglePinned(it) },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "منو")
                        }
                    },
                    title = {
                        val modelId = uiState.currentConversation?.model ?: Models.DEFAULT
                        val modelName = Models.AVAILABLE.find { it.id == modelId }?.displayName ?: modelId
                        TextButton(onClick = { showModelPicker = true }) {
                            Text(modelName, style = MaterialTheme.typography.titleMedium)
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (uiState.messages.isEmpty()) {
                    EmptyState(modifier = Modifier.weight(1f))
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
                        if (uiState.messages.isNotEmpty()) {
                            listState.animateScrollToItem(uiState.messages.size - 1)
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                fontScale = fontScale,
                                onEdit = { editingMessage = it },
                                onRegenerate = { viewModel.regenerateLastResponse() },
                                onPreviewArtifact = { lang, code -> viewModel.showArtifact(lang, code) },
                            )
                        }
                    }
                }

                ChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    pendingImages = uiState.pendingImages,
                    onRemoveImage = viewModel::removePendingImage,
                    onAttachImage = { imagePicker.launch("image/*") },
                    isGenerating = uiState.isGenerating,
                    onSend = {
                        if (inputText.isNotBlank() || uiState.pendingImages.isNotEmpty()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onStop = viewModel::stopGenerating,
                )
            }
        }
    }

    if (showModelPicker) {
        ModelPickerSheet(
            selectedModel = uiState.currentConversation?.model ?: Models.DEFAULT,
            onSelect = viewModel::setModel,
            onDismiss = { showModelPicker = false },
        )
    }

    editingMessage?.let { message ->
        EditMessageDialog(
            initialText = message.text,
            onDismiss = { editingMessage = null },
            onConfirm = { newText ->
                viewModel.editUserMessage(message, newText)
                editingMessage = null
            },
        )
    }

    uiState.artifact?.let { artifact ->
        ArtifactSheet(artifact = artifact, onDismiss = viewModel::dismissArtifact)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "امروز چه کاری از دستم برمی‌آید؟",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    pendingImages: List<String>,
    onRemoveImage: (Int) -> Unit,
    onAttachImage: () -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (pendingImages.isNotEmpty()) {
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    pendingImages.forEachIndexed { index, dataUri ->
                        val bitmap = remember(dataUri) { com.aiclient.chat.data.util.ImageEncoder.decodeDataUri(dataUri)?.asImageBitmap() }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 6.dp),
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                bitmap?.let {
                                    androidx.compose.foundation.Image(
                                        bitmap = it,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onRemoveImage(index) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_attachment))
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                IconButton(onClick = onAttachImage) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = stringResource(R.string.attach_image))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.message_input_placeholder)) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 6,
                )
                Spacer(Modifier.size(8.dp))
                if (isGenerating) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.stop_generating))
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = text.isNotBlank() || pendingImages.isNotEmpty(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            Icons.Filled.ArrowUpward,
                            contentDescription = stringResource(R.string.send),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMessageDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_message)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
