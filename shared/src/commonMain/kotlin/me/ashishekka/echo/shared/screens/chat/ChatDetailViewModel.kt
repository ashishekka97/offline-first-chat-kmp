package me.ashishekka.echo.shared.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.Message
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.Participant
import me.ashishekka.echo.shared.domain.onFailure
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.ParticipantRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.IdGenerator
import me.ashishekka.echo.shared.domain.usecase.GetChatByIdUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedMessagesUseCase
import me.ashishekka.echo.shared.domain.usecase.SendMessageUseCase
import me.ashishekka.echo.shared.domain.usecase.StartChatUseCase

/**
 * State for the Chat Detail screen.
 */
data class ChatDetailState(
    val chat: Chat? = null,
    val agent: Participant? = null,
    val messages: Flow<PagingData<Message>> = MutableStateFlow(PagingData.empty()),
    val isNewChat: Boolean = false,
    val isAgentTyping: Boolean = false,
    val currentDraft: String = "",
    val error: AppError? = null
)

/**
 * Intents for the Chat Detail screen.
 */
sealed interface ChatDetailIntent {
    data class SendMessage(val text: String, val localMediaPath: String? = null) : ChatDetailIntent
    data class RenameChat(val newTitle: String) : ChatDetailIntent
    data class UpdateDraft(val text: String) : ChatDetailIntent
    data object OnInitialMessagesLoaded : ChatDetailIntent
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
    private val chatId: ChatId,
    private val getChatByIdUseCase: GetChatByIdUseCase,
    private val getPagedMessagesUseCase: GetPagedMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val startChatUseCase: StartChatUseCase,
    private val agentService: AgentService,
    private val participantRepository: ParticipantRepository,
    private val chatRepository: ChatRepository,
    private val preferenceStorage: PreferenceStorage,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(ChatDetailState())
    val state: StateFlow<ChatDetailState> = _state.asStateFlow()

    private val _sideEffect = Channel<ChatDetailSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<ChatDetailSideEffect> = _sideEffect.receiveAsFlow()

    init {
        observeChat()
        observeTypingState()
        observeDraft()
        fetchAgent()
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

    private fun observeTypingState() {
        agentService.typingStates
            .map { it[chatId] ?: false }
            .onEach { isTyping ->
                _state.value = _state.value.copy(isAgentTyping = isTyping)
            }
            .launchIn(viewModelScope)
    }

    private fun observeDraft() {
        preferenceStorage.drafts
            .map { it[chatId] ?: "" }
            .onEach { draft ->
                _state.value = _state.value.copy(currentDraft = draft)
            }
            .launchIn(viewModelScope)
    }

    private fun fetchAgent() {
        viewModelScope.launch {
            participantRepository.getParticipantById(Constants.DEFAULT_AGENT_ID)
                .onSuccess { agent ->
                    _state.value = _state.value.copy(agent = agent)
                }
        }
    }

    private fun loadMessages() {
        _state.value = _state.value.copy(
            messages = getPagedMessagesUseCase(chatId).cachedIn(viewModelScope)
        )
    }

    fun onIntent(intent: ChatDetailIntent) {
        when (intent) {
            is ChatDetailIntent.SendMessage -> sendMessage(intent.text, intent.localMediaPath)
            is ChatDetailIntent.RenameChat -> renameChat(intent.newTitle)
            is ChatDetailIntent.UpdateDraft -> updateDraft(intent.text)
            is ChatDetailIntent.OnInitialMessagesLoaded -> scrollToBottom()
            is ChatDetailIntent.ClearError -> clearError()
        }
    }

    private fun sendMessage(text: String, localMediaPath: String?) {
        if (text.isBlank() && localMediaPath == null) return

        viewModelScope.launch {
            // Clear draft immediately on send attempt
            preferenceStorage.clearDraft(chatId)
            
            val result = if (_state.value.isNewChat) {
                startChatUseCase(
                    chatId = chatId,
                    participantIds = listOf(Constants.CURRENT_USER_ID, Constants.DEFAULT_AGENT_ID),
                    messageId = MessageId(idGenerator.generateUuid()),
                    message = text,
                    senderId = Constants.CURRENT_USER_ID,
                    localMediaPath = localMediaPath
                )
            } else {
                sendMessageUseCase(
                    id = MessageId(idGenerator.generateUuid()),
                    chatId = chatId,
                    senderId = Constants.CURRENT_USER_ID,
                    message = text,
                    localMediaPath = localMediaPath
                )
            }

            result
                .onSuccess {
                    scrollToBottom()
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error)
                }
        }
    }

    private fun renameChat(newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateChatTitle(chatId, newTitle)
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error)
                }
        }
    }

    private fun updateDraft(text: String) {
        viewModelScope.launch {
            preferenceStorage.saveDraft(chatId, text)
        }
    }

    private fun scrollToBottom() {
        viewModelScope.launch {
            _sideEffect.send(ChatDetailSideEffect.ScrollToBottom)
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
