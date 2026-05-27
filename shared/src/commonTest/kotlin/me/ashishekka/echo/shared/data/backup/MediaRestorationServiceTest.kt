package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.di.DispatcherProvider
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
            id = "m1",
            chatId = "c1",
            senderId = "u1",
            message = "",
            type = MessageType.FILE,
            file = FileDetails(path = bundledAssetName, fileSize = 3, thumbnail = null),
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
        override fun readText(fileName: String): String? = null
        override fun writeText(fileName: String, content: String) {}
        override fun readBytes(fileName: String): ByteArray? = storedFiles[fileName]
        override fun writeBytes(fileName: String, bytes: ByteArray) { storedFiles[fileName] = bytes }
        override fun deleteFile(fileName: String): Boolean = false
        override fun getAbsolutePath(fileName: String): String = "/local/$fileName"
        override fun exists(fileName: String): Boolean = storedFiles.containsKey(fileName)
        override fun readBundledAsset(fileName: String): String? = null
        override fun readBundledAssetBytes(fileName: String): ByteArray? = null
        override fun bundledAssetSource(fileName: String): Source? = null
        override suspend fun copyBundledAssetToLocal(fileName: String): Boolean = false
        override fun getZipFileSystem(fileName: String): FileSystem? = null
        override fun source(fileName: String): Source? = null
    }

    class FakeMediaProcessor : MediaProcessor {
        override suspend fun downsizeImage(imageData: ByteArray, maxWidth: Int, maxHeight: Int, quality: Int): ByteArray? = imageData
        override suspend fun generateThumbnail(imageData: ByteArray, maxDimension: Int, quality: Int): ByteArray? = byteArrayOf(0)
    }
}
