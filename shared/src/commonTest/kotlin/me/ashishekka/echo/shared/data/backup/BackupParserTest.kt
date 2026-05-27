package me.ashishekka.echo.shared.data.backup

import okio.Path.Companion.toPath
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
        parser = DefaultBackupParser()
        
        // Write the valid JSON to the fake file system
        fileSystem.write("data.json".toPath()) {
            writeUtf8(validJson)
        }
        // Write the dummy asset to the fake file system
        fileSystem.write("test.jpg".toPath()) {
            write(byteArrayOf(0))
        }
    }

    @Test
    fun testParseAndValidateValidSeedData() {
        val data = parser.parseSeedData(fileSystem, "data.json")
        assertNotNull(data)
        assertTrue(parser.validateSeedData(data, fileSystem))
        assertEquals("u1", data.participants[0].id)
    }

    @Test
    fun testValidationFailsWithMissingParticipant() {
        val invalidData = SeedDataDto(
            participants = listOf(ParticipantDto("u1", "Alice", null, false)),
            chats = listOf(ChatDto("c1", "Topic", listOf("u1", "a1"), null, 0, 0, 0)),
            messages = emptyMap()
        )
        assertFalse(parser.validateSeedData(invalidData, fileSystem))
    }

    @Test
    fun testValidationFailsWithMessageKeyMismatch() {
        val invalidData = SeedDataDto(
            participants = listOf(ParticipantDto("u1", "Alice", null, false)),
            chats = listOf(ChatDto("c1", "Topic", listOf("u1"), null, 0, 0, 0)),
            messages = mapOf("unknown_chat" to listOf(MessageDto("m1", "Hi", "text", "u1", 0)))
        )
        assertFalse(parser.validateSeedData(invalidData, fileSystem))
    }

    @Test
    fun testValidationFailsWithMissingPhysicalAsset() {
        // Delete the dummy asset to simulate missing file
        fileSystem.delete("test.jpg".toPath())

        val data = parser.parseSeedData(fileSystem, "data.json")
        assertNull(data, "Should return null if physical asset is missing during internal validation")
    }
}
