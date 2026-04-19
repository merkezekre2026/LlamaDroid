package com.llamadroid.domain.chat

enum class MessageRole {
    System,
    User,
    Assistant,
}

data class ChatSession(
    val id: Long,
    val title: String,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ChatMessage(
    val id: Long,
    val chatId: Long,
    val role: MessageRole,
    val content: String,
    val createdAt: Long,
    val tokenCount: Int? = null,
    val tokensPerSecond: Float? = null,
)

data class ChatWithMessages(
    val session: ChatSession,
    val messages: List<ChatMessage>,
)
