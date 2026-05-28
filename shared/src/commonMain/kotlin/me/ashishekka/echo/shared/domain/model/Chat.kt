package me.ashishekka.echo.shared.domain.model

/**
 * Domain model representing a chat conversation.
 *
 * @property id The unique identifier for the chat.
 * @property title The display title for the chat.
 * @property lastMessage The text content of the last message sent in this chat.
 * @property lastMessageTimestamp The timestamp of the last message.
 * @property createdAt The timestamp when the chat was created.
 * @property updatedAt The timestamp when the chat was last updated.
 */
data class Chat(
    val id: ChatId,
    val title: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long,
    val createdAt: Long,
    val updatedAt: Long
)
