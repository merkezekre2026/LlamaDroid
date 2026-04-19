package com.llamadroid.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.app.AppGraph
import com.llamadroid.domain.chat.ChatMessage
import com.llamadroid.domain.chat.MessageRole
import com.llamadroid.domain.chat.PromptBuilder
import com.llamadroid.domain.inference.EngineState
import com.llamadroid.domain.inference.GenerationEvent
import com.llamadroid.domain.inference.toGenerationParams
import com.llamadroid.domain.inference.toLoadConfig
import com.llamadroid.domain.model.LocalModel
import com.llamadroid.domain.settings.InferenceSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val chatId: Long? = null,
    val title: String = "New chat",
    val messages: List<ChatMessage> = emptyList(),
    val activeModel: LocalModel? = null,
    val engineState: EngineState = EngineState.Idle,
    val settings: InferenceSettings = InferenceSettings(),
    val draft: String = "",
    val streamingText: String = "",
    val promptMs: Long = 0,
    val generationMs: Long = 0,
    val tokensPerSecond: Float = 0f,
    val error: String? = null,
)

class ChatViewModel(private val graph: AppGraph) : ViewModel() {
    private val promptBuilder = PromptBuilder()
    private val selectedChatId = graph.selectedChatId
    private val draft = MutableStateFlow("")
    private val streamingText = MutableStateFlow("")
    private val metrics = MutableStateFlow(Triple(0L, 0L, 0f))
    private val error = MutableStateFlow<String?>(null)
    private val messageVersion = MutableStateFlow(0)
    private var generationJob: Job? = null

    val uiState: StateFlow<ChatUiState> = combine(
        selectedChatId,
        draft,
        streamingText,
        metrics,
        error,
        messageVersion,
        graph.modelRepository.activeModel,
        graph.llamaEngine.state,
        graph.settingsRepository.settings,
    ) { values ->
        val chatId = values[0] as Long?
        val draftValue = values[1] as String
        val streamValue = values[2] as String
        val metricValue = values[3] as Triple<Long, Long, Float>
        val errorValue = values[4] as String?
        val model = values[6] as LocalModel?
        val engineState = values[7] as EngineState
        val settings = values[8] as InferenceSettings
        val messages = chatId?.let { graph.chatRepository.messages(it) }.orEmpty()
        ChatUiState(
            chatId = chatId,
            messages = messages,
            activeModel = model,
            engineState = engineState,
            settings = settings,
            draft = draftValue,
            streamingText = streamValue,
            promptMs = metricValue.first,
            generationMs = metricValue.second,
            tokensPerSecond = metricValue.third,
            error = errorValue,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    init {
        viewModelScope.launch {
            if (selectedChatId.value == null) {
                val firstSession = graph.chatRepository.sessions.first().firstOrNull()
                selectedChatId.value = firstSession?.id ?: graph.chatRepository.createChat()
            }
        }
    }

    fun updateDraft(value: String) {
        draft.value = value
    }

    fun selectChat(id: Long) {
        selectedChatId.value = id
    }

    fun newChat() {
        viewModelScope.launch {
            selectedChatId.value = graph.chatRepository.createChat()
            draft.value = ""
            streamingText.value = ""
            messageVersion.value++
        }
    }

    fun send() {
        val content = draft.value.trim()
        if (content.isBlank() || generationJob?.isActive == true) return
        generationJob = viewModelScope.launch {
            val chatId = selectedChatId.value ?: graph.chatRepository.createChat().also { selectedChatId.value = it }
            val model = graph.modelRepository.activeModel.first()
            if (model == null) {
                error.value = "Import and select a GGUF model first."
                return@launch
            }
            val settings = graph.settingsRepository.settings.first()
            if (graph.llamaEngine.state.value !is EngineState.Ready) {
                graph.llamaEngine.loadModel(model.filePath, settings.toLoadConfig())
                    .onFailure {
                        error.value = it.message
                        return@launch
                    }
                graph.modelRepository.markLoaded(model.id)
            }

            draft.value = ""
            streamingText.value = ""
            metrics.value = Triple(0L, 0L, 0f)
            error.value = null
            graph.chatRepository.addMessage(chatId, MessageRole.User, content)
            messageVersion.value++
            val session = graph.chatRepository.observeSession(chatId).first()
            val history = graph.chatRepository.messages(chatId)
            val prompt = promptBuilder.build(history, session?.systemPrompt)
            val builder = StringBuilder()
            var tokenCount = 0

            graph.llamaEngine.generate(prompt, settings.toGenerationParams()).collect { event ->
                when (event) {
                    is GenerationEvent.Token -> {
                        builder.append(event.text)
                        tokenCount++
                        streamingText.value = builder.toString()
                    }
                    is GenerationEvent.Metrics -> {
                        metrics.value = Triple(event.promptMs, event.generationMs, event.tokensPerSecond)
                    }
                    GenerationEvent.Complete -> {
                        val response = builder.toString().trim()
                        if (response.isNotBlank()) {
                            graph.chatRepository.addMessage(
                                chatId = chatId,
                                role = MessageRole.Assistant,
                                content = response,
                                tokenCount = tokenCount,
                                tokensPerSecond = metrics.value.third,
                            )
                        }
                        streamingText.value = ""
                        messageVersion.value++
                    }
                    GenerationEvent.Cancelled -> {
                        val partial = builder.toString().trim()
                        if (partial.isNotBlank()) {
                            graph.chatRepository.addMessage(chatId, MessageRole.Assistant, partial, tokenCount, metrics.value.third)
                        }
                        streamingText.value = ""
                        messageVersion.value++
                    }
                    is GenerationEvent.Error -> {
                        error.value = event.message
                        streamingText.value = ""
                    }
                }
            }
        }
    }

    fun stop() {
        viewModelScope.launch { graph.llamaEngine.cancel() }
    }

    fun regenerate() {
        viewModelScope.launch {
            val chatId = selectedChatId.value ?: return@launch
            val messages = graph.chatRepository.messages(chatId)
            val lastAssistant = messages.lastOrNull { it.role == MessageRole.Assistant } ?: return@launch
            graph.chatRepository.deleteFrom(chatId, lastAssistant.id)
            messageVersion.value++
            val lastUser = messages.lastOrNull { it.role == MessageRole.User } ?: return@launch
            draft.value = lastUser.content
            send()
        }
    }

    fun editAndResend(message: ChatMessage) {
        if (message.role != MessageRole.User) return
        viewModelScope.launch {
            graph.chatRepository.deleteFrom(message.chatId, message.id)
            draft.value = message.content
            messageVersion.value++
        }
    }
}
