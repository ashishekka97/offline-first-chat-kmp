package me.ashishekka.echo.shared.data.file

import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Source
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
            override fun readAsset(fileName: String): String? {
                return if (fileName == "test.json") "{\"key\": \"value\"}" else null
            }

            override fun readAssetBytes(fileName: String): ByteArray? {
                return if (fileName == "test.bin") byteArrayOf(1, 2, 3) else null
            }

            override fun readAssetSource(fileName: String): Source? {
                return if (fileName == "test.bin") Buffer().write(byteArrayOf(1, 2, 3)) else null
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
        assertEquals(content, manager.readText(fileName))
    }

    @Test
    fun testReadNonExistentFile() {
        assertNull(manager.readText("nonexistent.txt"))
    }

    @Test
    fun testDeleteFile() {
        val fileName = "to_delete.txt"
        manager.writeText(fileName, "content")
        
        assertTrue(manager.exists(fileName))
        assertTrue(manager.deleteFile(fileName))
        assertFalse(manager.exists(fileName))
    }

    @Test
    fun testWriteAndReadBytes() {
        val fileName = "image.bin"
        val content = byteArrayOf(0, 1, 2, 3, 4, 5)
        
        manager.writeBytes(fileName, content)
        
        assertTrue(manager.exists(fileName))
        val result = manager.readBytes(fileName)
        assertTrue(content.contentEquals(result!!))
    }

    @Test
    fun testReadBundledAsset() {
        assertEquals("{\"key\": \"value\"}", manager.readBundledAsset("test.json"))
        assertNull(manager.readBundledAsset("unknown.json"))
    }

    @Test
    fun testReadBundledAssetBytes() {
        val result = manager.readBundledAssetBytes("test.bin")
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(result!!))
        assertNull(manager.readBundledAssetBytes("unknown.bin"))
    }

    @Test
    fun testBundledAssetSource() {
        val source = manager.bundledAssetSource("test.bin")
        assertNotNull(source)
        val result = source!!.buffer().readByteArray()
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(result))
        assertNull(manager.bundledAssetSource("unknown.bin"))
    }

    @Test
    fun testCopyBundledAssetToLocal() = runTest {
        val fileName = "test.bin"
        assertTrue(manager.copyBundledAssetToLocal(fileName))
        assertTrue(manager.exists(fileName))
        val result = manager.readBytes(fileName)
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(result!!))
    }

    @Test
    fun testSource() {
        val fileName = "local.bin"
        val content = byteArrayOf(4, 5, 6)
        manager.writeBytes(fileName, content)
        
        val source = manager.source(fileName)
        assertNotNull(source)
        val result = source!!.buffer().readByteArray()
        assertTrue(content.contentEquals(result))
    }

    private fun assertNotNull(actual: Any?) {
        assertTrue(actual != null, "Expected not null")
    }
}
