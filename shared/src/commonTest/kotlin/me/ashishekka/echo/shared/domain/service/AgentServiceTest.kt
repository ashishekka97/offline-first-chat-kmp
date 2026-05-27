package me.ashishekka.echo.shared.domain.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.ashishekka.echo.shared.domain.model.Message

@OptIn(ExperimentalCoroutinesApi::class)
class AgentServiceTest {

    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var testScope: TestScope
    private lateinit var clock: Clock
    private lateinit var agentService: AgentService

    @BeforeTest
    fun setup() {
        messageRepository = FakeMessageRepository()
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        dispatcherProvider = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        clock = object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(1000)
        }
        
        agentService = DefaultAgentService(
            messageRepository,
            dispatcherProvider,
            testScope,
            clock
        )
    }

    @Test
    fun testThresholdTrigger() = testScope.runTest {
        val chatId = "chat_1"
        
        // Threshold is 4 or 5. Let's send 3 messages.
        repeat(3) { agentService.triggerReply(chatId) }
        
        advanceTimeBy(3000) // Advance past potential 2s delay
        assertEquals(0, messageRepository.sentMessages.size, "Should not trigger reply before threshold")
        
        // Send 2 more messages (total 5)
        repeat(2) { agentService.triggerReply(chatId) }
        
        advanceTimeBy(3000)
        assertTrue(messageRepository.sentMessages.size >= 1, "Should trigger at least one reply after 5 messages")
        assertEquals(Constants.DEFAULT_AGENT_ID, messageRepository.sentMessages.first().senderId)
    }

    @Test
    fun testDebouncing() = testScope.runTest {
        val chatId = "chat_1"
        
        // Send 10 messages rapidly
        repeat(10) { agentService.triggerReply(chatId) }
        
        advanceTimeBy(3000)
        
        // Threshold is hit twice (at 5 and 10 messages). 
        // But debouncing should cancel the first simulation if it hasn't finished.
        // Since we are sending all 10 in a rapid burst, the second startSimulation call 
        // will cancel the Job from the first one.
        assertEquals(1, messageRepository.sentMessages.size, "Debouncing should ensure only one reply is sent for rapid bursts")
    }

    @Test
    fun testMultiChatIsolation() = testScope.runTest {
        val chatA = "chat_A"
        val chatB = "chat_B"

        // Send 3 messages in Chat A
        repeat(3) { agentService.triggerReply(chatA) }
        // Send 3 messages in Chat B
        repeat(3) { agentService.triggerReply(chatB) }

        advanceTimeBy(3000)
        assertEquals(0, messageRepository.sentMessages.size, "Counters should be isolated; 3+3 should not trigger anything")

        // Send 2 more in Chat A
        repeat(2) { agentService.triggerReply(chatA) }
        
        advanceTimeBy(3000)
        assertEquals(1, messageRepository.sentMessages.filter { it.chatId == chatA }.size, "Chat A should trigger")
        assertEquals(0, messageRepository.sentMessages.filter { it.chatId == chatB }.size, "Chat B should still be quiet")
    }

    @Test
    fun testPersistenceFailureHandling() = testScope.runTest {
        val chatId = "chat_1"
        messageRepository.shouldFail = true

        // Hit threshold
        repeat(5) { agentService.triggerReply(chatId) }

        advanceTimeBy(3000)
        
        // Verification: The app shouldn't crash. 
        // In a real scenario, we might want to log this or retry, 
        // but for now, we just ensure stability.
        assertEquals(0, messageRepository.sentMessages.size)
    }

    class FakeMessageRepository : MessageRepository {
        val sentMessages = mutableListOf<SentMessage>()
        var shouldFail = false

        data class SentMessage(
            val id: String,
            val chatId: String,
            val senderId: String,
            val message: String,
            val type: MessageType,
            val file: FileDetails?,
            val timestamp: Long
        )

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
            return if (shouldFail) {
                Result.Failure(DatabaseError.Unknown(Exception("Disk Full")))
            } else {
                sentMessages.add(SentMessage(id, chatId, senderId, message, type, file, timestamp))
                Result.Success(Unit)
            }
        }

        override suspend fun deleteMessagesForChat(chatId: String): Result<Unit, DatabaseError> = Result.Success(Unit)
    }
}
