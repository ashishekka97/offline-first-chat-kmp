package me.ashishekka.echo.screens

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.screens.home.HomeIntent
import me.ashishekka.echo.shared.screens.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onChatClick: (ChatId) -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val state by viewModel.state.collectAsState()
    val chats = state.chats.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = "Error: ${error::class.simpleName}",
                actionLabel = "Dismiss"
            )
            viewModel.onIntent(HomeIntent.ClearError)
        }
    }

    if (state.pendingDeleteChatId != null) {
        DeleteConfirmationDialog(
            onConfirm = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.onIntent(HomeIntent.DeletePendingChat) 
            },
            onDismiss = { viewModel.onIntent(HomeIntent.CancelDelete) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Echo") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChatClick) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        },
        modifier = modifier
    ) { padding ->
        val isInitialLoad = chats.loadState.refresh is LoadState.Loading && chats.itemCount == 0
        
        if (isInitialLoad) {
            // Instant startup means we show empty space while Paging hits the DB
        } else if (chats.itemCount == 0) {
            EmptyScreenContent(
                modifier = Modifier.padding(padding),
                text = "No chats yet. Start a new one!"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            ) {
                items(
                    count = chats.itemCount,
                    key = chats.itemKey { it.id.value },
                    contentType = chats.itemContentType { "chat_item" }
                ) { index ->
                    val chat = chats[index]
                    if (chat != null) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.onIntent(HomeIntent.ConfirmDelete(chat.id))
                                    false // Don't dismiss until confirmed by dialog
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.error)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onError
                                    )
                                }
                            },
                            content = {
                                ChatItem(
                                    chat = chat,
                                    onClick = { onChatClick(chat.id) },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Chat") },
        text = { Text("Are you sure you want to delete this conversation? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChatItem(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = chat.displayTimestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            val isDraft = !chat.draft.isNullOrBlank()
            val lastMessageText = if (isDraft) {
                "Draft: ${chat.draft}"
            } else {
                chat.lastMessage ?: "No messages yet"
            }
            
            Text(
                text = lastMessageText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDraft) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isDraft) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
