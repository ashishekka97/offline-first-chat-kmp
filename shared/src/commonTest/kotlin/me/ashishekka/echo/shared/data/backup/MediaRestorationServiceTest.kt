package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetailsEntity
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageTypeEntity
import me.ashishekka.echo.shared.data.file.FakeLocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import okio.FileSystem
import okio.Path.Companion.toPath
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
        
        // Verify local paths are relative and hashed
        assertTrue(updatedFile.path.startsWith("img_"))
        assertTrue(updatedFile.path.endsWith(".jpg"))
        assertTrue(updatedFile.thumbnail?.path?.startsWith("thumb_img_") == true)
        
        // Verify LocalAssetManager interactions
        val fileName = updatedFile.path
        val thumbName = updatedFile.thumbnail!!.path
        assertTrue(localAssetManager.storedFiles.containsKey(fileName))
        assertTrue(localAssetManager.storedFiles.containsKey(thumbName))
        
        // Verify bytes
        assertTrue(imageBytes.contentEquals(localAssetManager.storedFiles[fileName]))
        assertTrue(byteArrayOf(0).contentEquals(localAssetManager.storedFiles[thumbName]))
    }

    class FakeMediaProcessor : MediaProcessor {
        override suspend fun downsizeImage(imageData: ByteArray, maxWidth: Int, maxHeight: Int, quality: Int): Result<ByteArray, MediaError> = Result.Success(imageData)
        override suspend fun generateThumbnail(imageData: ByteArray, maxDimension: Int, quality: Int): Result<ByteArray, MediaError> = Result.Success(byteArrayOf(0))
    }
}
