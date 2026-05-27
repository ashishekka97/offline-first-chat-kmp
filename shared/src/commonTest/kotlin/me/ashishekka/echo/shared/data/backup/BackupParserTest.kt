package me.ashishekka.echo.shared.data.backup

import me.ashishekka.echo.shared.data.file.AssetReader
import me.ashishekka.echo.shared.data.file.DefaultLocalAssetManager
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import okio.Buffer
import okio.Source
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BackupParserTest {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var assetReader: AssetReader
    private lateinit var manager: LocalAssetManager
    private lateinit var parser: BackupParser

    private val validJson = """
        {
          "participants": [
            { "id": "u1", "name": "Alice", "isAgent": false }
          ],
          "chats": [
            {
              "id": "c1",
              "title": "Topic",
              "participantIds": ["u1"],
              "lastMessage": "Hi",
              "lastMessageTimestampOffsetMs": -1000,
              "createdAtOffsetMs": -2000,
              "updatedAtOffsetMs": -1000
            }
          ],
          "messages": {
            "c1": [
              {
                "id": "m1",
                "message": "Hello",
                "type": "file",
                "sender": "u1",
                "timestampOffsetMs": -1000,
                "file": {
                  "bundledAssetName": "test.jpg",
                  "fileSize": 100
                }
              }
            ]
          }
        }
    """.trimIndent()

    @BeforeTest
    fun setup() {
        fileSystem = FakeFileSystem()
        assetReader = object : AssetReader {
            override fun readAsset(fileName: String): String? = null // Should use source
            override fun readAssetBytes(fileName: String): ByteArray? = null

            override fun readAssetSource(fileName: String): Source? {
                return when (fileName) {
                    "seed_data.json" -> Buffer().writeUtf8(validJson)
                    "test.jpg" -> Buffer().write(byteArrayOf(0))
                    else -> null
                }
            }
        }
        manager = DefaultLocalAssetManager("/data", assetReader, fileSystem)
        parser = DefaultBackupParser(manager)
    }

    @Test
    fun testParseAndValidateValidSeedData() {
        val data = parser.parseSeedData("seed_data.json")
        assertNotNull(data)
        assertTrue(parser.validateSeedData(data))
        assertEquals("u1", data.participants[0].id)
    }

    @Test
    fun testValidationFailsWithMissingParticipant() {
        val invalidData = SeedDataDto(
            participants = listOf(ParticipantDto("u1", "Alice", null, false)),
            chats = listOf(ChatDto("c1", "Topic", listOf("u1", "a1"), null, 0, 0, 0)),
            messages = emptyMap()
        )
        assertFalse(parser.validateSeedData(invalidData))
    }

    @Test
    fun testValidationFailsWithMessageKeyMismatch() {
        val invalidData = SeedDataDto(
            participants = listOf(ParticipantDto("u1", "Alice", null, false)),
            chats = listOf(ChatDto("c1", "Topic", listOf("u1"), null, 0, 0, 0)),
            messages = mapOf("unknown_chat" to listOf(MessageDto("m1", "Hi", "text", "u1", 0)))
        )
        assertFalse(parser.validateSeedData(invalidData))
    }

    @Test
    fun testValidationFailsWithMissingPhysicalAsset() {
        assetReader = object : AssetReader {
            override fun readAsset(fileName: String): String? = null
            override fun readAssetBytes(fileName: String): ByteArray? = null
            override fun readAssetSource(fileName: String): Source? {
                return if (fileName == "seed_data.json") Buffer().writeUtf8(validJson) else null
            }
        }
        manager = DefaultLocalAssetManager("/data", assetReader, fileSystem)
        parser = DefaultBackupParser(manager)

        val data = parser.parseSeedData("seed_data.json")
        assertNull(data, "Should return null if physical asset is missing")
    }
}
