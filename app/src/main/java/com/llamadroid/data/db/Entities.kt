package com.llamadroid.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.llamadroid.domain.chat.ChatMessage
import com.llamadroid.domain.chat.ChatSession
import com.llamadroid.domain.chat.MessageRole
import com.llamadroid.domain.model.LocalModel

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "chat_messages",
    indices = [Index("chatId"), Index(value = ["chatId", "createdAt"])],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val role: String,
    val content: String,
    val createdAt: Long,
    val tokenCount: Int?,
    val tokensPerSecond: Float?,
)

@Entity(tableName = "models", indices = [Index("isActive")])
data class ModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val fileName: String,
    val filePath: String,
    val sourceUri: String?,
    val sizeBytes: Long,
    val quantization: String?,
    val isActive: Boolean,
    val importedAt: Long,
    val lastLoadedAt: Long?,
)

fun ChatSessionEntity.toDomain() = ChatSession(
    id = id,
    title = title,
    systemPrompt = systemPrompt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    chatId = chatId,
    role = MessageRole.valueOf(role),
    content = content,
    createdAt = createdAt,
    tokenCount = tokenCount,
    tokensPerSecond = tokensPerSecond,
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    chatId = chatId,
    role = role.name,
    content = content,
    createdAt = createdAt,
    tokenCount = tokenCount,
    tokensPerSecond = tokensPerSecond,
)

fun ModelEntity.toDomain() = LocalModel(
    id = id,
    displayName = displayName,
    fileName = fileName,
    filePath = filePath,
    sourceUri = sourceUri,
    sizeBytes = sizeBytes,
    quantization = quantization,
    isActive = isActive,
    importedAt = importedAt,
    lastLoadedAt = lastLoadedAt,
)
