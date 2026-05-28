package me.ashishekka.echo.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.MediaService
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.MediaError
import okio.Source
import okio.FileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class SendMessageUseCaseTest {

    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var agentService: FakeAgentService
    private lateinit var mediaService: FakeMediaService
    private lateinit var localAssetManager: FakeLocalAssetManager
    private lateinit var sendMessageUseCase: SendMessageUseCase

    @BeforeTest
    fun setup() {
        messageRepository = FakeMessageRepository()
        agentService = FakeAgentService()
        mediaService = FakeMediaService()
        localAssetManager = FakeLocalAssetManager()
        sendMessageUseCase = SendMessageUseCase(messageRepository, agentService, mediaService, localAssetManager)
    }

    @Test
    fun testInvokeTriggersAgentForUserMessage() = runTest {
        val result = sendMessageUseCase(
            id = MessageId("m1"),
            chatId = ChatId("c1"),
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
            id = MessageId("m1"),
            chatId = ChatId("c1"),
            senderId = ParticipantId("other_user"),
            message = "Hello"
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals(1, messageRepository.sendCount)
        assertEquals(0, agentService.triggerCount)
    }

    class FakeMessageRepository : MessageRepository {
        var sendCount = 0
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
            sendCount++
            return Result.Success(Unit)
        }

        override suspend fun getFilePathsForChat(chatId: ChatId): Result<List<String>, DatabaseError> = Result.Success(emptyList())
        override suspend fun deleteMessagesForChat(chatId: ChatId): Result<Unit, DatabaseError> = Result.Success(Unit)
    }

    class FakeAgentService : AgentService {
        override val typingStates: StateFlow<Map<ChatId, Boolean>> = MutableStateFlow(emptyMap())
        var triggerCount = 0
        override fun triggerReply(chatId: ChatId) {
            triggerCount++
        }
        override fun cancel() {}
    }

    class FakeMediaService : MediaService {
        override suspend fun processImage(bytes: ByteArray, originalPath: String): Result<FileDetails, MediaError> {
            return Result.Success(FileDetails(originalPath, bytes.size.toLong(), null))
        }
    }

    class FakeLocalAssetManager : LocalAssetManager {
        override fun readText(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
        override fun writeText(fileName: String, content: String): Result<Unit, AssetError> = Result.Success(Unit)
        override fun readBytes(fileName: String): Result<ByteArray, AssetError> = Result.Success(ByteArray(0))
        override fun writeBytes(fileName: String, bytes: ByteArray): Result<Unit, AssetError> = Result.Success(Unit)
        override fun deleteFile(fileName: String): Result<Unit, AssetError> = Result.Success(Unit)
        override fun getAbsolutePath(fileName: String): String = fileName
        override fun exists(fileName: String): Boolean = true
        override fun readBundledAsset(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
        override fun readBundledAssetBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
        override fun bundledAssetSource(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
        override suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError> = Result.Success(Unit)
        override fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError> = Result.Failure(AssetError.NotFound)
        override fun source(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
    }
}
