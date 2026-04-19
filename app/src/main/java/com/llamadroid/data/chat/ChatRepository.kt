package com.llamadroid.data.chat

import com.llamadroid.data.db.ChatDao
import com.llamadroid.data.db.ChatMessageEntity
import com.llamadroid.data.db.ChatSessionEntity
import com.llamadroid.data.db.toDomain
import com.llamadroid.domain.chat.ChatMessage
import com.llamadroid.domain.chat.ChatSession
import com.llamadroid.domain.chat.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ChatRepository(private val chatDao: ChatDao) {
    val sessions: Flow<List<ChatSession>> = chatDao.observeSessions().map { rows -> rows.map { it.toDomain() } }

    fun observeSession(chatId: Long) = chatDao.observeSession(chatId).map { it?.toDomain() }

    fun observeMessages(chatId: Long): Flow<List<ChatMessage>> = chatDao.observeMessages(chatId)
        .map { rows -> rows.map { it.toDomain() } }

    fun observeChat(chatId: Long) = combine(observeSession(chatId), observeMessages(chatId)) { session, messages ->
        session?.let { com.llamadroid.domain.chat.ChatWithMessages(it, messages) }
    }

    suspend fun createChat(title: String = "New chat", systemPrompt: String? = null): Long {
        val now = System.currentTimeMillis()
        return chatDao.insertSession(
            ChatSessionEntity(
                title = title,
                systemPrompt = systemPrompt,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun addMessage(
        chatId: Long,
        role: MessageRole,
        content: String,
        tokenCount: Int? = null,
        tokensPerSecond: Float? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val id = chatDao.insertMessage(
            ChatMessageEntity(
                chatId = chatId,
                role = role.name,
                content = content,
                createdAt = now,
                tokenCount = tokenCount,
                tokensPerSecond = tokensPerSecond,
            ),
        )
        chatDao.touch(chatId, now)
        return id
    }

    suspend fun updateMessage(messageId: Long, content: String) {
        chatDao.updateMessage(messageId, content)
    }

    suspend fun messages(chatId: Long): List<ChatMessage> = chatDao.messages(chatId).map { it.toDomain() }

    suspend fun rename(chatId: Long, title: String) = chatDao.rename(chatId, title, System.currentTimeMillis())

    suspend fun updateSystemPrompt(chatId: Long, systemPrompt: String?) {
        chatDao.updateSystemPrompt(chatId, systemPrompt, System.currentTimeMillis())
    }

    suspend fun clear(chatId: Long) = chatDao.clearMessages(chatId)

    suspend fun delete(chatId: Long) {
        chatDao.deleteMessagesForChat(chatId)
        chatDao.deleteSession(chatId)
    }

    suspend fun deleteFrom(chatId: Long, fromMessageId: Long) = chatDao.deleteFrom(chatId, fromMessageId)
}
