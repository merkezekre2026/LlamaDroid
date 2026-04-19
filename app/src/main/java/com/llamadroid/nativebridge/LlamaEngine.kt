package com.llamadroid.nativebridge

import com.llamadroid.domain.inference.EngineState
import com.llamadroid.domain.inference.GenerationEvent
import com.llamadroid.domain.inference.GenerationParams
import com.llamadroid.domain.inference.ModelLoadConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LlamaEngine {
    val state: StateFlow<EngineState>
    suspend fun loadModel(path: String, config: ModelLoadConfig): Result<Unit>
    fun generate(prompt: String, params: GenerationParams): Flow<GenerationEvent>
    suspend fun cancel()
    suspend fun unload()
    suspend fun release()
}
