package com.llamadroid.domain.chat

import com.llamadroid.data.chat.ChatRepository

class CreateChatUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(title: String = "New chat", systemPrompt: String? = null): Long =
        repository.createChat(title, systemPrompt)
}

class ExportChatMarkdownUseCase {
    operator fun invoke(chat: ChatWithMessages): String = buildString {
        appendLine("# ${chat.session.title}")
        chat.session.systemPrompt?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("## System")
            appendLine(it)
        }
        chat.messages.forEach { message ->
            appendLine()
            appendLine("## ${message.role.name}")
            appendLine(message.content)
        }
    }
}
