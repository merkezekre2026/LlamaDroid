package com.llamadroid.ui.conversations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llamadroid.R
import com.llamadroid.app.AppGraph
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(graph: AppGraph, contentPadding: PaddingValues, onOpenChat: (Long) -> Unit) {
    val sessions by graph.chatRepository.sessions.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val formatter = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.conversations)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    val id = graph.chatRepository.createChat()
                    onOpenChat(id)
                }
            }) {
                Icon(Icons.Outlined.NoteAdd, contentDescription = stringResource(R.string.new_chat))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sessions, key = { it.id }) { session ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(session.title)
                            Text(formatter.format(Date(session.updatedAt)))
                        }
                        Button(onClick = { onOpenChat(session.id) }) {
                            Text(stringResource(R.string.open))
                        }
                        IconButton(onClick = { scope.launch { graph.chatRepository.delete(session.id) } }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}
