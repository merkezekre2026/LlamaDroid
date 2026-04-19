package com.llamadroid.ui.models

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llamadroid.R
import com.llamadroid.app.AppGraph
import com.llamadroid.domain.model.LocalModel
import com.llamadroid.domain.model.ModelLoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(graph: AppGraph, contentPadding: PaddingValues) {
    val viewModel = remember { ModelsViewModel(graph) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.models)) },
                actions = {
                    Button(onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) }) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null)
                        Text(stringResource(R.string.import_model))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            state.warning?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::loadActive, enabled = state.activeModel != null && state.loadState !is ModelLoadState.Loading) {
                    Text(stringResource(R.string.load_model))
                }
                OutlinedButton(onClick = viewModel::unload) { Text(stringResource(R.string.unload_model)) }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                items(state.models, key = { it.id }) { model ->
                    ModelRow(
                        model = model,
                        isActive = model.id == state.activeModel?.id,
                        onSelect = { viewModel.setActive(model) },
                        onDelete = { viewModel.delete(model) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelRow(model: LocalModel, isActive: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(model.displayName, style = MaterialTheme.typography.titleMedium)
                Text("${formatBytes(model.sizeBytes)}${model.quantization?.let { " - $it" }.orEmpty()}")
                if (isActive) Text(stringResource(R.string.active_model), color = MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(onClick = onSelect, enabled = !isActive) {
                Text(stringResource(R.string.use))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    val mb = bytes / 1024.0 / 1024.0
    return if (gb >= 1) "%.2f GB".format(gb) else "%.1f MB".format(mb)
}
