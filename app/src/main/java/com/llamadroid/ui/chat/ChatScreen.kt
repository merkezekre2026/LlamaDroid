package com.llamadroid.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llamadroid.R
import com.llamadroid.app.AppGraph
import com.llamadroid.domain.chat.ChatMessage
import com.llamadroid.domain.chat.MessageRole
import com.llamadroid.domain.inference.EngineState
import com.llamadroid.ui.markdown.MarkdownText

@Composable
fun ChatRoute(graph: AppGraph, contentPadding: PaddingValues) {
    val viewModel = remember { ChatViewModel(graph) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(state, viewModel, contentPadding)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(state: ChatUiState, actions: ChatViewModel, contentPadding: PaddingValues) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val rows = state.messages + if (state.streamingText.isNotBlank()) {
        listOf(ChatMessage(-1, state.chatId ?: 0, MessageRole.Assistant, state.streamingText, System.currentTimeMillis()))
    } else {
        emptyList()
    }
    LaunchedEffect(rows.size, state.streamingText.length) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (rows.isNotEmpty() && lastVisible >= rows.lastIndex - 2) {
            listState.animateScrollToItem(rows.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LlamaDroid")
                        Text(
                            text = state.activeModel?.displayName ?: stringResource(R.string.no_model_title),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = actions::regenerate, enabled = state.messages.any { it.role == MessageRole.Assistant }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.regenerate))
                    }
                },
            )
        },
        bottomBar = {
            Composer(
                draft = state.draft,
                generating = state.engineState is EngineState.Generating || state.engineState is EngineState.Cancelling,
                onDraft = actions::updateDraft,
                onSend = actions::send,
                onStop = actions::stop,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (rows.isEmpty()) {
                EmptyChat(state.error)
            } else {
                MetricsRow(state)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    items(rows, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onCopy = { copyToClipboard(context, it.content) },
                            onEdit = actions::editAndResend,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChat(error: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.privacy_onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.privacy_onboarding_body), style = MaterialTheme.typography.bodyMedium)
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MetricsRow(state: ChatUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AssistChip(onClick = {}, label = { Text("%.1f tok/s".format(state.tokensPerSecond)) })
        AssistChip(onClick = {}, label = { Text("${state.promptMs}ms prompt") })
        AssistChip(onClick = {}, label = { Text("${state.generationMs}ms gen") })
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onCopy: (ChatMessage) -> Unit, onEdit: (ChatMessage) -> Unit) {
    val isUser = message.role == MessageRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 0.94f),
        ) {
            Column(Modifier.padding(14.dp)) {
                MarkdownText(message.content)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { onCopy(message) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy))
                    }
                    if (isUser) {
                        IconButton(onClick = { onEdit(message) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    generating: Boolean,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            modifier = Modifier.weight(1f),
            minLines = 1,
            maxLines = 6,
            placeholder = { Text(stringResource(R.string.message_hint)) },
        )
        Button(onClick = if (generating) onStop else onSend, enabled = generating || draft.isNotBlank()) {
            Icon(
                imageVector = if (generating) Icons.Outlined.StopCircle else Icons.Outlined.Send,
                contentDescription = if (generating) stringResource(R.string.stop) else stringResource(R.string.send),
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("LlamaDroid message", text))
}
