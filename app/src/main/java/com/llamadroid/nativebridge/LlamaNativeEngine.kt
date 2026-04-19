package com.llamadroid.nativebridge

import com.llamadroid.domain.inference.EngineState
import com.llamadroid.domain.inference.GenerationEvent
import com.llamadroid.domain.inference.GenerationParams
import com.llamadroid.domain.inference.ModelLoadConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LlamaNativeEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : LlamaEngine {
    private val mutex = Mutex()
    private val handle: Long = LlamaNativeBindings.createEngine()
    private val mutableState = MutableStateFlow<EngineState>(EngineState.Idle)

    override val state: StateFlow<EngineState> = mutableState

    override suspend fun loadModel(path: String, config: ModelLoadConfig): Result<Unit> = withContext(dispatcher) {
        mutex.withLock {
            mutableState.value = EngineState.LoadingModel
            val error = LlamaNativeBindings.loadModel(handle, path, config)
            if (error == null) {
                mutableState.value = EngineState.Ready
                Result.success(Unit)
            } else {
                mutableState.value = EngineState.Error(error)
                Result.failure(IllegalStateException(error))
            }
        }
    }

    override fun generate(prompt: String, params: GenerationParams): Flow<GenerationEvent> = callbackFlow {
        val callback = object : NativeGenerationCallback {
            override fun onToken(text: String) {
                trySend(GenerationEvent.Token(text))
            }

            override fun onMetrics(promptMs: Long, generationMs: Long, outputTokens: Int, tokensPerSecond: Float) {
                trySend(GenerationEvent.Metrics(promptMs, generationMs, outputTokens, tokensPerSecond))
            }

            override fun onComplete() {
                trySend(GenerationEvent.Complete)
                close()
            }

            override fun onCancelled() {
                trySend(GenerationEvent.Cancelled)
                close()
            }

            override fun onError(message: String) {
                trySend(GenerationEvent.Error(message))
                close(IllegalStateException(message))
            }
        }

        val job = CoroutineScope(dispatcher).launch {
            mutex.withLock {
                mutableState.value = EngineState.Generating
                val error = LlamaNativeBindings.generate(handle, prompt, params, callback)
                if (error != null) {
                    mutableState.value = EngineState.Error(error)
                    trySend(GenerationEvent.Error(error))
                } else if (mutableState.value !is EngineState.Error) {
                    mutableState.value = EngineState.Ready
                }
                close()
            }
        }

        awaitClose {
            if (job.isActive) {
                LlamaNativeBindings.cancel(handle)
            }
        }
    }.buffer(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun cancel() = withContext(dispatcher) {
        mutableState.value = EngineState.Cancelling
        LlamaNativeBindings.cancel(handle)
    }

    override suspend fun unload() = withContext(dispatcher) {
        mutex.withLock {
            LlamaNativeBindings.unloadModel(handle)
            mutableState.value = EngineState.Idle
        }
    }

    override suspend fun release() = withContext(dispatcher) {
        mutex.withLock {
            LlamaNativeBindings.release(handle)
            mutableState.value = EngineState.Idle
        }
    }
}
