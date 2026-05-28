package me.ashishekka.echo.shared.data.backup

import me.ashishekka.echo.shared.data.entity.MessageTypeEntity
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackupMappersTest {

    private val baseTime = 1000000L

    @Test
    fun testParticipantDtoToEntity() {
        val dto = ParticipantDto(ParticipantId("user-1"), "Alice", "url", false)
        val entity = dto.toEntity()
        assertEquals(dto.id, entity.id)
        assertEquals(dto.name, entity.name)
        assertEquals(dto.isAgent, entity.isAgent)
    }

    @Test
    fun testChatDtoToEntityWithRelativeTime() {
        val chatId = ChatId("chat-1")
        val dto = ChatDto(
            id = chatId,
            title = "Topic",
            participantIds = listOf(ParticipantId("u1")),
            lastMessage = "Hey",
            lastMessageTimestampOffsetMs = -1000L,
            createdAtOffsetMs = -5000L,
            updatedAtOffsetMs = -1000L
        )
        val entity = dto.toEntity(baseTime)
        
        assertEquals(baseTime - 1000L, entity.lastMessageTimestamp)
        assertEquals(baseTime - 5000L, entity.createdAt)
    }

    @Test
    fun testMessageDtoToEntityWithComplexFile() {
        val messageId = MessageId("m1")
        val dto = MessageDto(
            id = messageId,
            message = "Photo",
            type = "FILE",
            sender = ParticipantId("u1"),
            timestampOffsetMs = 0,
            file = FileDto(
                path = "url",
                bundledAssetName = "local.jpg",
                fileSize = 100,
                thumbnail = ThumbnailDto(bundledAssetName = "thumb.jpg")
            )
        )
        val entity = dto.toEntity(ChatId("c1"), baseTime)
        
        assertEquals(MessageTypeEntity.FILE, entity.type)
        assertNotNull(entity.file)
        assertEquals("local.jpg", entity.file.path)
        assertNotNull(entity.file.thumbnail)
        assertEquals("thumb.jpg", entity.file.thumbnail.path)
    }

    @Test
    fun testChatDtoToCrossRefs() {
        val chatId = ChatId("c1")
        val p1Id = ParticipantId("p1")
        val p2Id = ParticipantId("p2")
        val dto = ChatDto(
            id = chatId,
            title = "T",
            participantIds = listOf(p1Id, p2Id),
            lastMessage = null,
            lastMessageTimestampOffsetMs = 0,
            createdAtOffsetMs = 0,
            updatedAtOffsetMs = 0
        )
        val refs = dto.toCrossRefs()
        assertEquals(2, refs.size)
        assertEquals(p1Id, refs[0].participantId)
        assertEquals(p2Id, refs[1].participantId)
    }

    @Test
    fun testFileDtoPrefersBundledAsset() {
        val dto = FileDto(
            path = "http://remote.com/image.jpg",
            bundledAssetName = "local_image.jpg",
            fileSize = 100L
        )
        val entity = dto.toEntity()
        assertEquals("local_image.jpg", entity.path)
    }
}
