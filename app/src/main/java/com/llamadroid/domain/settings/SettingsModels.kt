package com.llamadroid.domain.settings

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class InferenceProfile {
    Balanced,
    Fast,
    Quality,
}

data class InferenceSettings(
    val temperature: Float = 0.8f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val minP: Float = 0.05f,
    val repeatPenalty: Float = 1.1f,
    val maxTokens: Int = 512,
    val contextSize: Int = 4096,
    val cpuThreads: Int = maxOf(2, Runtime.getRuntime().availableProcessors() / 2),
    val gpuLayers: Int = 0,
    val batchSize: Int = 512,
    val keepScreenOn: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val profile: InferenceProfile = InferenceProfile.Balanced,
)

fun InferenceProfile.defaults(): InferenceSettings = when (this) {
    InferenceProfile.Balanced -> InferenceSettings(profile = this)
    InferenceProfile.Fast -> InferenceSettings(
        temperature = 0.7f,
        topK = 32,
        topP = 0.9f,
        minP = 0.05f,
        repeatPenalty = 1.08f,
        maxTokens = 384,
        contextSize = 3072,
        cpuThreads = Runtime.getRuntime().availableProcessors(),
        gpuLayers = 0,
        batchSize = 768,
        profile = this,
    )
    InferenceProfile.Quality -> InferenceSettings(
        temperature = 0.85f,
        topK = 50,
        topP = 0.96f,
        minP = 0.03f,
        repeatPenalty = 1.12f,
        maxTokens = 768,
        contextSize = 8192,
        cpuThreads = maxOf(2, Runtime.getRuntime().availableProcessors() - 1),
        gpuLayers = 0,
        batchSize = 512,
        profile = this,
    )
}
