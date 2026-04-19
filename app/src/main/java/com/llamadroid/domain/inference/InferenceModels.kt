package com.llamadroid.domain.inference

import com.llamadroid.domain.settings.InferenceSettings

sealed interface EngineState {
    data object Idle : EngineState
    data object LoadingModel : EngineState
    data object Ready : EngineState
    data object Generating : EngineState
    data object Cancelling : EngineState
    data class Error(val message: String) : EngineState
}

data class ModelLoadConfig(
    val contextSize: Int,
    val batchSize: Int,
    val cpuThreads: Int,
    val gpuLayers: Int,
)

data class GenerationParams(
    val temperature: Float,
    val topK: Int,
    val topP: Float,
    val minP: Float,
    val repeatPenalty: Float,
    val maxTokens: Int,
    val cpuThreads: Int,
)

fun InferenceSettings.toLoadConfig(): ModelLoadConfig = ModelLoadConfig(
    contextSize = contextSize,
    batchSize = batchSize,
    cpuThreads = cpuThreads,
    gpuLayers = gpuLayers,
)

fun InferenceSettings.toGenerationParams(): GenerationParams = GenerationParams(
    temperature = temperature,
    topK = topK,
    topP = topP,
    minP = minP,
    repeatPenalty = repeatPenalty,
    maxTokens = maxTokens,
    cpuThreads = cpuThreads,
)

sealed interface GenerationEvent {
    data class Token(val text: String) : GenerationEvent
    data class Metrics(
        val promptMs: Long,
        val generationMs: Long,
        val outputTokens: Int,
        val tokensPerSecond: Float,
    ) : GenerationEvent
    data class Error(val message: String) : GenerationEvent
    data object Cancelled : GenerationEvent
    data object Complete : GenerationEvent
}
