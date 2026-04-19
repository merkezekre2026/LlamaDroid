package com.llamadroid.data.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.llamadroid.data.db.ModelDao
import com.llamadroid.data.db.ModelEntity
import com.llamadroid.data.db.toDomain
import com.llamadroid.domain.model.LocalModel
import com.llamadroid.domain.model.ModelImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class ModelRepository(
    private val context: Context,
    private val modelDao: ModelDao,
) {
    val models: Flow<List<LocalModel>> = modelDao.observeModels().map { rows -> rows.map { it.toDomain() } }
    val activeModel: Flow<LocalModel?> = modelDao.observeActiveModel().map { it?.toDomain() }

    suspend fun importModel(uri: Uri): ModelImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val fileName = resolver.displayName(uri) ?: "model-${System.currentTimeMillis()}.gguf"
        require(fileName.lowercase(Locale.US).endsWith(".gguf")) { "Only GGUF files are supported." }

        val targetDir = File(context.filesDir, "models").apply { mkdirs() }
        val target = File(targetDir, sanitizeFileName(fileName))
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Unable to open selected model." }
            target.outputStream().use { output -> input.copyTo(output) }
        }

        val sizeBytes = target.length()
        val row = ModelEntity(
            displayName = fileName.removeSuffix(".gguf"),
            fileName = fileName,
            filePath = target.absolutePath,
            sourceUri = uri.toString(),
            sizeBytes = sizeBytes,
            quantization = deriveQuantization(fileName),
            isActive = false,
            importedAt = System.currentTimeMillis(),
            lastLoadedAt = null,
        )
        val id = modelDao.insert(row)
        val warning = if (sizeBytes > 4L * 1024L * 1024L * 1024L) {
            "This model is larger than 4 GB. Mid-range devices may fail to load it."
        } else {
            null
        }
        ModelImportResult(row.copy(id = id).toDomain(), warning)
    }

    suspend fun setActiveModel(id: Long) {
        modelDao.clearActive()
        modelDao.setActive(id)
    }

    suspend fun markLoaded(id: Long) {
        modelDao.markLoaded(id, System.currentTimeMillis())
    }

    suspend fun deleteModel(id: Long) {
        modelDao.getModel(id)?.let { row ->
            File(row.filePath).delete()
            modelDao.delete(id)
        }
    }

    private fun ContentResolver.displayName(uri: Uri): String? {
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return uri.lastPathSegment
    }

    private fun sanitizeFileName(name: String): String = name.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun deriveQuantization(name: String): String? {
        val match = Regex("""(?i)\b(Q[0-9]_[A-Z0-9_]+|IQ[0-9]_[A-Z0-9_]+|F16|F32)\b""").find(name)
        return match?.value?.uppercase(Locale.US)
    }
}
