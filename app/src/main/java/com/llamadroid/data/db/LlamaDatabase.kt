package com.llamadroid.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class, ModelEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LlamaDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun modelDao(): ModelDao

    companion object {
        fun create(context: Context): LlamaDatabase = Room.databaseBuilder(
            context.applicationContext,
            LlamaDatabase::class.java,
            "llamadroid.db",
        ).build()
    }
}
