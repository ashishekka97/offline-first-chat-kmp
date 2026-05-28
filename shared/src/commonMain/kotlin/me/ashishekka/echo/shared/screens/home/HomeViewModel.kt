package me.ashishekka.echo.shared.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.onFailure
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.service.IdGenerator
import me.ashishekka.echo.shared.domain.usecase.DeleteChatUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedChatsUseCase

/**
 * State for the Home screen.
 */
data class HomeState(
    val chats: Flow<PagingData<Chat>> = MutableStateFlow(PagingData.empty()),
    val isDeleting: Boolean = false,
    val pendingDeleteChatId: ChatId? = null,
    val error: AppError? = null
)

/**
 * Intents for the Home screen.
 */
sealed interface HomeIntent {
    data class ConfirmDelete(val chatId: ChatId) : HomeIntent
    data object CancelDelete : HomeIntent
    data object DeletePendingChat : HomeIntent
    data class ClickChat(val chatId: ChatId) : HomeIntent
    data object NewChat : HomeIntent
    data object ClearError : HomeIntent
}

/**
 * Side effects (one-time events) for the Home screen.
 */
sealed interface HomeSideEffect {
    data class NavigateToChat(val chatId: ChatId) : HomeSideEffect
}

/**
 * Shared ViewModel for the Home screen, managing the chat list and global actions.
 */
class HomeViewModel(
    private val getPagedChatsUseCase: GetPagedChatsUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _sideEffect = Channel<HomeSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<HomeSideEffect> = _sideEffect.receiveAsFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        _state.value = _state.value.copy(
            chats = getPagedChatsUseCase().cachedIn(viewModelScope)
        )
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.ConfirmDelete -> _state.value = _state.value.copy(pendingDeleteChatId = intent.chatId)
            is HomeIntent.CancelDelete -> _state.value = _state.value.copy(pendingDeleteChatId = null)
            is HomeIntent.DeletePendingChat -> deletePendingChat()
            is HomeIntent.ClickChat -> navigateToChat(intent.chatId)
            is HomeIntent.NewChat -> startNewChat()
            is HomeIntent.ClearError -> clearError()
        }
    }

    private fun deletePendingChat() {
        val chatId = _state.value.pendingDeleteChatId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true, pendingDeleteChatId = null)
            deleteChatUseCase(chatId)
                .onSuccess {
                    _state.value = _state.value.copy(isDeleting = false)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(error = error, isDeleting = false)
                }
        }
    }

    private fun navigateToChat(chatId: ChatId) {
        viewModelScope.launch {
            _sideEffect.send(HomeSideEffect.NavigateToChat(chatId))
        }
    }

    private fun startNewChat() {
        viewModelScope.launch {
            // Generate a random UUID for the new chat session.
            val tempId = ChatId(idGenerator.generateUuid())
            _sideEffect.send(HomeSideEffect.NavigateToChat(tempId))
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
