package me.ashishekka.echo.shared.data.mapper

import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatWithParticipants
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import me.ashishekka.echo.shared.util.StringProvider
import me.ashishekka.echo.shared.util.EchoString
import kotlin.test.Test
import kotlin.test.assertEquals

class EntityMappersTest {

    private val mockStringProvider = object : StringProvider {
        override fun get(string: EchoString): String = "mock_string"
        override val is24HourFormat: Boolean = false
    }

    @Test
    fun `toDomain should prefer explicit chat title over participant name`() {
        // Arrange
        val currentUserId = ParticipantId("user-123")
        val agentId = ParticipantId("agent-456")
        
        val chatEntity = ChatEntity(
            id = ChatId("chat-1"),
            title = "Specific Project Title",
            lastMessage = "Hello",
            lastMessageTimestamp = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        val participants = listOf(
            ParticipantEntity(id = currentUserId, name = "Me", profileImageUrl = null, isAgent = false),
            ParticipantEntity(id = agentId, name = "AI Assistant", profileImageUrl = null, isAgent = true)
        )
        
        val chatWithParticipants = ChatWithParticipants(
            chat = chatEntity,
            participants = participants
        )

        // Act
        val domainChat = chatWithParticipants.toDomain(currentUserId, mockStringProvider)

        // Assert
        assertEquals("Specific Project Title", domainChat.title)
    }

    @Test
    fun `toDomain should fallback to participant name if chat title is blank`() {
        // Arrange
        val currentUserId = ParticipantId("user-123")
        val agentId = ParticipantId("agent-456")
        
        val chatEntity = ChatEntity(
            id = ChatId("chat-1"),
            title = "", // Blank title
            lastMessage = "Hello",
            lastMessageTimestamp = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        val participants = listOf(
            ParticipantEntity(id = currentUserId, name = "Me", profileImageUrl = null, isAgent = false),
            ParticipantEntity(id = agentId, name = "AI Assistant", profileImageUrl = null, isAgent = true)
        )
        
        val chatWithParticipants = ChatWithParticipants(
            chat = chatEntity,
            participants = participants
        )

        // Act
        val domainChat = chatWithParticipants.toDomain(currentUserId, mockStringProvider)

        // Assert
        assertEquals("AI Assistant", domainChat.title)
    }
}
