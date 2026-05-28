package me.ashishekka.echo.shared.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.model.Message
import me.ashishekka.echo.shared.domain.onFailure
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.usecase.GetChatByIdUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedMessagesUseCase
import me.ashishekka.echo.shared.domain.usecase.SendMessageUseCase
import me.ashishekka.echo.shared.domain.usecase.StartChatUseCase

/**
 * State for the Chat Detail screen.
 */
data class ChatDetailState(
    val chat: Chat? = null,
    val messages: Flow<PagingData<Message>> = MutableStateFlow(PagingData.empty()),
    val isNewChat: Boolean = false,
    val error: AppError? = null
)

/**
 * Intents for the Chat Detail screen.
 */
sealed interface ChatDetailIntent {
    data class SendMessage(val text: String) : ChatDetailIntent
    data object ClearError : ChatDetailIntent
}

/**
 * Side effects (one-time events) for the Chat Detail screen.
 */
sealed interface ChatDetailSideEffect {
    data object ScrollToBottom : ChatDetailSideEffect
}

/**
 * Shared ViewModel for the Chat Detail screen, managing message history and sending.
 */
class ChatDetailViewModel(
    private val chatId: String,
    private val getChatByIdUseCase: GetChatByIdUseCase,
    private val getPagedMessagesUseCase: GetPagedMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val startChatUseCase: StartChatUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ChatDetailState())
    val state: StateFlow<ChatDetailState> = _state.asStateFlow()

    private val _sideEffect = Channel<ChatDetailSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<ChatDetailSideEffect> = _sideEffect.receiveAsFlow()

    init {
        observeChat()
        loadMessages()
    }

    private fun observeChat() {
        getChatByIdUseCase(chatId)
            .onEach { chat ->
                _state.value = _state.value.copy(
                    chat = chat,
                    isNewChat = chat == null
                )
            }
            .launchIn(viewModelScope)
    }

    private fun loadMessages() {
        _state.value = _state.value.copy(
            messages = getPagedMessagesUseCase(chatId).cachedIn(viewModelScope)
        )
    }

    fun onIntent(intent: ChatDetailIntent) {
        when (intent) {
            is ChatDetailIntent.SendMessage -> sendMessage(intent.text)
            is ChatDetailIntent.ClearError -> clearError()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val result = if (_state.value.isNewChat) {
                startChatUseCase(
                    chatId = chatId,
                    title = "New Chat", // Default title for MVP
                    participantIds = listOf(Constants.CURRENT_USER_ID, Constants.DEFAULT_AGENT_ID),
                    messageId = Uuid.random().toString(),
                    message = text,
                    senderId = Constants.CURRENT_USER_ID
                )
            } else {
                sendMessageUseCase(
                    id = Uuid.random().toString(),
                    chatId = chatId,
                    senderId = Constants.CURRENT_USER_ID,
                    message = text
                )
            }

            result
                .onSuccess {
                    _sideEffect.send(ChatDetailSideEffect.ScrollToBottom)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error)
                }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
