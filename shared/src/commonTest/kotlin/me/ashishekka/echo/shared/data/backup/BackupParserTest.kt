package me.ashishekka.echo.shared.data.backup

import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.ParticipantId
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val result = parser.parseSeedData(fileSystem, "data.json")
        assertTrue(result is Result.Success)
        val data = result.data
        assertTrue(parser.validateSeedData(data, fileSystem) is Result.Success)
        assertEquals(ParticipantId("u1"), data.participants[0].id)
    }

    @Test
    fun testValidationFailsWithMissingParticipant() {
        val invalidData = SeedDataDto(
            participants = listOf(ParticipantDto(ParticipantId("u1"), "Alice", null, false)),
            chats = listOf(ChatDto(me.ashishekka.echo.shared.domain.model.ChatId("c1"), "Topic", listOf(ParticipantId("u1"), ParticipantId("a1")), null, 0, 0, 0)),
            messages = emptyMap()
        )
        assertTrue(parser.validateSeedData(invalidData, fileSystem) is Result.Failure)
    }

    @Test
    fun testValidationFailsWithMessageKeyMismatch() {
        val invalidData = SeedDataDto(
            participants = listOf(ParticipantDto(ParticipantId("u1"), "Alice", null, false)),
            chats = listOf(ChatDto(me.ashishekka.echo.shared.domain.model.ChatId("c1"), "Topic", listOf(ParticipantId("u1")), null, 0, 0, 0)),
            messages = mapOf(me.ashishekka.echo.shared.domain.model.ChatId("unknown_chat") to listOf(MessageDto(me.ashishekka.echo.shared.domain.model.MessageId("m1"), "Hi", "text", ParticipantId("u1"), 0)))
        )
        assertTrue(parser.validateSeedData(invalidData, fileSystem) is Result.Failure)
    }

    @Test
    fun testValidationFailsWithMissingPhysicalAsset() {
        // Delete the dummy asset to simulate missing file
        fileSystem.delete("test.jpg".toPath())

        val result = parser.parseSeedData(fileSystem, "data.json")
        assertTrue(result is Result.Failure, "Should return failure if physical asset is missing during internal validation")
    }
}
