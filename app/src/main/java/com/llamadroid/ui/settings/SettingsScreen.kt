package com.llamadroid.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llamadroid.R
import com.llamadroid.app.AppGraph
import com.llamadroid.domain.settings.InferenceProfile
import com.llamadroid.domain.settings.InferenceSettings
import com.llamadroid.domain.settings.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(graph: AppGraph, contentPadding: PaddingValues) {
    val settings by graph.settingsRepository.settings.collectAsStateWithLifecycle(InferenceSettings())
    val scope = rememberCoroutineScope()
    fun update(block: (InferenceSettings) -> InferenceSettings) {
        scope.launch { graph.settingsRepository.update(block(settings)) }
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section(stringResource(R.string.generation)) {
                ChipRow(InferenceProfile.entries.map { it.name }, settings.profile.name) { selected ->
                    scope.launch { graph.settingsRepository.applyProfile(InferenceProfile.valueOf(selected)) }
                }
                SettingSlider("Temperature", settings.temperature, 0f, 1.5f) { value -> update { it.copy(temperature = value) } }
                SettingSlider("Top P", settings.topP, 0.1f, 1f) { value -> update { it.copy(topP = value) } }
                SettingSlider("Min P", settings.minP, 0f, 0.3f) { value -> update { it.copy(minP = value) } }
                SettingSlider("Repeat penalty", settings.repeatPenalty, 0.8f, 1.5f) { value -> update { it.copy(repeatPenalty = value) } }
                SettingSlider("Max tokens", settings.maxTokens.toFloat(), 64f, 2048f) { value -> update { it.copy(maxTokens = value.toInt()) } }
            }
            Section(stringResource(R.string.performance)) {
                SettingSlider("Context", settings.contextSize.toFloat(), 1024f, 8192f) { value -> update { it.copy(contextSize = value.toInt()) } }
                SettingSlider("Threads", settings.cpuThreads.toFloat(), 1f, Runtime.getRuntime().availableProcessors().toFloat()) { value -> update { it.copy(cpuThreads = value.toInt()) } }
                SettingSlider("GPU layers", settings.gpuLayers.toFloat(), 0f, 99f) { value -> update { it.copy(gpuLayers = value.toInt()) } }
                SettingSlider("Batch", settings.batchSize.toFloat(), 64f, 1024f) { value -> update { it.copy(batchSize = value.toInt()) } }
            }
            Section(stringResource(R.string.appearance)) {
                ChipRow(ThemeMode.entries.map { it.name }, settings.themeMode.name) { selected ->
                    update { it.copy(themeMode = ThemeMode.valueOf(selected)) }
                }
                ToggleRow("Keep screen on while generating", settings.keepScreenOn) { update { it.copy(keepScreenOn = !it.keepScreenOn) } }
                ToggleRow("Haptic feedback", settings.hapticsEnabled) { update { it.copy(hapticsEnabled = !it.hapticsEnabled) } }
            }
            Section(stringResource(R.string.privacy)) {
                Text(stringResource(R.string.privacy_onboarding_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SettingSlider(label: String, value: Float, from: Float, to: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${if (to > 10) value.toInt() else "%.2f".format(value)}")
        Slider(value = value.coerceIn(from, to), onValueChange = onChange, valueRange = from..to)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ChipRow(values: List<String>, selected: String, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(value) },
            )
        }
    }
}
