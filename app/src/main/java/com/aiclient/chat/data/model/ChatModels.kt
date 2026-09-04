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
    val providerId: String,
    val model: String,
    val systemPrompt: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
)
