package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.BackupError
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.PreferenceError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.ChatId
import okio.FileSystem
import okio.Path
import okio.Source
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestorationEngineTest {

    private lateinit var backupParser: FakeBackupParser
    private lateinit var mediaRestorationService: FakeMediaRestorationService
    private lateinit var seedDataRepository: FakeSeedDataRepository
    private lateinit var localAssetManager: FakeLocalAssetManager
    private lateinit var preferenceStorage: FakePreferenceStorage
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var clock: Clock
    private lateinit var engine: BackupRestorationEngine

    @BeforeTest
    fun setup() {
        backupParser = FakeBackupParser()
        mediaRestorationService = FakeMediaRestorationService()
        seedDataRepository = FakeSeedDataRepository()
        localAssetManager = FakeLocalAssetManager()
        preferenceStorage = FakePreferenceStorage()
        
        val testDispatcher = UnconfinedTestDispatcher()
        dispatcherProvider = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        
        clock = object : Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(1000)
        }
        
        engine = DefaultBackupRestorationEngine(
            backupParser,
            mediaRestorationService,
            seedDataRepository,
            localAssetManager,
            preferenceStorage,
            dispatcherProvider,
            clock
        )
    }

    @Test
    fun testSuccessfulRestore() = runTest {
        val result = engine.restore("seed_backup.zip")
        
        assertTrue(result is RestorationResult.Success)
        assertTrue(seedDataRepository.clearCalled)
        assertTrue(localAssetManager.copyCalled)
        assertTrue(backupParser.parseCalled)
        assertTrue(mediaRestorationService.processCalled)
        assertTrue(seedDataRepository.saveCalled)
        assertTrue(preferenceStorage.completed)
    }

    @Test
    fun testRestoreAlreadyCompleted() = runTest {
        preferenceStorage.setRestoreCompleted(true)
        
        val result = engine.restore("seed_backup.zip")
        
        assertTrue(result is RestorationResult.AlreadyCompleted)
        assertFalse(localAssetManager.copyCalled)
    }

    @Test
    fun testRestoreFailure() = runTest {
        localAssetManager.shouldFailCopy = true
        
        val result = engine.restore("seed_backup.zip")
        
        assertTrue(result is RestorationResult.Failure)
        assertFalse(preferenceStorage.completed)
    }

    class FakeBackupParser : BackupParser {
        var parseCalled = false
        override fun parseSeedData(fileSystem: FileSystem, jsonFileName: String): Result<SeedDataDto, BackupError> {
            parseCalled = true
            return Result.Success(SeedDataDto(emptyList(), emptyList(), emptyMap()))
        }
        override fun validateSeedData(data: SeedDataDto, fileSystem: FileSystem): Result<Unit, BackupError> = Result.Success(Unit)
    }

    class FakeMediaRestorationService : MediaRestorationService {
        var processCalled = false
        override suspend fun processMedia(messages: List<MessageEntity>, zipFileSystem: FileSystem): List<MessageEntity> {
            processCalled = true
            return messages
        }
    }

    class FakeSeedDataRepository : SeedDataRepository {
        var saveCalled = false
        var clearCalled = false
        override suspend fun saveSeedData(
            participants: List<ParticipantEntity>,
            chats: List<ChatEntity>,
            chatCrossRefs: List<ChatParticipantCrossRef>,
            messages: List<MessageEntity>
        ): Result<Unit, DatabaseError> {
            saveCalled = true
            return Result.Success(Unit)
        }

        override suspend fun clearExistingData(): Result<Unit, DatabaseError> {
            clearCalled = true
            return Result.Success(Unit)
        }
    }

    class FakeLocalAssetManager : LocalAssetManager {
        var copyCalled = false
        var shouldFailCopy = false
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
        override suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError> {
            copyCalled = true
            return if (shouldFailCopy) Result.Failure(AssetError.Unknown(Exception())) else Result.Success(Unit)
        }
        override fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError> = Result.Success(FakeFileSystem())
        override fun source(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
    }

    class FakePreferenceStorage : PreferenceStorage {
        var completed = false
        override val isRestoreCompleted: Flow<Boolean> = MutableStateFlow(completed)
        override suspend fun setRestoreCompleted(completed: Boolean): Result<Unit, PreferenceError> {
            this.completed = completed
            (isRestoreCompleted as MutableStateFlow).value = completed
            return Result.Success(Unit)
        }

        override val drafts: Flow<Map<ChatId, String>> = MutableStateFlow(emptyMap())
        override suspend fun saveDraft(chatId: ChatId, text: String): Result<Unit, PreferenceError> = Result.Success(Unit)
        override suspend fun clearDraft(chatId: ChatId): Result<Unit, PreferenceError> = Result.Success(Unit)
    }
}
