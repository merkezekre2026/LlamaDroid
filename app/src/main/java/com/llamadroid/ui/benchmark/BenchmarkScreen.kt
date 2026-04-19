package com.llamadroid.ui.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.llamadroid.R
import com.llamadroid.app.AppGraph
import com.llamadroid.domain.inference.GenerationEvent
import com.llamadroid.domain.inference.toGenerationParams
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(graph: AppGraph, contentPadding: PaddingValues) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }
    val runningText = stringResource(R.string.benchmark_running)
    val noMetricsText = stringResource(R.string.benchmark_no_metrics)
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.benchmark)) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(result.ifBlank { stringResource(R.string.benchmark_idle) })
            Button(onClick = {
                scope.launch {
                    result = runningText
                    val settings = graph.settingsRepository.settings.first()
                    var metrics: GenerationEvent.Metrics? = null
                    graph.llamaEngine.generate(
                        prompt = "<|user|>\nWrite three concise facts about offline AI.\n<|assistant|>",
                        params = settings.toGenerationParams().copy(maxTokens = 96),
                    ).collect { event ->
                        if (event is GenerationEvent.Metrics) metrics = event
                    }
                    result = metrics?.let {
                        "Prompt: ${it.promptMs}ms\nGeneration: ${it.generationMs}ms\nSpeed: %.1f tok/s".format(it.tokensPerSecond)
                    } ?: noMetricsText
                }
            }) {
                Text(stringResource(R.string.run_benchmark))
            }
        }
    }
}
