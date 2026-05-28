package me.ashishekka.echo.shared.screens.home

import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.backup.BackupRestorationEngine
import me.ashishekka.echo.shared.data.backup.RestorationResult
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.IdGenerator
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

    private val idGenerator = FakeIdGenerator()
    private val agentService = FakeAgentService()
    private val restorationEngine = FakeRestorationEngine()
    private val preferenceStorage = FakePreferenceStorage()

    @Test
    fun testInitialStateHasChatsFlow() = runTest {
        val repository = FakeChatRepository()
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageRepository(), FakeLocalAssetManager(), agentService)
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase, restorationEngine, preferenceStorage, idGenerator)

        val state = viewModel.state.value
        assertNotNull(state.chats)
        assertNull(state.error)
    }

    @Test
    fun testDeleteChatCallsUseCase() = runTest {
        val repository = FakeChatRepository()
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageRepository(), FakeLocalAssetManager(), agentService)
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase, restorationEngine, preferenceStorage, idGenerator)

        val chatId = ChatId("chat_1")
        viewModel.onIntent(HomeIntent.ConfirmDelete(chatId))
        assertEquals(chatId, viewModel.state.value.pendingDeleteChatId)
        
        viewModel.onIntent(HomeIntent.DeletePendingChat)
        advanceUntilIdle()

        assertNull(viewModel.state.value.pendingDeleteChatId)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun testDeleteChatFailureSetsErrorState() = runTest {
        val repository = FakeChatRepository(shouldFail = true)
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageRepository(), FakeLocalAssetManager(), agentService)
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase, restorationEngine, preferenceStorage, idGenerator)

        viewModel.onIntent(HomeIntent.ConfirmDelete(ChatId("chat_1")))
        viewModel.onIntent(HomeIntent.DeletePendingChat)
        
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
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageRepository(), FakeLocalAssetManager(), agentService)
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase, restorationEngine, preferenceStorage, idGenerator)

        viewModel.onIntent(HomeIntent.ClickChat(ChatId("chat_123")))

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is HomeSideEffect.NavigateToChat)
        assertEquals(ChatId("chat_123"), effect.chatId)
    }

    @Test
    fun testNewChatTriggersNavigationWithUuid() = runTest {
        val repository = FakeChatRepository()
        val getPagedChatsUseCase = GetPagedChatsUseCase(repository)
        val deleteChatUseCase = DeleteChatUseCase(repository, FakeMessageRepository(), FakeLocalAssetManager(), agentService)
        val viewModel = HomeViewModel(getPagedChatsUseCase, deleteChatUseCase, restorationEngine, preferenceStorage, idGenerator)

        idGenerator.nextId = "fixed-uuid"
        viewModel.onIntent(HomeIntent.NewChat)

        val effect = viewModel.sideEffect.first()
        assertTrue(effect is HomeSideEffect.NavigateToChat)
        assertEquals(ChatId("fixed-uuid"), effect.chatId)
    }
}

class FakeChatRepository(private val shouldFail: Boolean = false) : ChatRepository {
    override fun getPagedChats(): Flow<PagingData<Chat>> = flowOf(PagingData.empty())
    override fun getChatById(id: ChatId): Flow<Chat?> = flowOf(null)
    override suspend fun createChat(id: ChatId, title: String, participantIds: List<ParticipantId>): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun createChatWithMessage(chatId: ChatId, title: String, participantIds: List<ParticipantId>, messageId: MessageId, message: String, senderId: ParticipantId, type: MessageType, file: FileDetails?, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun updateLastMessage(chatId: ChatId, message: String, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun updateChatTitle(chatId: ChatId, newTitle: String): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun deleteChat(chatId: ChatId): Result<Unit, DatabaseError> {
        return if (shouldFail) Result.Failure(DatabaseError.NotFound) else Result.Success(Unit)
    }
}

class FakeMessageRepository : MessageRepository {
    override fun getPagedMessagesForChat(chatId: ChatId): Flow<PagingData<Message>> = flowOf(PagingData.empty())
    override suspend fun sendMessage(id: MessageId, chatId: ChatId, senderId: ParticipantId, message: String, type: MessageType, file: FileDetails?, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun getFilePathsForChat(chatId: ChatId): Result<List<String>, DatabaseError> = Result.Success(emptyList())
    override suspend fun getAllLocalMediaPaths(): Result<List<String>, DatabaseError> = Result.Success(emptyList())
    override suspend fun deleteMessagesForChat(chatId: ChatId): Result<Unit, DatabaseError> = Result.Success(Unit)
}

class FakeLocalAssetManager : LocalAssetManager {
    override fun readText(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
    override fun writeText(fileName: String, content: String): Result<Unit, AssetError> = Result.Success(Unit)
    override fun readBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
    override fun readUriBytes(uriPath: String): Result<ByteArray, AssetError> = readBytes(uriPath)
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

class FakeIdGenerator : IdGenerator {
    var nextId: String = "default-id"
    override fun generateUuid(): String = nextId
}

class FakeAgentService : AgentService {
    override val typingStates: StateFlow<Map<ChatId, Boolean>> = MutableStateFlow(emptyMap())
    override fun triggerReply(chatId: ChatId) {}
    override fun cancel() {}
}

class FakeRestorationEngine : BackupRestorationEngine {
    override suspend fun restore(zipFileName: String): RestorationResult = RestorationResult.Success
}

class FakePreferenceStorage : me.ashishekka.echo.shared.data.PreferenceStorage {
    override val drafts: Flow<Map<ChatId, String>> = flowOf(emptyMap())
    override val isRestoreCompleted: Flow<Boolean> = flowOf(true)
    override suspend fun setRestoreCompleted(completed: Boolean): Result<Unit, me.ashishekka.echo.shared.domain.PreferenceError> = Result.Success(Unit)
    override suspend fun saveDraft(chatId: ChatId, text: String): Result<Unit, me.ashishekka.echo.shared.domain.PreferenceError> = Result.Success(Unit)
    override suspend fun clearDraft(chatId: ChatId): Result<Unit, me.ashishekka.echo.shared.domain.PreferenceError> = Result.Success(Unit)
}
