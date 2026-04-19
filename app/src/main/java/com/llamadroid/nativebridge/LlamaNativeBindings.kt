package com.llamadroid.nativebridge

import com.llamadroid.domain.inference.GenerationParams
import com.llamadroid.domain.inference.ModelLoadConfig

internal object LlamaNativeBindings {
    init {
        System.loadLibrary("llamadroid")
    }

    @JvmStatic external fun createEngine(): Long
    @JvmStatic external fun loadModel(handle: Long, path: String, config: ModelLoadConfig): String?
    @JvmStatic external fun generate(handle: Long, prompt: String, params: GenerationParams, callback: NativeGenerationCallback): String?
    @JvmStatic external fun cancel(handle: Long)
    @JvmStatic external fun unloadModel(handle: Long)
    @JvmStatic external fun release(handle: Long)
}

internal interface NativeGenerationCallback {
    fun onToken(text: String)
    fun onMetrics(promptMs: Long, generationMs: Long, outputTokens: Int, tokensPerSecond: Float)
    fun onComplete()
    fun onCancelled()
    fun onError(message: String)
}
