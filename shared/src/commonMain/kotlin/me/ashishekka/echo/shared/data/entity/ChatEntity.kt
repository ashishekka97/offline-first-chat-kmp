package me.ashishekka.echo.shared.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a chat conversation in the database.
 *
 * @property id The unique identifier for the chat.
 * @property lastMessage The text content of the last message sent in this chat.
 * @property lastMessageTimestamp The timestamp of the last message, used for sorting the chat list.
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long
)
