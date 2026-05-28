package me.ashishekka.echo.shared.screens.chat

import androidx.paging.PagingData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.repository.ParticipantRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.IdGenerator
import me.ashishekka.echo.shared.domain.service.MediaService
import me.ashishekka.echo.shared.domain.usecase.GetChatByIdUseCase
import me.ashishekka.echo.shared.domain.usecase.GetPagedMessagesUseCase
import me.ashishekka.echo.shared.domain.usecase.SendMessageUseCase
import me.ashishekka.echo.shared.domain.usecase.StartChatUseCase
import okio.FileSystem
import okio.Source
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import me.ashishekka.echo.shared.util.EchoString
import me.ashishekka.echo.shared.util.StringProvider

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModelTest {

    private val chatRepo = FakeChatRepo()
    private val messageRepo = FakeMessageRepo()
    private val agentService = FakeAgentService()
    private val participantRepo = FakeParticipantRepo()
    private val idGenerator = FakeIdGenerator()
    private val mediaService = FakeMediaService()
    private val localAssetManager = FakeLocalAssetManager()
    private val preferenceStorage = FakePreferenceStorage()
    private val stringProvider = FakeStringProvider()
    
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateForExistingChat() = runTest(testDispatcher) {
        val dispatcherProvider = createDispatcherProvider(testDispatcher)
        val chatId = ChatId("existing_chat")
        chatRepo.setChat(chatId, Chat(chatId, "Title", null, 0L, 0L, 0L))
        
        val viewModel = createViewModel(chatId, dispatcherProvider)
        
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertNotNull(state.chat)
        assertEquals(chatId, state.chat?.id)
        assertFalse(state.isNewChat)
        assertNotNull(state.agent)
    }

    @Test
    fun testInitialStateForNewChat() = runTest(testDispatcher) {
        val dispatcherProvider = createDispatcherProvider(testDispatcher)
        val chatId = ChatId("new_chat")
        
        val viewModel = createViewModel(chatId, dispatcherProvider)
        
        advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state.isNewChat)
        assertNull(state.chat)
        assertNotNull(state.agent)
    }

    @Test
    fun testObserveTypingState() = runTest(testDispatcher) {
        val dispatcherProvider = createDispatcherProvider(testDispatcher)
        val chatId = ChatId("existing_chat")
        val viewModel = createViewModel(chatId, dispatcherProvider)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAgentTyping)

        agentService.setTyping(chatId, true)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isAgentTyping)

        agentService.setTyping(chatId, false)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isAgentTyping)
    }

    @Test
    fun testSendMessageInExistingChat() = runTest(testDispatcher) {
        val dispatcherProvider = createDispatcherProvider(testDispatcher)
        val chatId = ChatId("existing_chat")
        chatRepo.setChat(chatId, Chat(chatId, "Title", null, 0L, 0L, 0L))
        val viewModel = createViewModel(chatId, dispatcherProvider)
        advanceUntilIdle()

        viewModel.onIntent(ChatDetailIntent.SendMessage("Hello"))
        advanceUntilIdle()

        assertEquals("Hello", messageRepo.lastSentMessage)
        assertEquals(chatId, messageRepo.lastChatId)
        
        val effect = viewModel.sideEffect.first()
        assertTrue(effect is ChatDetailSideEffect.ScrollToBottom)
    }

    private fun createDispatcherProvider(dispatcher: CoroutineDispatcher) = object : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }

    private fun createViewModel(chatId: ChatId, dispatcherProvider: DispatcherProvider) = ChatDetailViewModel(
        chatId = chatId,
        getChatByIdUseCase = GetChatByIdUseCase(chatRepo),
        getPagedMessagesUseCase = GetPagedMessagesUseCase(messageRepo),
        sendMessageUseCase = SendMessageUseCase(messageRepo, agentService, mediaService, localAssetManager),
        startChatUseCase = StartChatUseCase(chatRepo, agentService, mediaService, localAssetManager, stringProvider),
        agentService = agentService,
        participantRepository = participantRepo,
        chatRepository = chatRepo,
        preferenceStorage = preferenceStorage,
        idGenerator = idGenerator,
        dispatcherProvider = dispatcherProvider
    )
}

class FakeStringProvider : StringProvider {
    override fun get(key: EchoString): String = key.name
    override val is24HourFormat: Boolean = true
}

class FakeChatRepo : ChatRepository {
    private val chats = mutableMapOf<ChatId, Chat?>()
    var lastCreatedChatId: ChatId? = null
    var lastCreatedChatMessage: String? = null

    fun setChat(id: ChatId, chat: Chat?) {
        chats[id] = chat
    }

    override fun getPagedChats(): Flow<PagingData<Chat>> = flowOf(PagingData.empty())

    override fun getChatById(id: ChatId): Flow<Chat?> = flowOf(chats[id])

    override suspend fun createChat(id: ChatId, title: String, participantIds: List<ParticipantId>): Result<Unit, DatabaseError> {
        lastCreatedChatId = id
        return Result.Success(Unit)
    }

    override suspend fun createChatWithMessage(
        chatId: ChatId, title: String, participantIds: List<ParticipantId>,
        messageId: MessageId, message: String, senderId: ParticipantId,
        type: MessageType, file: FileDetails?, timestamp: Long
    ): Result<Unit, DatabaseError> {
        lastCreatedChatId = chatId
        lastCreatedChatMessage = message
        return Result.Success(Unit)
    }

    override suspend fun updateLastMessage(chatId: ChatId, message: String, timestamp: Long): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun updateChatTitle(chatId: ChatId, newTitle: String): Result<Unit, DatabaseError> = Result.Success(Unit)
    override suspend fun deleteChat(chatId: ChatId): Result<Unit, DatabaseError> = Result.Success(Unit)
}

class FakeMessageRepo : MessageRepository {
    var lastSentMessage: String? = null
    var lastChatId: ChatId? = null
    var shouldFail: Boolean = false

    override fun getPagedMessagesForChat(chatId: ChatId): Flow<PagingData<Message>> = flowOf(PagingData.empty())

    override suspend fun sendMessage(
        id: MessageId, chatId: ChatId, senderId: ParticipantId, message: String,
        type: MessageType, file: FileDetails?, timestamp: Long
    ): Result<Unit, DatabaseError> {
        if (shouldFail) return Result.Failure(DatabaseError.Unknown(Exception("Failed")))
        lastSentMessage = message
        lastChatId = chatId
        return Result.Success(Unit)
    }

    override suspend fun getFilePathsForChat(chatId: ChatId): Result<List<String>, DatabaseError> = Result.Success(emptyList())

    override suspend fun deleteMessagesForChat(chatId: ChatId): Result<Unit, DatabaseError> = Result.Success(Unit)
}

class FakeAgentService : AgentService {
    private val _typingStates = MutableStateFlow<Map<ChatId, Boolean>>(emptyMap())
    override val typingStates: StateFlow<Map<ChatId, Boolean>> = _typingStates.asStateFlow()

    fun setTyping(chatId: ChatId, isTyping: Boolean) {
        _typingStates.update { it + (chatId to isTyping) }
    }

    override fun triggerReply(chatId: ChatId) {}
    override fun cancel() {}
}

class FakeParticipantRepo : ParticipantRepository {
    override suspend fun getParticipantById(id: ParticipantId): Result<Participant, DatabaseError> {
        return Result.Success(Participant(id, "Agent", null, true))
    }
    override suspend fun getAllParticipants(): Result<List<Participant>, DatabaseError> = Result.Success(emptyList())
    override suspend fun saveParticipant(participant: Participant): Result<Unit, DatabaseError> = Result.Success(Unit)
}

class FakeIdGenerator : IdGenerator {
    var nextId: String = "uuid-123"
    override fun generateUuid(): String = nextId
}

class FakePreferenceStorage : me.ashishekka.echo.shared.data.PreferenceStorage {
    private val _drafts = MutableStateFlow<Map<ChatId, String>>(emptyMap())
    override val drafts: Flow<Map<ChatId, String>> = _drafts.asStateFlow()
    override val isRestoreCompleted: Flow<Boolean> = flowOf(true)
    override suspend fun setRestoreCompleted(completed: Boolean): Result<Unit, me.ashishekka.echo.shared.domain.PreferenceError> = Result.Success(Unit)
    
    fun getLastSavedDraft(chatId: ChatId): String? = _drafts.value[chatId]

    override suspend fun saveDraft(chatId: ChatId, text: String): Result<Unit, me.ashishekka.echo.shared.domain.PreferenceError> {
        _drafts.update { it + (chatId to text) }
        return Result.Success(Unit)
    }
    override suspend fun clearDraft(chatId: ChatId): Result<Unit, me.ashishekka.echo.shared.domain.PreferenceError> {
        _drafts.update { it - chatId }
        return Result.Success(Unit)
    }
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
