package me.ashishekka.echo.shared.data.file

import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.Result
import okio.Buffer
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalAssetManagerTest {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var assetReader: AssetReader
    private lateinit var manager: LocalAssetManager
    private val baseDirPath = "/data"

    @BeforeTest
    fun setup() {
        fileSystem = FakeFileSystem()
        assetReader = object : AssetReader {
            override fun readAsset(fileName: String): Result<String, AssetError> {
                return if (fileName == "test.json") Result.Success("{\"key\": \"value\"}") else Result.Failure(AssetError.NotFound)
            }

            override fun readAssetBytes(fileName: String): Result<ByteArray, AssetError> {
                return if (fileName == "test.bin") Result.Success(byteArrayOf(1, 2, 3)) else Result.Failure(AssetError.NotFound)
            }

            override fun readAssetSource(fileName: String): Result<Source, AssetError> {
                return if (fileName == "test.bin") Result.Success(Buffer().write(byteArrayOf(1, 2, 3))) else Result.Failure(AssetError.NotFound)
            }
        }
        manager = DefaultLocalAssetManager(baseDirPath, assetReader, fileSystem)
    }

    @Test
    fun testWriteAndReadText() {
        val fileName = "test.txt"
        val content = "Hello, World!"
        
        manager.writeText(fileName, content)
        
        assertTrue(manager.exists(fileName))
        assertEquals(content, manager.readText(fileName).getOrNull())
    }

    @Test
    fun testReadNonExistentFile() {
        assertTrue(manager.readText("nonexistent.txt") is Result.Failure)
    }

    @Test
    fun testDeleteFile() {
        val fileName = "to_delete.txt"
        manager.writeText(fileName, "content")
        
        assertTrue(manager.exists(fileName))
        assertTrue(manager.deleteFile(fileName) is Result.Success)
        assertFalse(manager.exists(fileName))
    }

    @Test
    fun testWriteAndReadBytes() {
        val fileName = "image.bin"
        val content = byteArrayOf(0, 1, 2, 3, 4, 5)
        
        manager.writeBytes(fileName, content)
        
        assertTrue(manager.exists(fileName))
        val result = manager.readBytes(fileName).getOrNull()
        assertTrue(content.contentEquals(result!!))
    }

    @Test
    fun testReadBundledAsset() {
        assertEquals("{\"key\": \"value\"}", manager.readBundledAsset("test.json").getOrNull())
        assertTrue(manager.readBundledAsset("unknown.json") is Result.Failure)
    }

    @Test
    fun testReadBundledAssetBytes() {
        val result = manager.readBundledAssetBytes("test.bin").getOrNull()
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(result!!))
        assertTrue(manager.readBundledAssetBytes("unknown.bin") is Result.Failure)
    }

    @Test
    fun testBundledAssetSource() {
        val result = manager.bundledAssetSource("test.bin")
        assertTrue(result is Result.Success)
        val bytes = result.data.buffer().readByteArray()
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(bytes))
        assertTrue(manager.bundledAssetSource("unknown.bin") is Result.Failure)
    }

    @Test
    fun testCopyBundledAssetToLocal() = runTest {
        val fileName = "test.bin"
        assertTrue(manager.copyBundledAssetToLocal(fileName) is Result.Success)
        assertTrue(manager.exists(fileName))
        val result = manager.readBytes(fileName).getOrNull()
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(result!!))
    }

    @Test
    fun testSource() {
        val fileName = "local.bin"
        val content = byteArrayOf(4, 5, 6)
        manager.writeBytes(fileName, content)
        
        val result = manager.source(fileName)
        assertTrue(result is Result.Success)
        val bytes = result.data.buffer().readByteArray()
        assertTrue(content.contentEquals(bytes))
    }

    @Test
    fun testGetAbsolutePathWithUrls() {
        val httpUrl = "http://example.com/image.jpg"
        val httpsUrl = "https://example.com/image.jpg"
        val contentUri = "content://media/external/images/media/1"
        val fileUri = "file:///storage/emulated/0/image.jpg"
        val localFile = "my_image.jpg"

        assertEquals(httpUrl, manager.getAbsolutePath(httpUrl))
        assertEquals(httpsUrl, manager.getAbsolutePath(httpsUrl))
        assertEquals(contentUri, manager.getAbsolutePath(contentUri))
        assertEquals(fileUri, manager.getAbsolutePath(fileUri))
        assertEquals("$baseDirPath/my_image.jpg", manager.getAbsolutePath(localFile))
    }

    private fun <T, E : me.ashishekka.echo.shared.domain.AppError> assertSuccess(result: Result<T, E>) {
        assertTrue(result is Result.Success, "Expected Success but was $result")
    }
}
