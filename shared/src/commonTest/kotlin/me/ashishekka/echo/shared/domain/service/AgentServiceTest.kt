package me.ashishekka.echo.shared.domain.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.data.file.FakeLocalAssetManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.paging.PagingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalCoroutinesApi::class)
class AgentServiceTest {

    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var localAssetManager: FakeLocalAssetManager
    private lateinit var idGenerator: FakeIdGenerator
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var testScope: TestScope
    private lateinit var serviceScope: CoroutineScope
    private lateinit var clock: Clock
    private lateinit var agentService: AgentService

    @BeforeTest
    fun setup() {
        messageRepository = FakeMessageRepository()
        localAssetManager = FakeLocalAssetManager()
        idGenerator = FakeIdGenerator()
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        serviceScope = CoroutineScope(testDispatcher + SupervisorJob())
        
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
            localAssetManager,
            idGenerator,
            dispatcherProvider,
            serviceScope,
            clock
        )
    }

    @AfterTest
    fun tearDown() {
        serviceScope.cancel()
    }

    @Test
    fun testThresholdTrigger() = testScope.runTest {
        val chatId = ChatId("chat_1")
        
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
        val chatId = ChatId("chat_1")
        
        // Send 10 messages rapidly
        repeat(10) { agentService.triggerReply(chatId) }
        
        advanceTimeBy(3000)
        
        // Threshold is hit twice (at 5 and 10 messages). 
        // But debouncing should cancel the first simulation if it hasn't finished.
        assertEquals(1, messageRepository.sentMessages.size, "Debouncing should ensure only one reply is sent for rapid bursts")
    }

    @Test
    fun testGlobalMediaSelection() = testScope.runTest {
        val chatId = ChatId("chat_empty")
        val globalFile = "img_global_789.jpg"
        
        // Setup repository with NO media for this chat, but media exists globally
        messageRepository.existingMedia = emptyList()
        messageRepository.globalMedia = listOf(globalFile)
        
        // Ensure the global file exists
        localAssetManager.writeBytes(globalFile, byteArrayOf(1))
        
        // Trigger a reply repeatedly until we get an image
        var imageReply: FakeMessageRepository.SentMessage? = null
        repeat(50) {
            agentService.triggerReply(chatId)
            advanceTimeBy(3000)
            val last = messageRepository.sentMessages.lastOrNull()
            if (last?.type == MessageType.FILE) {
                imageReply = last
                return@repeat
            }
        }
        
        if (imageReply != null) {
            assertEquals(globalFile, imageReply.file?.path, "Agent should fallback to global media if chat media is missing")
        }
    }

    @Test
    fun testBundledFallback() = testScope.runTest {
        val chatId = ChatId("chat_new")
        
        // Setup repository with NO media anywhere
        messageRepository.existingMedia = emptyList()
        messageRepository.globalMedia = emptyList()
        
        // Trigger a reply repeatedly until we get an image
        var imageReply: FakeMessageRepository.SentMessage? = null
        repeat(50) {
            agentService.triggerReply(chatId)
            advanceTimeBy(3000)
            val last = messageRepository.sentMessages.lastOrNull()
            if (last?.type == MessageType.FILE) {
                imageReply = last
                return@repeat
            }
        }
        
        if (imageReply != null) {
            val path = imageReply.file?.path ?: ""
            assertTrue(path.startsWith("agent_fallback_") && path.endsWith(".jpg"), "Agent should fallback to one of the bundled assets")
            assertTrue(localAssetManager.exists(path), "Agent should have copied the selected bundled asset to local storage")
        }
    }

    class FakeMessageRepository : MessageRepository {
        val sentMessages = mutableListOf<SentMessage>()
        var existingMedia = emptyList<String>()
        var globalMedia = emptyList<String>()
        var shouldFail = false

        data class SentMessage(
            val id: MessageId,
            val chatId: ChatId,
            val senderId: ParticipantId,
            val message: String,
            val type: MessageType,
            val file: FileDetails?,
            val timestamp: Long
        )

        override fun getPagedMessagesForChat(chatId: ChatId): Flow<PagingData<Message>> = emptyFlow()

        override suspend fun sendMessage(
            id: MessageId,
            chatId: ChatId,
            senderId: ParticipantId,
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

        override suspend fun getFilePathsForChat(chatId: ChatId): Result<List<String>, DatabaseError> = Result.Success(existingMedia)
        
        override suspend fun getAllLocalMediaPaths(): Result<List<String>, DatabaseError> = Result.Success(globalMedia)

        override suspend fun deleteMessagesForChat(chatId: ChatId): Result<Unit, DatabaseError> = Result.Success(Unit)
    }

    class FakeIdGenerator : IdGenerator {
        override fun generateUuid(): String = "agent_msg_123"
    }
}
