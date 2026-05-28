package me.ashishekka.echo.shared.screens.chat

import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.model.Message
import me.ashishekka.echo.shared.domain.model.Participant
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.repository.ParticipantRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.usecase.GetChatByIdUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedMessagesUseCase
import me.ashishekka.echo.shared.domain.usecase.SendMessageUseCase
import me.ashishekka.echo.shared.domain.usecase.StartChatUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModelTest {

    private val chatRepo = FakeChatRepo()
    private val messageRepo = FakeMessageRepo()
    private val agentService = FakeAgentService()
    private val participantRepo = FakeParticipantRepo()

    private val getChatByIdUseCase = GetChatByIdUseCase(chatRepo)
    private val getPagedMessagesUseCase = GetPagedMessagesUseCase(messageRepo)
    private val sendMessageUseCase = SendMessageUseCase(messageRepo, agentService)
    private val startChatUseCase = StartChatUseCase(chatRepo, agentService)

    @Test
    fun testInitialStateForExistingChat() = runTest {
        val chatId = "existing_chat"
        chatRepo.setChat(chatId, Chat(chatId, "Title", null, 0L, 0L, 0L))
        
        val viewModel = createViewModel(chatId)
        
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertNotNull(state.chat)
        assertEquals(chatId, state.chat?.id)
        assertFalse(state.isNewChat)
        assertNotNull(state.agent)
    }

    @Test
    fun testInitialStateForNewChat() = runTest {
        val chatId = "new_chat"
        // chatRepo returns null for unknown chat
        
        val viewModel = createViewModel(chatId)
        
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state.isNewChat)
        assertNull(state.chat)
        assertNotNull(state.agent)
    }

    @Test
    fun testObserveTypingState() = runTest {
        val chatId = "existing_chat"
        val viewModel = createViewModel(chatId)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAgentTyping)

        agentService.setTyping(chatId, true)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isAgentTyping)

        agentService.setTyping(chatId, false)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isAgentTyping)
    }

    @Test
    fun testSendMessageInExistingChat() = runTest {
        val chatId = "existing_chat"
        chatRepo.setChat(chatId, Chat(chatId, "Title", null, 0L, 0L, 0L))
        val viewModel = createViewModel(chatId)
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.SendMessage("Hello"))
        advanceUntilIdle()

        assertEquals("Hello", messageRepo.lastSentMessage)
        assertEquals(chatId, messageRepo.lastChatId)
        
        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ChatDetailSideEffect.ScrollToBottom)
    }

    @Test
    fun testSendMessageInNewChat() = runTest {
        val chatId = "new_chat"
        val viewModel = createViewModel(chatId)
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.SendMessage("First Message"))
        advanceUntilIdle()

        // Verify StartChatUseCase was used (via FakeChatRepo)
        assertEquals(chatId, chatRepo.lastCreatedChatId)
        assertEquals("First Message", chatRepo.lastCreatedChatMessage)
        
        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ChatDetailSideEffect.ScrollToBottom)
    }

    @Test
    fun testOnInitialMessagesLoadedTriggersScroll() = runTest {
        val chatId = "existing_chat"
        val viewModel = createViewModel(chatId)
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.OnInitialMessagesLoaded)
        
        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ChatDetailSideEffect.ScrollToBottom)
    }

    @Test
    fun testSendMessageFailureSetsError() = runTest {
        val chatId = "existing_chat"
        chatRepo.setChat(chatId, Chat(chatId, "Title", null, 0L, 0L, 0L))
        messageRepo.shouldFail = true
        val viewModel = createViewModel(chatId)
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.SendMessage("Hello"))
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.error is DatabaseError.Unknown)
    }

    private fun createViewModel(chatId: String) = ChatDetailViewModel(
        chatId = chatId,
        getChatByIdUseCase = getChatByIdUseCase,
        getPagedMessagesUseCase = getPagedMessagesUseCase,
        sendMessageUseCase = sendMessageUseCase,
        startChatUseCase = startChatUseCase,
        agentService = agentService,
        participantRepository = participantRepo
    )
}

class FakeChatRepo : ChatRepository {
    private val chats = mutableMapOf<String, Chat?>()
    var lastCreatedChatId: String? = null
    var lastCreatedChatMessage: String? = null

    fun setChat(id: String, chat: Chat?) {
        chats[id] = chat
    }

    override fun getPagedChats(): Flow<PagingData<Chat>> = flowOf(PagingData.empty())

    override fun getChatById(id: String): Flow<Chat?> = flowOf(chats[id])

    override suspend fun createChat(id: String, title: String, participantIds: List<String>): Result<Unit, DatabaseError> {
        lastCreatedChatId = id
        return Result.Success(Unit)
    }

    override suspend fun createChatWithMessage(
        chatId: String, title: String, participantIds: List<String>,
        messageId: String, message: String, senderId: String,
        type: MessageType, file: FileDetails?, timestamp: Long
    ): Result<Unit, DatabaseError> {
        lastCreatedChatId = chatId
        lastCreatedChatMessage = message
        return Result.Success(Unit)
    }

    override suspend fun updateLastMessage(chatId: String, message: String, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun deleteChat(chatId: String): Result<Unit, DatabaseError> = Result.Success(Unit)
}

class FakeMessageRepo : MessageRepository {
    var lastSentMessage: String? = null
    var lastChatId: String? = null
    var shouldFail: Boolean = false

    override fun getPagedMessagesForChat(chatId: String): Flow<PagingData<Message>> = flowOf(PagingData.empty())

    override suspend fun sendMessage(
        id: String, chatId: String, senderId: String, message: String,
        type: MessageType, file: FileDetails?, timestamp: Long
    ): Result<Unit, DatabaseError> {
        if (shouldFail) return Result.Failure(DatabaseError.Unknown(Exception("Failed")))
        lastSentMessage = message
        lastChatId = chatId
        return Result.Success(Unit)
    }

    override suspend fun deleteMessagesForChat(chatId: String): Result<Unit, DatabaseError> = Result.Success(Unit)
}

class FakeAgentService : AgentService {
    private val _typingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val typingStates: StateFlow<Map<String, Boolean>> = _typingStates.asStateFlow()

    fun setTyping(chatId: String, isTyping: Boolean) {
        _typingStates.update { it + (chatId to isTyping) }
    }

    override fun triggerReply(chatId: String) {}
}

class FakeParticipantRepo : ParticipantRepository {
    override suspend fun getParticipantById(id: String): Result<Participant, DatabaseError> {
        return Result.Success(Participant(id, "Agent", null, true))
    }
    override suspend fun getAllParticipants(): Result<List<Participant>, DatabaseError> = Result.Success(emptyList())
    override suspend fun saveParticipant(participant: Participant): Result<Unit, DatabaseError> = Result.Success(Unit)
}
