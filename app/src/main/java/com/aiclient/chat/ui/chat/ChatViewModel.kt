package com.aiclient.chat.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiclient.chat.AiClientApp
import com.aiclient.chat.R
import com.aiclient.chat.data.model.ChatMessage
import com.aiclient.chat.data.model.Conversation
import com.aiclient.chat.data.model.Role
import com.aiclient.chat.data.remote.StreamEvent
import com.aiclient.chat.data.util.ImageEncoder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val currentConversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val searchQuery: String = "",
    val pendingImages: List<String> = emptyList(),
    val errorMessage: String? = null,
    val artifact: ArtifactPreview? = null,
)

data class ArtifactPreview(val language: String, val code: String)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<AiClientApp>()
    private val repo get() = app.chatRepository
    private val api get() = app.apiClient
    private val settings get() = app.settingsRepository

    private val searchQuery = MutableStateFlow("")
    private val currentConversationId = MutableStateFlow<String?>(null)
    private val streamingMessage = MutableStateFlow<ChatMessage?>(null)
    private val pendingImages = MutableStateFlow<List<String>>(emptyList())
    private val errorMessage = MutableStateFlow<String?>(null)
    private val artifact = MutableStateFlow<ArtifactPreview?>(null)
    private val isGenerating = MutableStateFlow(false)

    private var generationJob: Job? = null
    private var streamingAccumulator: StringBuilder = StringBuilder()

    // Conversations list, re-queried whenever the search text changes.
    private val conversationsFlow = MutableStateFlow<List<Conversation>>(emptyList())

    private val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

    val uiState: StateFlow<ChatUiState> = combine(
        conversationsFlow,
        currentConversationId,
        messagesFlow,
        streamingMessage,
        isGenerating,
        searchQuery,
        pendingImages,
        errorMessage,
        artifact,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val convs = values[0] as List<Conversation>
        val currentId = values[1] as String?
        val msgs = values[2] as List<ChatMessage>
        val streaming = values[3] as ChatMessage?
        val generating = values[4] as Boolean
        val query = values[5] as String
        val images = values[6] as List<String>
        val error = values[7] as String?
        val art = values[8] as ArtifactPreview?
        ChatUiState(
            conversations = convs,
            currentConversation = convs.find { it.id == currentId },
            messages = if (streaming != null) msgs + streaming else msgs,
            isGenerating = generating,
            searchQuery = query,
            pendingImages = images,
            errorMessage = error,
            artifact = art,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    init {
        viewModelScope.launch {
            searchQuery.flatMapLatest { q -> repo.observeConversations(q) }
                .collect { conversationsFlow.value = it }
        }
        viewModelScope.launch {
            currentConversationId.flatMapLatest { id ->
                if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else repo.observeMessages(id)
            }.collect { messagesFlow.value = it }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectConversation(id: String) {
        currentConversationId.value = id
        artifact.value = null
    }

    fun startNewConversation() {
        currentConversationId.value = null
        streamingMessage.value = null
        pendingImages.value = emptyList()
        artifact.value = null
    }

    fun renameConversation(id: String, title: String) = viewModelScope.launch {
        repo.renameConversation(id, title)
    }

    fun togglePinned(conversation: Conversation) = viewModelScope.launch {
        repo.setPinned(conversation.id, !conversation.pinned)
    }

    /** Changes the model for the active conversation, or the app-wide default when no chat is open yet. */
    fun setModel(modelId: String) = viewModelScope.launch {
        val convId = currentConversationId.value
        if (convId != null) {
            repo.touchConversation(convId, modelId)
        } else {
            settings.setDefaultModel(modelId)
        }
    }

    fun deleteConversation(id: String) = viewModelScope.launch {
        repo.deleteConversation(id)
        if (currentConversationId.value == id) startNewConversation()
    }

    fun deleteAllConversations() = viewModelScope.launch {
        repo.deleteAllConversations()
        startNewConversation()
    }

    fun addPendingImage(uri: Uri) = viewModelScope.launch {
        val dataUri = ImageEncoder.encodeToDataUri(app, uri) ?: return@launch
        pendingImages.update { it + dataUri }
    }

    fun removePendingImage(index: Int) {
        pendingImages.update { it.toMutableList().apply { removeAt(index) } }
    }

    fun dismissError() {
        errorMessage.value = null
    }

    fun dismissArtifact() {
        artifact.value = null
    }

    fun showArtifact(language: String, code: String) {
        artifact.value = ArtifactPreview(language, code)
    }

    fun sendMessage(text: String) {
        if (text.isBlank() && pendingImages.value.isEmpty()) return
        val apiKey = settings.getApiKey()
        if (apiKey.isNullOrBlank()) {
            errorMessage.value = app.getString(R.string.error_no_api_key)
            return
        }

        viewModelScope.launch {
            val defaultModel = first(settings.defaultModel)
            val defaultSystemPrompt = first(settings.systemPrompt)

            var conversation = currentConversationId.value?.let { repo.getConversation(it) }
            if (conversation == null) {
                conversation = repo.createConversation(defaultModel, defaultSystemPrompt, title = deriveTitle(text))
                currentConversationId.value = conversation.id
            } else if (conversation.title == "گفتگوی جدید" && text.isNotBlank()) {
                repo.renameConversation(conversation.id, deriveTitle(text))
            }
            val conversationId = conversation.id

            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = Role.USER,
                text = text,
                images = pendingImages.value,
                createdAt = System.currentTimeMillis(),
            )
            repo.addMessage(userMessage)
            pendingImages.value = emptyList()

            runGeneration(conversationId, conversation.model, conversation.systemPrompt.ifBlank { defaultSystemPrompt }, apiKey)
        }
    }

    fun stopGenerating() {
        generationJob?.cancel()
        isGenerating.value = false
        viewModelScope.launch { finalizeStreamingMessage() }
    }

    fun regenerateLastResponse() {
        val convId = currentConversationId.value ?: return
        val apiKey = settings.getApiKey()
        if (apiKey.isNullOrBlank()) {
            errorMessage.value = app.getString(R.string.error_no_api_key)
            return
        }
        viewModelScope.launch {
            val conversation = repo.getConversation(convId) ?: return@launch
            val history = repo.getHistory(convId)
            val lastAssistant = history.lastOrNull { it.role == Role.ASSISTANT }
            if (lastAssistant != null) {
                repo.deleteMessagesFrom(convId, lastAssistant.createdAt)
            }
            val defaultSystemPrompt = first(settings.systemPrompt)
            runGeneration(convId, conversation.model, conversation.systemPrompt.ifBlank { defaultSystemPrompt }, apiKey)
        }
    }

    fun editUserMessage(message: ChatMessage, newText: String) {
        val convId = currentConversationId.value ?: return
        val apiKey = settings.getApiKey()
        if (apiKey.isNullOrBlank()) {
            errorMessage.value = app.getString(R.string.error_no_api_key)
            return
        }
        viewModelScope.launch {
            val conversation = repo.getConversation(convId) ?: return@launch
            repo.deleteMessagesFrom(convId, message.createdAt)
            repo.addMessage(message.copy(text = newText, createdAt = message.createdAt))
            val defaultSystemPrompt = first(settings.systemPrompt)
            runGeneration(convId, conversation.model, conversation.systemPrompt.ifBlank { defaultSystemPrompt }, apiKey)
        }
    }

    private fun runGeneration(conversationId: String, model: String, systemPrompt: String, apiKey: String) {
        generationJob?.cancel()
        streamingAccumulator = StringBuilder()
        streamingMessage.value = ChatMessage(
            id = "streaming",
            conversationId = conversationId,
            role = Role.ASSISTANT,
            text = "",
            createdAt = System.currentTimeMillis(),
            isStreaming = true,
        )
        isGenerating.value = true

        generationJob = viewModelScope.launch {
            val history = repo.getHistory(conversationId)
            api.streamMessage(apiKey, model, systemPrompt, history).collect { event ->
                when (event) {
                    is StreamEvent.Started -> Unit
                    is StreamEvent.TextDelta -> {
                        streamingAccumulator.append(event.text)
                        streamingMessage.value = streamingMessage.value?.copy(text = streamingAccumulator.toString())
                    }
                    is StreamEvent.Done -> {
                        finalizeStreamingMessage()
                        isGenerating.value = false
                        repo.touchConversation(conversationId)
                    }
                    is StreamEvent.Failed -> {
                        if (streamingAccumulator.isNotEmpty()) {
                            finalizeStreamingMessage()
                        } else {
                            streamingMessage.value = null
                        }
                        repo.addMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                conversationId = conversationId,
                                role = Role.ASSISTANT,
                                text = event.message,
                                createdAt = System.currentTimeMillis(),
                                isError = true,
                            )
                        )
                        isGenerating.value = false
                        errorMessage.value = event.message
                    }
                }
            }
        }
    }

    private suspend fun finalizeStreamingMessage() {
        val pending = streamingMessage.value ?: return
        streamingMessage.value = null
        if (pending.text.isNotBlank()) {
            repo.addMessage(pending.copy(id = UUID.randomUUID().toString(), isStreaming = false))
        }
    }

    private fun deriveTitle(text: String): String {
        val trimmed = text.trim().replace("\n", " ")
        return if (trimmed.length > 48) trimmed.take(48) + "…" else trimmed.ifBlank { "گفتگوی جدید" }
    }
}
