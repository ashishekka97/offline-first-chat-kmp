package me.ashishekka.echo.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.model.Message

@OptIn(ExperimentalCoroutinesApi::class)
class SendMessageUseCaseTest {

    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var agentService: FakeAgentService
    private lateinit var sendMessageUseCase: SendMessageUseCase

    @BeforeTest
    fun setup() {
        messageRepository = FakeMessageRepository()
        agentService = FakeAgentService()
        sendMessageUseCase = SendMessageUseCase(messageRepository, agentService)
    }

    @Test
    fun testInvokeTriggersAgentForUserMessage() = runTest {
        val result = sendMessageUseCase(
            id = "m1",
            chatId = "c1",
            senderId = Constants.CURRENT_USER_ID,
            message = "Hello"
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals(1, messageRepository.sendCount)
        assertEquals(1, agentService.triggerCount)
    }

    @Test
    fun testInvokeDoesNotTriggerAgentForOtherSender() = runTest {
        val result = sendMessageUseCase(
            id = "m1",
            chatId = "c1",
            senderId = "other_user",
            message = "Hello"
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals(1, messageRepository.sendCount)
        assertEquals(0, agentService.triggerCount)
    }

    class FakeMessageRepository : MessageRepository {
        var sendCount = 0
        override fun getPagedMessagesForChat(chatId: String): Flow<PagingData<Message>> = emptyFlow()
        override suspend fun sendMessage(
            id: String,
            chatId: String,
            senderId: String,
            message: String,
            type: MessageType,
            file: FileDetails?,
            timestamp: Long
        ): Result<Unit, DatabaseError> {
            sendCount++
            return Result.Success(Unit)
        }
        override suspend fun deleteMessagesForChat(chatId: String): Result<Unit, DatabaseError> = Result.Success(Unit)
    }

    class FakeAgentService : AgentService {
        var triggerCount = 0
        override fun triggerReply(chatId: String) {
            triggerCount++
        }
    }
}
