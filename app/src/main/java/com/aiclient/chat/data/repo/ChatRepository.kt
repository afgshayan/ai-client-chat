package com.aiclient.chat.data.repo

import com.aiclient.chat.data.local.ConversationDao
import com.aiclient.chat.data.local.ConversationEntity
import com.aiclient.chat.data.local.MessageDao
import com.aiclient.chat.data.local.MessageEntity
import com.aiclient.chat.data.model.ChatMessage
import com.aiclient.chat.data.model.Conversation
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
) {
    fun observeConversations(query: String): Flow<List<Conversation>> =
        (if (query.isBlank()) conversationDao.observeAll() else conversationDao.search(query))
            .map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun getConversation(id: String): Conversation? = conversationDao.getById(id)?.toDomain()

    suspend fun createConversation(model: String, systemPrompt: String, title: String = "New chat"): Conversation {
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
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

    suspend fun touchConversation(id: String, model: String? = null) {
        val conv = conversationDao.getById(id) ?: return
        conversationDao.update(conv.copy(updatedAt = System.currentTimeMillis(), model = model ?: conv.model))
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
}

private fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
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
