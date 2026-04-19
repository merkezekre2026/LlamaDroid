package com.llamadroid.app

import android.content.Context
import com.llamadroid.data.chat.ChatRepository
import com.llamadroid.data.db.LlamaDatabase
import com.llamadroid.data.model.ModelRepository
import com.llamadroid.data.settings.SettingsRepository
import com.llamadroid.nativebridge.LlamaEngine
import com.llamadroid.nativebridge.LlamaNativeEngine
import kotlinx.coroutines.flow.MutableStateFlow

class AppGraph private constructor(context: Context) {
    val dispatchers = AppDispatchers()
    val database = LlamaDatabase.create(context)
    val chatRepository = ChatRepository(database.chatDao())
    val modelRepository = ModelRepository(context.applicationContext, database.modelDao())
    val settingsRepository = SettingsRepository(context.applicationContext)
    val llamaEngine: LlamaEngine = LlamaNativeEngine(dispatchers.default)
    val selectedChatId = MutableStateFlow<Long?>(null)

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph = instance ?: synchronized(this) {
            instance ?: AppGraph(context.applicationContext).also { instance = it }
        }
    }
}
