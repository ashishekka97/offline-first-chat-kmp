package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetailsEntity
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageTypeEntity
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Source
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRestorationServiceTest {

    private lateinit var zipFileSystem: FakeFileSystem
    private lateinit var localAssetManager: FakeLocalAssetManager
    private lateinit var mediaProcessor: FakeMediaProcessor
    private lateinit var dispatcherProvider: DispatcherProvider
    private lateinit var service: MediaRestorationService

    @BeforeTest
    fun setup() {
        zipFileSystem = FakeFileSystem()
        localAssetManager = FakeLocalAssetManager()
        mediaProcessor = FakeMediaProcessor()
        
        val testDispatcher = UnconfinedTestDispatcher()
        dispatcherProvider = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
        }
        
        service = DefaultMediaRestorationService(localAssetManager, mediaProcessor, dispatcherProvider)
    }

    @Test
    fun testProcessMedia() = runTest {
        // Prepare ZIP content
        val bundledAssetName = "image.jpg"
        val imageBytes = byteArrayOf(1, 2, 3)
        zipFileSystem.write(bundledAssetName.toPath()) {
            write(imageBytes)
        }

        val message = MessageEntity(
            id = MessageId("m1"),
            chatId = ChatId("c1"),
            senderId = ParticipantId("u1"),
            message = "",
            type = MessageTypeEntity.FILE,
            file = FileDetailsEntity(path = bundledAssetName, fileSize = 3, thumbnail = null),
            timestamp = 0
        )

        val result = service.processMedia(listOf(message), zipFileSystem)
        
        assertEquals(1, result.size)
        val updatedMessage = result[0]
        val updatedFile = updatedMessage.file
        assertNotNull(updatedFile)
        
        // Verify local paths
        assertTrue(updatedFile.path.contains("file_m1_image.jpg"))
        assertTrue(updatedFile.thumbnail?.path?.contains("thumb_m1_image.jpg") == true)
        
        // Verify LocalAssetManager interactions
        assertTrue(localAssetManager.storedFiles.containsKey("file_m1_image.jpg"))
        assertTrue(localAssetManager.storedFiles.containsKey("thumb_m1_image.jpg"))
        
        // Verify bytes
        assertTrue(imageBytes.contentEquals(localAssetManager.storedFiles["file_m1_image.jpg"]))
        assertTrue(byteArrayOf(0).contentEquals(localAssetManager.storedFiles["thumb_m1_image.jpg"]))
    }

    class FakeLocalAssetManager : LocalAssetManager {
        val storedFiles = mutableMapOf<String, ByteArray>()
        override fun readText(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
        override fun writeText(fileName: String, content: String): Result<Unit, AssetError> {
            storedFiles[fileName] = content.encodeToByteArray()
            return Result.Success(Unit)
        }
        override fun readBytes(fileName: String): Result<ByteArray, AssetError> {
            val bytes = storedFiles[fileName]
            return if (bytes != null) Result.Success(bytes) else Result.Failure(AssetError.NotFound)
        }
        override fun readUriBytes(uriPath: String): Result<ByteArray, AssetError> = readBytes(uriPath)
        override fun writeBytes(fileName: String, bytes: ByteArray): Result<Unit, AssetError> {
            storedFiles[fileName] = bytes
            return Result.Success(Unit)
        }
        override fun deleteFile(fileName: String): Result<Unit, AssetError> {
            storedFiles.remove(fileName)
            return Result.Success(Unit)
        }
        override fun getAbsolutePath(fileName: String): String = "/local/$fileName"
        override fun exists(fileName: String): Boolean = storedFiles.containsKey(fileName)
        override fun readBundledAsset(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
        override fun readBundledAssetBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
        override fun bundledAssetSource(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
        override suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError> = Result.Failure(AssetError.NotFound)
        override fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError> = Result.Failure(AssetError.NotFound)
        override fun source(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
    }

    class FakeMediaProcessor : MediaProcessor {
        override suspend fun downsizeImage(imageData: ByteArray, maxWidth: Int, maxHeight: Int, quality: Int): Result<ByteArray, MediaError> = Result.Success(imageData)
        override suspend fun generateThumbnail(imageData: ByteArray, maxDimension: Int, quality: Int): Result<ByteArray, MediaError> = Result.Success(byteArrayOf(0))
    }
}
