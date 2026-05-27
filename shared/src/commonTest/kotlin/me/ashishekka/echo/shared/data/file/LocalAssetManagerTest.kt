package me.ashishekka.echo.shared.data.file

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
}
