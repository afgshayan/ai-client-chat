package com.aiclient.chat.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val providerId: String,
    val model: String,
    val systemPrompt: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val baseUrl: String,
    val authHeaderName: String,
    val authScheme: String,
    val model: String,
    val createdAt: Long,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user" | "assistant" | "system"
    val text: String,
    /** JSON array of base64 image data-URIs attached to a user message. */
    val imagesJson: String? = null,
    val createdAt: Long,
    val isError: Boolean = false,
    val isStreaming: Boolean = false,
)
