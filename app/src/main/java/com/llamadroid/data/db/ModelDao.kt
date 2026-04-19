package com.llamadroid.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY isActive DESC, importedAt DESC")
    fun observeModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE isActive = 1 LIMIT 1")
    fun observeActiveModel(): Flow<ModelEntity?>

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getModel(id: Long): ModelEntity?

    @Insert
    suspend fun insert(model: ModelEntity): Long

    @Query("UPDATE models SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE models SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("UPDATE models SET lastLoadedAt = :timestamp WHERE id = :id")
    suspend fun markLoaded(id: Long, timestamp: Long)

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun delete(id: Long)
}
