package me.ashishekka.echo.shared.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.di.DispatcherProvider
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
    private val idGenerator: IdGenerator,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _state = MutableStateFlow(ChatDetailState())
    @NativeCoroutines
    val state: StateFlow<ChatDetailState> = _state.asStateFlow()

    // Local in-memory state for lag-free typing
    private val draftState = MutableStateFlow("")

    private val _sideEffect = Channel<ChatDetailSideEffect>(Channel.BUFFERED)
    @NativeCoroutines
    val sideEffect: Flow<ChatDetailSideEffect> = _sideEffect.receiveAsFlow()

    init {
        observeChat()
        observeTypingState()
        loadInitialDraft()
        syncDraftToState()
        fetchAgent()
        loadMessages()
    }

    private fun observeChat() {
        getChatByIdUseCase(chatId)
            .onEach { chat ->
                _state.update { it.copy(
                    chat = chat,
                    isNewChat = chat == null
                ) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTypingState() {
        agentService.typingStates
            .map { it[chatId] ?: false }
            .onEach { isTyping ->
                _state.update { it.copy(isAgentTyping = isTyping) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadInitialDraft() {
        viewModelScope.launch {
            val initialDraft = preferenceStorage.drafts.map { it[chatId] ?: "" }.first()
            draftState.value = initialDraft
        }
    }

    private fun syncDraftToState() {
        // Sync local typing state to UI state immediately
        draftState
            .onEach { draft ->
                _state.update { it.copy(currentDraft = draft) }
            }
            .launchIn(viewModelScope)

        // Persist draft to DataStore with 1s debounce to avoid laggy typing
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        draftState
            .debounce(1000)
            .onEach { draft ->
                preferenceStorage.saveDraft(chatId, draft)
            }
            .flowOn(dispatcherProvider.io)
            .launchIn(viewModelScope)
    }

    private fun fetchAgent() {
        viewModelScope.launch {
            participantRepository.getParticipantById(Constants.DEFAULT_AGENT_ID)
                .onSuccess { agent ->
                    _state.update { it.copy(agent = agent) }
                }
        }
    }

    private fun loadMessages() {
        _state.update { it.copy(
            messages = getPagedMessagesUseCase(chatId).cachedIn(viewModelScope)
        ) }
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
            // Clear local and persistent draft immediately on send
            draftState.value = ""
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
                    _state.update { it.copy(error = error) }
                }
        }
    }

    private fun renameChat(newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateChatTitle(chatId, newTitle)
                .onFailure { error ->
                    _state.update { it.copy(error = error) }
                }
        }
    }

    private fun updateDraft(text: String) {
        draftState.value = text
    }

    private fun scrollToBottom() {
        viewModelScope.launch {
            _sideEffect.send(ChatDetailSideEffect.ScrollToBottom)
        }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
