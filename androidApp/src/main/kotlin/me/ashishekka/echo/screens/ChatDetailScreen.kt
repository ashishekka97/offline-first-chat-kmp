package me.ashishekka.echo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.Message
import me.ashishekka.echo.shared.domain.model.MessageType
import me.ashishekka.echo.shared.screens.chat.ChatDetailIntent
import me.ashishekka.echo.shared.screens.chat.ChatDetailSideEffect
import me.ashishekka.echo.shared.screens.chat.ChatDetailViewModel
import me.ashishekka.echo.shared.util.DesignTokens
import me.ashishekka.echo.ui.theme.toColor
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatDetailViewModel = koinViewModel(parameters = { parametersOf(ChatId(chatId)) })
) {
    val state by viewModel.state.collectAsState()
    val messages = state.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ChatDetailSideEffect.ScrollToBottom -> {
                    if (messages.itemCount > 0) {
                        listState.animateScrollToItem(messages.itemCount - 1)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.chat?.title ?: "New Chat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                text = state.currentDraft,
                onTextChanged = { viewModel.onIntent(ChatDetailIntent.UpdateDraft(it)) },
                onSendClick = { viewModel.onIntent(ChatDetailIntent.SendMessage(it)) },
                onAttachClick = { /* TODO: Phase 5.6 */ }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = false
        ) {
            items(
                count = messages.itemCount,
                key = messages.itemKey { it.id.value }
            ) { index ->
                val message = messages[index]
                if (message != null) {
                    MessageBubble(message = message)
                }
            }

            if (state.isAgentTyping) {
                item {
                    Text(
                        text = "Agent is typing...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInput(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: (String) -> Unit,
    onAttachClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAttachClick) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach")
            }
            TextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text("Message") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            IconButton(
                onClick = { onSendClick(text) },
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) {
        DesignTokens.Colors.UserBubble.toColor()
    } else {
        DesignTokens.Colors.AgentBubble.toColor()
    }
    val textColor = DesignTokens.Colors.TextPrimary.toColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.type == MessageType.FILE && message.file != null) {
                    // Placeholder for image rendering
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                    )
                    if (message.message.isNotEmpty()) {
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
                Text(
                    text = message.displayTimestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = DesignTokens.Colors.TextSecondary.toColor(),
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}
