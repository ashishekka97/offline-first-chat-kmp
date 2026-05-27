package me.ashishekka.echo.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.data.dao.MessageDao
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
import me.ashishekka.echo.shared.domain.model.Chat

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteChatUseCaseTest {

    private lateinit var chatRepository: FakeChatRepository
    private lateinit var messageDao: FakeMessageDao
    private lateinit var localAssetManager: FakeLocalAssetManager
    private lateinit var deleteChatUseCase: DeleteChatUseCase

    @BeforeTest
    fun setup() {
        chatRepository = FakeChatRepository()
        messageDao = FakeMessageDao()
        localAssetManager = FakeLocalAssetManager()
        deleteChatUseCase = DeleteChatUseCase(chatRepository, messageDao, localAssetManager)
    }

    @Test
    fun testDeleteChatCleansUpFilesAndDatabase() = runTest {
        val chatId = "c1"
        
        // Prepare mock messages with local files
        messageDao.messages = listOf(
            MessageEntity("m1", chatId, "u1", "Hi", MessageType.TEXT, null, 0),
            MessageEntity("m2", chatId, "u1", "", MessageType.FILE, FileDetails("local_file.jpg", 100, null), 0)
        )

        val result = deleteChatUseCase(chatId)

        assertEquals(Result.Success(Unit), result)
        assertTrue(chatRepository.deleteCalled)
        assertEquals(chatId, chatRepository.deletedChatId)
        
        // Verify physical file deletion was attempted for local_file.jpg
        assertTrue(localAssetManager.deletedFiles.contains("local_file.jpg"))
    }

    @Test
    fun testDeleteChatSkipsNetworkUrls() = runTest {
        val chatId = "c1"
        messageDao.messages = listOf(
            MessageEntity("m1", chatId, "u1", "", MessageType.FILE, FileDetails("https://example.com/image.jpg", 100, null), 0)
        )

        deleteChatUseCase(chatId)

        assertEquals(0, localAssetManager.deletedFiles.size, "Should not attempt to delete HTTP URLs from local storage")
    }

    class FakeChatRepository : ChatRepository {
        var deleteCalled = false
        var deletedChatId: String? = null
        
        override fun getPagedChats(): Flow<PagingData<Chat>> = emptyFlow()
        override fun getChatById(id: String): Flow<Chat?> = emptyFlow()
        override suspend fun createChat(id: String, title: String, participantIds: List<String>): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun createChatWithMessage(chatId: String, title: String, participantIds: List<String>, messageId: String, message: String, senderId: String, type: MessageType, file: FileDetails?, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun updateLastMessage(chatId: String, message: String, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
        override suspend fun deleteChat(chatId: String): Result<Unit, DatabaseError> {
            deleteCalled = true
            deletedChatId = chatId
            return Result.Success(Unit)
        }
    }

    class FakeMessageDao : MessageDao {
        var messages = emptyList<MessageEntity>()
        override fun getMessagesForChat(chatId: String): androidx.paging.PagingSource<Int, me.ashishekka.echo.shared.data.entity.MessageWithSender> = throw UnsupportedOperationException()
        override suspend fun getMessagesByChatId(chatId: String): List<MessageEntity> = messages
        override suspend fun insertMessage(message: MessageEntity) {}
        override suspend fun updateChatLastMessage(chatId: String, message: String, timestamp: Long) {}
        override suspend fun insertMessageAndUpdateChat(message: MessageEntity) {}
        override suspend fun deleteMessagesForChat(chatId: String) {}
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
