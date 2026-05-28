package me.ashishekka.echo.screens

import me.ashishekka.echo.shared.R as SharedR
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val haptic = LocalHapticFeedback.current
    val state by viewModel.state.collectAsState()
    val messages = state.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = context.getString(SharedR.string.common_error_prefix, error::class.simpleName ?: ""),
                actionLabel = context.getString(SharedR.string.common_dismiss)
            )
            viewModel.onIntent(ChatDetailIntent.ClearError)
        }
    }

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
                        text = if (state.isNewChat) stringResource(SharedR.string.chat_new_title) else (state.chat?.title ?: ""),
                        modifier = Modifier.clickable { if (!state.isNewChat) showRenameDialog = true }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(SharedR.string.chat_back_desc))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MessageInput(
                text = state.currentDraft,
                onTextChanged = { viewModel.onIntent(ChatDetailIntent.UpdateDraft(it)) },
                onSendClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onIntent(ChatDetailIntent.SendMessage(it)) 
                },
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
                    TypingIndicator(
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    
    @Composable
    fun typingDot(delay: Int) = transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delay)
        ),
        label = "dot"
    ).value

    Row(
        modifier = modifier
            .background(
                color = if (isSystemInDarkTheme()) DesignTokens.Colors.Dark.AgentBubble.toColor() else DesignTokens.Colors.AgentBubble.toColor(),
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
        Box(Modifier.size(6.dp).background(dotColor.copy(alpha = typingDot(0)), CircleShape))
        Box(Modifier.size(6.dp).background(dotColor.copy(alpha = typingDot(200)), CircleShape))
        Box(Modifier.size(6.dp).background(dotColor.copy(alpha = typingDot(400)), CircleShape))
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
        title = { Text(stringResource(SharedR.string.chat_rename_title)) },
        text = {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(SharedR.string.chat_rename_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(SharedR.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(SharedR.string.common_cancel))
            }
        }
    )
}

@Composable
fun MessageBubble(
    message: Message,
    onImageClick: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) {
        if (isDark) DesignTokens.Colors.Dark.UserBubble.toColor() else DesignTokens.Colors.UserBubble.toColor()
    } else {
        if (isDark) DesignTokens.Colors.Dark.AgentBubble.toColor() else DesignTokens.Colors.AgentBubble.toColor()
    }
    val textColor = if (isDark) DesignTokens.Colors.Dark.TextPrimary.toColor() else DesignTokens.Colors.TextPrimary.toColor()
    val secondaryTextColor = if (isDark) DesignTokens.Colors.Dark.TextSecondary.toColor() else DesignTokens.Colors.TextSecondary.toColor()
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
                        color = secondaryTextColor,
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
                    color = secondaryTextColor,
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
        title = { Text(stringResource(SharedR.string.chat_choose_source)) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(SharedR.string.chat_gallery)) },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onGalleryClick)
                )
                ListItem(
                    headlineContent = { Text(stringResource(SharedR.string.chat_camera)) },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    modifier = Modifier.clickable(onClick = onCameraClick)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(SharedR.string.common_cancel)) }
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
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime)
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = onAttachClick,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        Icons.Default.AttachFile, 
                        contentDescription = stringResource(SharedR.string.chat_attach_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                TextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text(stringResource(SharedR.string.chat_message_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                )
                IconButton(
                    onClick = { if (text.isNotBlank()) onSendClick(text) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(SharedR.string.chat_send_desc),
                            tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
