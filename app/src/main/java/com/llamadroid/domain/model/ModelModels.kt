package com.llamadroid.domain.model

data class LocalModel(
    val id: Long,
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

sealed interface ModelLoadState {
    data object Unloaded : ModelLoadState
    data object Loading : ModelLoadState
    data class Ready(val model: LocalModel) : ModelLoadState
    data class Error(val message: String) : ModelLoadState
}

data class ModelImportResult(
    val model: LocalModel,
    val warning: String?,
)
