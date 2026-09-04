package com.aiclient.chat.data.model

enum class Role { USER, ASSISTANT, SYSTEM }

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: Role,
    val text: String,
    val images: List<String> = emptyList(), // base64 data URIs
    val createdAt: Long,
    val isError: Boolean = false,
    val isStreaming: Boolean = false,
)

data class Conversation(
    val id: String,
    val title: String,
    val model: String,
    val systemPrompt: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
)

data class AiModel(
    val id: String,
    val displayName: String,
    val description: String,
)

object Models {
    val AVAILABLE = listOf(
        AiModel("claude-opus-5", "Opus 5", "Most capable model for complex tasks and deep reasoning"),
        AiModel("claude-sonnet-5", "Sonnet 5", "Balances speed and capability — great for most tasks"),
        AiModel("claude-fable-5-1", "Fable 5.1", "Creative model for writing and natural conversation"),
        AiModel("claude-haiku-4-5-20251001", "Haiku 4.5", "Fastest model for near-instant responses"),
    )

    val DEFAULT = AVAILABLE[1].id
}
