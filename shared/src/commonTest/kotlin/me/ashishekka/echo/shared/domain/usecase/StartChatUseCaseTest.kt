package me.ashishekka.echo.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.repository.ChatRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class StartChatUseCaseTest {

    private lateinit var chatRepository: FakeChatRepository
    private lateinit var agentService: FakeAgentService
    private lateinit var mediaService: FakeMediaService
    private lateinit var localAssetManager: FakeLocalAssetManager
    private lateinit var startChatUseCase: StartChatUseCase

    @BeforeTest
    fun setup() {
        chatRepository = FakeChatRepository()
        agentService = FakeAgentService()
        mediaService = FakeMediaService()
        localAssetManager = FakeLocalAssetManager()
        startChatUseCase = StartChatUseCase(chatRepository, agentService, mediaService, localAssetManager)
    }

    @Test
    fun testInvokeTriggersAgentForUserMessage() = runTest {
        val result = startChatUseCase(
            chatId = ChatId("c1"),
            participantIds = listOf(ParticipantId("p1"), ParticipantId("p2")),
            messageId = MessageId("m1"),
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
            chatId = ChatId("c1"),
            participantIds = listOf(ParticipantId("p1"), ParticipantId("p2")),
            messageId = MessageId("m1"),
            message = "Hello",
            senderId = ParticipantId("other_user")
        )

        assertEquals(Result.Success(Unit), result)
        assertEquals(1, chatRepository.createCount)
        assertEquals(0, agentService.triggerCount)
    }

    class FakeChatRepository : ChatRepository {
        var createCount = 0
        override fun getPagedChats(): Flow<PagingData<Chat>> = emptyFlow()
        override fun getChatById(id: ChatId): Flow<Chat?> = emptyFlow()
        override suspend fun createChat(id: ChatId, title: String, participantIds: List<ParticipantId>): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun createChatWithMessage(
            chatId: ChatId,
            title: String,
            participantIds: List<ParticipantId>,
            messageId: MessageId,
            message: String,
            senderId: ParticipantId,
            type: MessageType,
            file: FileDetails?,
            timestamp: Long
        ): Result<Unit, DatabaseError> {
            createCount++
            return Result.Success(Unit)
        }
        override suspend fun updateLastMessage(chatId: ChatId, message: String, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun updateChatTitle(chatId: ChatId, newTitle: String): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun deleteChat(chatId: ChatId): Result<Unit, DatabaseError> = Result.Success(Unit)
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
            return Result.Success(FileDetails(path = originalPath, fileSize = bytes.size.toLong(), thumbnail = null))
        }
    }

    class FakeLocalAssetManager : LocalAssetManager {
        override fun readText(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
        override fun writeText(fileName: String, content: String): Result<Unit, AssetError> = Result.Success(Unit)
        override fun readBytes(fileName: String): Result<ByteArray, AssetError> = Result.Success(ByteArray(0))
        override fun readUriBytes(uriPath: String): Result<ByteArray, AssetError> = readBytes(uriPath)
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
