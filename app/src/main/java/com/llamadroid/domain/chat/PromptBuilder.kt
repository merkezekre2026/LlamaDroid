package com.llamadroid.domain.chat

class PromptBuilder {
    fun build(
        messages: List<ChatMessage>,
        systemPrompt: String?,
        maxCharacters: Int = 24_000,
    ): String {
        val header = buildString {
            val prompt = systemPrompt?.trim().orEmpty()
            if (prompt.isNotBlank()) {
                append("<|system|>\n")
                append(prompt)
                append("\n")
            }
        }

        val turns = messages
            .filterNot { it.role == MessageRole.System }
            .joinToString(separator = "\n") { message ->
                when (message.role) {
                    MessageRole.User -> "<|user|>\n${message.content.trim()}\n<|assistant|>"
                    MessageRole.Assistant -> message.content.trim()
                    MessageRole.System -> ""
                }
            }

        val prompt = header + turns + "\n"
        return if (prompt.length <= maxCharacters) {
            prompt
        } else {
            header + prompt.takeLast(maxCharacters - header.length).trimStart()
        }
    }
}
