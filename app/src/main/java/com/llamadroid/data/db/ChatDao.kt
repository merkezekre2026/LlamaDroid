package com.llamadroid.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY createdAt ASC, id ASC")
    fun observeMessages(chatId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY createdAt ASC, id ASC")
    suspend fun messages(chatId: Long): List<ChatMessageEntity>

    @Insert
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun rename(chatId: Long, title: String, updatedAt: Long)

    @Query("UPDATE chat_sessions SET systemPrompt = :systemPrompt, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun updateSystemPrompt(chatId: Long, systemPrompt: String?, updatedAt: Long)

    @Query("UPDATE chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessage(messageId: Long, content: String)

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun touch(chatId: Long, updatedAt: Long)

    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun clearMessages(chatId: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :chatId")
    suspend fun deleteSession(chatId: Long)

    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: Long)

    @Query("DELETE FROM chat_messages WHERE id >= :fromMessageId AND chatId = :chatId")
    suspend fun deleteFrom(chatId: Long, fromMessageId: Long)
}
