package com.llamadroid.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.llamadroid.domain.settings.InferenceProfile
import com.llamadroid.domain.settings.InferenceSettings
import com.llamadroid.domain.settings.ThemeMode
import com.llamadroid.domain.settings.defaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val temperature = floatPreferencesKey("temperature")
        val topK = intPreferencesKey("top_k")
        val topP = floatPreferencesKey("top_p")
        val minP = floatPreferencesKey("min_p")
        val repeatPenalty = floatPreferencesKey("repeat_penalty")
        val maxTokens = intPreferencesKey("max_tokens")
        val contextSize = intPreferencesKey("context_size")
        val cpuThreads = intPreferencesKey("cpu_threads")
        val gpuLayers = intPreferencesKey("gpu_layers")
        val batchSize = intPreferencesKey("batch_size")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val haptics = booleanPreferencesKey("haptics")
        val themeMode = intPreferencesKey("theme_mode")
        val profile = intPreferencesKey("profile")
    }

    val settings: Flow<InferenceSettings> = context.settingsDataStore.data.map { prefs ->
        val defaults = InferenceSettings()
        InferenceSettings(
            temperature = prefs[Keys.temperature] ?: defaults.temperature,
            topK = prefs[Keys.topK] ?: defaults.topK,
            topP = prefs[Keys.topP] ?: defaults.topP,
            minP = prefs[Keys.minP] ?: defaults.minP,
            repeatPenalty = prefs[Keys.repeatPenalty] ?: defaults.repeatPenalty,
            maxTokens = prefs[Keys.maxTokens] ?: defaults.maxTokens,
            contextSize = prefs[Keys.contextSize] ?: defaults.contextSize,
            cpuThreads = prefs[Keys.cpuThreads] ?: defaults.cpuThreads,
            gpuLayers = prefs[Keys.gpuLayers] ?: defaults.gpuLayers,
            batchSize = prefs[Keys.batchSize] ?: defaults.batchSize,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            hapticsEnabled = prefs[Keys.haptics] ?: defaults.hapticsEnabled,
            themeMode = ThemeMode.entries.getOrElse(prefs[Keys.themeMode] ?: 0) { ThemeMode.System },
            profile = InferenceProfile.entries.getOrElse(prefs[Keys.profile] ?: 0) { InferenceProfile.Balanced },
        )
    }

    suspend fun update(settings: InferenceSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.temperature] = settings.temperature
            prefs[Keys.topK] = settings.topK
            prefs[Keys.topP] = settings.topP
            prefs[Keys.minP] = settings.minP
            prefs[Keys.repeatPenalty] = settings.repeatPenalty
            prefs[Keys.maxTokens] = settings.maxTokens
            prefs[Keys.contextSize] = settings.contextSize
            prefs[Keys.cpuThreads] = settings.cpuThreads
            prefs[Keys.gpuLayers] = settings.gpuLayers
            prefs[Keys.batchSize] = settings.batchSize
            prefs[Keys.keepScreenOn] = settings.keepScreenOn
            prefs[Keys.haptics] = settings.hapticsEnabled
            prefs[Keys.themeMode] = settings.themeMode.ordinal
            prefs[Keys.profile] = settings.profile.ordinal
        }
    }

    suspend fun applyProfile(profile: InferenceProfile) {
        update(profile.defaults())
    }
}
