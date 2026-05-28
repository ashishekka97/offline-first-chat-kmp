package me.ashishekka.echo.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import coil3.compose.AsyncImage
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

import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.core.content.FileProvider
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    onBackClick: () -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatDetailViewModel = koinViewModel(parameters = { parametersOf(ChatId(chatId)) })
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val messages = state.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    // Detect if keyboard is visible
    val isKeyboardVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

    // Auto-scroll logic for new messages, agent typing, and keyboard appearance
    LaunchedEffect(messages.itemCount, state.isAgentTyping, isKeyboardVisible) {
        if (messages.itemCount > 0) {
            listState.animateScrollToItem(messages.itemCount - 1)
        }
    }

    var showSourcePicker by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(ChatDetailIntent.SendMessage("", uri.toString()))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            viewModel.onIntent(ChatDetailIntent.SendMessage("", capturedImageUri.toString()))
        }
    }

    fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
    }

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

    if (showSourcePicker) {
        AttachmentSourceDialog(
            onDismiss = { showSourcePicker = false },
            onGalleryClick = {
                showSourcePicker = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCameraClick = {
                showSourcePicker = false
                val uri = getTmpFileUri()
                capturedImageUri = uri
                cameraLauncher.launch(uri)
            }
        )
    }

    if (showRenameDialog && state.chat != null) {
        RenameChatDialog(
            initialTitle = state.chat!!.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                showRenameDialog = false
                viewModel.onIntent(ChatDetailIntent.RenameChat(newTitle))
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = state.chat?.title ?: "New Chat",
                        modifier = Modifier.clickable { if (!state.isNewChat) showRenameDialog = true }
                    ) 
                },
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
                onAttachClick = { showSourcePicker = true }
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
                    MessageBubble(
                        message = message,
                        onImageClick = onImageClick
                    )
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
fun RenameChatDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Chat") },
        text = {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Chat Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
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
fun MessageBubble(
    message: Message,
    onImageClick: (String) -> Unit
) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) {
        DesignTokens.Colors.UserBubble.toColor()
    } else {
        DesignTokens.Colors.AgentBubble.toColor()
    }
    val textColor = DesignTokens.Colors.TextPrimary.toColor()
    val cornerRadius = DesignTokens.Shape.BubbleCornerRadius.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomStart = if (message.isFromMe) cornerRadius else 4.dp,
                bottomEnd = if (message.isFromMe) 4.dp else cornerRadius
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.type == MessageType.FILE && message.file != null) {
                    AsyncImage(
                        model = message.file?.fullPath ?: message.file?.path,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable {
                                val url = message.file?.fullPath ?: message.file?.path
                                if (url != null) onImageClick(url)
                            }
                    )
                    if (message.message.isNotEmpty()) {
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = message.displaySize,
                        style = MaterialTheme.typography.labelSmall,
                        color = DesignTokens.Colors.TextSecondary.toColor(),
                        modifier = Modifier.padding(top = 2.dp)
                    )
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

@Composable
fun AttachmentSourceDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Source") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Gallery") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onGalleryClick)
                )
                ListItem(
                    headlineContent = { Text("Camera") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onCameraClick)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
