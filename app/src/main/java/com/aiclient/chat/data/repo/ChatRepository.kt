package com.aiclient.chat.data.repo

import com.aiclient.chat.data.local.ConversationDao
import com.aiclient.chat.data.local.ConversationEntity
import com.aiclient.chat.data.local.MessageDao
import com.aiclient.chat.data.local.MessageEntity
import com.aiclient.chat.data.local.ProviderDao
import com.aiclient.chat.data.local.ProviderEntity
import com.aiclient.chat.data.model.AiProvider
import com.aiclient.chat.data.model.AuthScheme
import com.aiclient.chat.data.model.ChatMessage
import com.aiclient.chat.data.model.Conversation
import com.aiclient.chat.data.model.ProviderKind
import com.aiclient.chat.data.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val providerDao: ProviderDao,
) {
    fun observeConversations(query: String): Flow<List<Conversation>> =
        (if (query.isBlank()) conversationDao.observeAll() else conversationDao.search(query))
            .map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun getConversation(id: String): Conversation? = conversationDao.getById(id)?.toDomain()

    suspend fun createConversation(providerId: String, model: String, systemPrompt: String, title: String = "New chat"): Conversation {
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            providerId = providerId,
            model = model,
            systemPrompt = systemPrompt,
            createdAt = now,
            updatedAt = now,
        )
        conversationDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun renameConversation(id: String, title: String) {
        conversationDao.rename(id, title, System.currentTimeMillis())
    }

    suspend fun setPinned(id: String, pinned: Boolean) = conversationDao.setPinned(id, pinned)

    suspend fun deleteConversation(id: String) = conversationDao.delete(id)

    suspend fun deleteAllConversations() = conversationDao.deleteAll()

    suspend fun setConversationProvider(id: String, providerId: String, model: String) {
        val conv = conversationDao.getById(id) ?: return
        conversationDao.update(conv.copy(updatedAt = System.currentTimeMillis(), providerId = providerId, model = model))
    }

    suspend fun touchConversation(id: String) {
        val conv = conversationDao.getById(id) ?: return
        conversationDao.update(conv.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addMessage(message: ChatMessage) {
        messageDao.upsert(message.toEntity())
    }

    suspend fun updateMessage(message: ChatMessage) {
        messageDao.upsert(message.toEntity())
    }

    suspend fun deleteMessagesFrom(conversationId: String, fromCreatedAt: Long) {
        messageDao.deleteFrom(conversationId, fromCreatedAt)
    }

    suspend fun getHistory(conversationId: String): List<ChatMessage> =
        messageDao.getForConversation(conversationId).map { it.toDomain() }

    // --- Providers ---

    fun observeProviders(): Flow<List<AiProvider>> = providerDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getProviders(): List<AiProvider> = providerDao.getAll().map { it.toDomain() }

    suspend fun getProvider(id: String): AiProvider? = providerDao.getById(id)?.toDomain()

    suspend fun saveProvider(provider: AiProvider) {
        val existingCreatedAt = providerDao.getById(provider.id)?.createdAt
        providerDao.upsert(provider.toEntity(createdAt = existingCreatedAt ?: System.currentTimeMillis()))
    }

    suspend fun deleteProvider(id: String) = providerDao.delete(id)
}

private fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
    providerId = providerId,
    model = model,
    systemPrompt = systemPrompt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pinned = pinned,
)

private fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    conversationId = conversationId,
    role = when (role) {
        "user" -> Role.USER
        "assistant" -> Role.ASSISTANT
        else -> Role.SYSTEM
    },
    text = text,
    images = imagesJson?.let { json -> runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull() }
        ?: emptyList(),
    createdAt = createdAt,
    isError = isError,
    isStreaming = isStreaming,
)

private fun ChatMessage.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = when (role) {
        Role.USER -> "user"
        Role.ASSISTANT -> "assistant"
        Role.SYSTEM -> "system"
    },
    text = text,
    imagesJson = if (images.isEmpty()) null else Json.encodeToString(images),
    createdAt = createdAt,
    isError = isError,
    isStreaming = isStreaming,
)

private fun ProviderEntity.toDomain() = AiProvider(
    id = id,
    name = name,
    kind = runCatching { ProviderKind.valueOf(kind) }.getOrDefault(ProviderKind.ANTHROPIC),
    baseUrl = baseUrl,
    authHeaderName = authHeaderName,
    authScheme = runCatching { AuthScheme.valueOf(authScheme) }.getOrDefault(AuthScheme.RAW),
    model = model,
)

private fun AiProvider.toEntity(createdAt: Long) = ProviderEntity(
    id = id,
    name = name,
    kind = kind.name,
    baseUrl = baseUrl,
    authHeaderName = authHeaderName,
    authScheme = authScheme.name,
    model = model,
    createdAt = createdAt,
)
