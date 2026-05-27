package me.ashishekka.echo.shared.screens.home

import androidx.paging.PagingData
import androidx.paging.PagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.data.entity.MessageWithSender
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.usecase.DeleteChatUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedChatsUseCase
import okio.FileSystem
import okio.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Test
    fun testInitialStateHasChatsFlow() = runTest {
        val repository = FakeChatRepository()
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageDao(), FakeLocalAssetManager())
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase)

        val state = viewModel.state.value
        assertNotNull(state.chats)
        assertNull(state.error)
    }

    @Test
    fun testDeleteChatCallsUseCase() = runTest {
        val repository = FakeChatRepository()
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageDao(), FakeLocalAssetManager())
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase)

        viewModel.onIntent(HomeIntent.DeleteChat("chat_1"))
        
        // UseCase execution would trigger repository deletion in this setup.
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun testDeleteChatFailureSetsErrorState() = runTest {
        val repository = FakeChatRepository(shouldFail = true)
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageDao(), FakeLocalAssetManager())
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase)

        viewModel.onIntent(HomeIntent.DeleteChat("chat_1"))
        
        // Let coroutine finish
        advanceUntilIdle()

        assertEquals(DatabaseError.NotFound, viewModel.state.value.error)

        viewModel.onIntent(HomeIntent.ClearError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun testClickChatTriggersNavigationSideEffect() = runTest {
        val repository = FakeChatRepository()
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageDao(), FakeLocalAssetManager())
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase)

        viewModel.onIntent(HomeIntent.ClickChat("chat_123"))

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is HomeSideEffect.NavigateToChat)
        assertEquals("chat_123", effect.chatId)
    }

    @Test
    fun testNewChatTriggersNavigationWithUuid() = runTest {
        val repository = FakeChatRepository()
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageDao(), FakeLocalAssetManager())
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase)

        viewModel.onIntent(HomeIntent.NewChat)

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is HomeSideEffect.NavigateToChat)
        // Check if it's a UUID (length 36 is standard for 8-4-4-4-12)
        assertEquals(36, effect.chatId.length)
    }
}

class FakeChatRepository(private val shouldFail: Boolean = false) : ChatRepository {
    override fun getPagedChats(): Flow<PagingData<Chat>> {
        return kotlinx.coroutines.flow.flowOf(PagingData.empty())
    }

    override fun getChatById(id: String): Flow<Chat?> = kotlinx.coroutines.flow.flowOf(null)

    override suspend fun createChat(
        id: String,
        title: String,
        participantIds: List<String>
    ): Result<Unit, DatabaseError> = Result.Success(Unit)

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
    ): Result<Unit, DatabaseError> = Result.Success(Unit)

    override suspend fun updateLastMessage(
        chatId: String,
        message: String,
        timestamp: Long
    ): Result<Unit, DatabaseError> = Result.Success(Unit)

    override suspend fun deleteChat(chatId: String): Result<Unit, DatabaseError> {
        return if (shouldFail) Result.Failure(DatabaseError.NotFound) else Result.Success(Unit)
    }
}

class FakeMessageDao : MessageDao {
    override fun getMessagesForChat(chatId: String): PagingSource<Int, MessageWithSender> {
        throw UnsupportedOperationException()
    }
    override suspend fun getMessagesByChatId(chatId: String): List<MessageEntity> = emptyList()
    override suspend fun insertMessage(message: MessageEntity) {}
    override suspend fun updateChatLastMessage(chatId: String, message: String, timestamp: Long) {}
    override suspend fun deleteMessagesForChat(chatId: String) {}
}

class FakeLocalAssetManager : LocalAssetManager {
    override fun readText(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
    override fun writeText(fileName: String, content: String): Result<Unit, AssetError> = Result.Success(Unit)
    override fun readBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
    override fun writeBytes(fileName: String, bytes: ByteArray): Result<Unit, AssetError> = Result.Success(Unit)
    override fun deleteFile(fileName: String): Result<Unit, AssetError> = Result.Success(Unit)
    override fun getAbsolutePath(fileName: String): String = ""
    override fun exists(fileName: String): Boolean = false
    override fun readBundledAsset(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
    override fun readBundledAssetBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
    override fun bundledAssetSource(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
    override suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError> = Result.Success(Unit)
    override fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError> = Result.Failure(AssetError.NotFound)
    override fun source(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
}
