package me.ashishekka.echo.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.paging.PagingData

@OptIn(ExperimentalCoroutinesApi::class)
class StartChatUseCaseTest {

    private lateinit var chatRepository: FakeChatRepository
    private lateinit var agentService: FakeAgentService
    private lateinit var startChatUseCase: StartChatUseCase

    @BeforeTest
    fun setup() {
        chatRepository = FakeChatRepository()
        agentService = FakeAgentService()
        startChatUseCase = StartChatUseCase(chatRepository, agentService)
    }

    @Test
    fun testInvokeTriggersAgentForUserMessage() = runTest {
        val result = startChatUseCase(
            chatId = "c1",
            title = "New Chat",
            participantIds = listOf("p1", "p2"),
            messageId = "m1",
            message = "Hello",
            senderId = Constants.CURRENT_USER_ID
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals(1, chatRepository.createCount)
        assertEquals(1, agentService.triggerCount)
    }

    @Test
    fun testInvokeDoesNotTriggerAgentForOtherSender() = runTest {
        val result = startChatUseCase(
            chatId = "c1",
            title = "New Chat",
            participantIds = listOf("p1", "p2"),
            messageId = "m1",
            message = "Hello",
            senderId = "other_user"
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals(1, chatRepository.createCount)
        assertEquals(0, agentService.triggerCount)
    }

    class FakeChatRepository : ChatRepository {
        var createCount = 0
        override fun getPagedChats(): Flow<PagingData<Chat>> = emptyFlow()
        override fun getChatById(id: String): Flow<Chat?> = emptyFlow()
        override suspend fun createChat(id: String, title: String, participantIds: List<String>): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun createChatWithMessage(
            chatId: String,
            title: String,
            participantIds: List<String>,
            messageId: String,
            message: String,
            senderId: String,
            type: MessageType,
            file: FileDetails?,
            timestamp: Long
        ): Result<Unit, DatabaseError> {
            createCount++
            return Result.Success(Unit)
        }
        override suspend fun updateLastMessage(chatId: String, message: String, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun deleteChat(chatId: String): Result<Unit, DatabaseError> = Result.Success(Unit)
    }

    class FakeAgentService : AgentService {
        override val typingStates: StateFlow<Map<String, Boolean>> = MutableStateFlow(emptyMap())
        var triggerCount = 0
        override fun triggerReply(chatId: String) {
            triggerCount++
        }
    }
}
