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
import okio.FileSystem
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
        override fun parseSeedData(fileSystem: FileSystem, jsonFileName: String): SeedDataDto? {
            parseCalled = true
            return SeedDataDto(emptyList(), emptyList(), emptyMap())
        }
        override fun validateSeedData(data: SeedDataDto, fileSystem: FileSystem): Boolean = true
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
        ) {
            saveCalled = true
        }

        override suspend fun clearExistingData() {
            clearCalled = true
        }
    }

    class FakeLocalAssetManager : LocalAssetManager {
        var copyCalled = false
        var shouldFailCopy = false
        override fun readText(fileName: String): String? = null
        override fun writeText(fileName: String, content: String) {}
        override fun readBytes(fileName: String): ByteArray? = null
        override fun writeBytes(fileName: String, bytes: ByteArray) {}
        override fun deleteFile(fileName: String): Boolean = false
        override fun getAbsolutePath(fileName: String): String = ""
        override fun exists(fileName: String): Boolean = false
        override fun readBundledAsset(fileName: String): String? = null
        override fun readBundledAssetBytes(fileName: String): ByteArray? = null
        override fun bundledAssetSource(fileName: String): Source? = null
        override suspend fun copyBundledAssetToLocal(fileName: String): Boolean {
            copyCalled = true
            return !shouldFailCopy
        }
        override fun getZipFileSystem(fileName: String): FileSystem? = FakeFileSystem()
        override fun source(fileName: String): Source? = null
    }

    class FakePreferenceStorage : PreferenceStorage {
        var completed = false
        override val isRestoreCompleted: Flow<Boolean> = MutableStateFlow(completed)
        override suspend fun setRestoreCompleted(completed: Boolean) {
            this.completed = completed
            (isRestoreCompleted as MutableStateFlow).value = completed
        }
    }
}
