package me.ashishekka.echo.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.domain.AssetError
import okio.Source
import okio.FileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import me.ashishekka.echo.shared.domain.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteChatUseCaseTest {

    private lateinit var chatRepository: FakeChatRepository
    private lateinit var messageRepository: FakeMessageRepository
    private lateinit var localAssetManager: FakeLocalAssetManager
    private lateinit var deleteChatUseCase: DeleteChatUseCase

    @BeforeTest
    fun setup() {
        chatRepository = FakeChatRepository()
        messageRepository = FakeMessageRepository()
        localAssetManager = FakeLocalAssetManager()
        deleteChatUseCase = DeleteChatUseCase(chatRepository, messageRepository, localAssetManager)
    }

    @Test
    fun testDeleteChatCleansUpFilesAndDatabase() = runTest {
        val chatId = ChatId("c1")
        
        // Prepare mock file paths
        messageRepository.filePaths = listOf("local_file.jpg")

        val result = deleteChatUseCase(chatId)

        assertEquals(Result.Success(Unit), result)
        assertTrue(chatRepository.deleteCalled)
        assertEquals(chatId, chatRepository.deletedChatId)
        
        // Verify physical file deletion was attempted for local_file.jpg
        assertTrue(localAssetManager.deletedFiles.contains("local_file.jpg"))
    }

    class FakeChatRepository : ChatRepository {
        var deleteCalled = false
        var deletedChatId: ChatId? = null
        
        override fun getPagedChats(): Flow<PagingData<Chat>> = emptyFlow()
        override fun getChatById(id: ChatId): Flow<Chat?> = emptyFlow()
        override suspend fun createChat(id: ChatId, title: String, participantIds: List<ParticipantId>): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun createChatWithMessage(chatId: ChatId, title: String, participantIds: List<ParticipantId>, messageId: MessageId, message: String, senderId: ParticipantId, type: MessageType, file: FileDetails?, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun updateLastMessage(chatId: ChatId, message: String, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun deleteChat(chatId: ChatId): Result<Unit, DatabaseError> {
            deleteCalled = true
            deletedChatId = chatId
            return Result.Success(Unit)
        }
    }

    class FakeMessageRepository : MessageRepository {
        var filePaths = emptyList<String>()
        override fun getPagedMessagesForChat(chatId: ChatId): Flow<PagingData<Message>> = emptyFlow()
        override suspend fun sendMessage(id: MessageId, chatId: ChatId, senderId: ParticipantId, message: String, type: MessageType, file: FileDetails?, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun getFilePathsForChat(chatId: ChatId): Result<List<String>, DatabaseError> = Result.Success(filePaths)
        override suspend fun deleteMessagesForChat(chatId: ChatId): Result<Unit, DatabaseError> = Result.Success(Unit)
    }

    class FakeLocalAssetManager : LocalAssetManager {
        val deletedFiles = mutableListOf<String>()
        override fun readText(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
        override fun writeText(fileName: String, content: String): Result<Unit, AssetError> = Result.Success(Unit)
        override fun readBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
        override fun writeBytes(fileName: String, bytes: ByteArray): Result<Unit, AssetError> = Result.Success(Unit)
        override fun deleteFile(fileName: String): Result<Unit, AssetError> {
            deletedFiles.add(fileName)
            return Result.Success(Unit)
        }
        override fun getAbsolutePath(fileName: String): String = ""
        override fun exists(fileName: String): Boolean = false
        override fun readBundledAsset(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
        override fun readBundledAssetBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
        override fun bundledAssetSource(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
        override suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError> = Result.Failure(AssetError.NotFound)
        override fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError> = Result.Failure(AssetError.NotFound)
        override fun source(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
    }
}
