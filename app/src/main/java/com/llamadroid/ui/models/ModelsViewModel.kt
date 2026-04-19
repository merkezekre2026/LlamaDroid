package com.llamadroid.ui.models

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.app.AppGraph
import com.llamadroid.domain.inference.EngineState
import com.llamadroid.domain.inference.toLoadConfig
import com.llamadroid.domain.model.LocalModel
import com.llamadroid.domain.model.ModelLoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModelsUiState(
    val models: List<LocalModel> = emptyList(),
    val activeModel: LocalModel? = null,
    val loadState: ModelLoadState = ModelLoadState.Unloaded,
    val error: String? = null,
    val warning: String? = null,
)

class ModelsViewModel(private val graph: AppGraph) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val warning = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModelsUiState> = combine(
        graph.modelRepository.models,
        graph.modelRepository.activeModel,
        graph.llamaEngine.state,
        error,
        warning,
    ) { models, active, engineState, errorValue, warningValue ->
        ModelsUiState(
            models = models,
            activeModel = active,
            loadState = when (engineState) {
                EngineState.Idle -> ModelLoadState.Unloaded
                EngineState.LoadingModel -> ModelLoadState.Loading
                EngineState.Ready, EngineState.Generating, EngineState.Cancelling -> active?.let(ModelLoadState::Ready) ?: ModelLoadState.Unloaded
                is EngineState.Error -> ModelLoadState.Error(engineState.message)
            },
            error = errorValue,
            warning = warningValue,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState())

    fun import(uri: Uri) {
        viewModelScope.launch {
            runCatching { graph.modelRepository.importModel(uri) }
                .onSuccess {
                    warning.value = it.warning
                    graph.modelRepository.setActiveModel(it.model.id)
                }
                .onFailure { error.value = it.message }
        }
    }

    fun setActive(model: LocalModel) {
        viewModelScope.launch {
            if (graph.llamaEngine.state.value is EngineState.Generating) {
                graph.llamaEngine.cancel()
            }
            graph.llamaEngine.unload()
            graph.modelRepository.setActiveModel(model.id)
        }
    }

    fun loadActive() {
        viewModelScope.launch {
            val model = graph.modelRepository.activeModel.first() ?: run {
                error.value = "Select a model first."
                return@launch
            }
            val settings = graph.settingsRepository.settings.first()
            graph.llamaEngine.loadModel(model.filePath, settings.toLoadConfig())
                .onSuccess { graph.modelRepository.markLoaded(model.id) }
                .onFailure { error.value = it.message }
        }
    }

    fun unload() {
        viewModelScope.launch { graph.llamaEngine.unload() }
    }

    fun delete(model: LocalModel) {
        viewModelScope.launch {
            if (uiState.value.activeModel?.id == model.id) {
                graph.llamaEngine.unload()
            }
            graph.modelRepository.deleteModel(model.id)
        }
    }
}
