package me.ashishekka.echo.shared.domain.model

import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType

/**
 * Domain model representing a chat message.
 *
 * @property id The unique identifier for the message.
 * @property chatId The ID of the chat this message belongs to.
 * @property sender The [Participant] who sent the message.
 * @property message The text content or caption of the message.
 * @property type The type of message (e.g., [MessageType.TEXT] or [MessageType.FILE]).
 * @property file Optional details if the message contains a file or image.
 * @property timestamp The time the message was sent.
 * @property isFromMe True if the message was sent by the local user.
 */
data class Message(
    val id: String,
    val chatId: String,
    val sender: Participant,
    val message: String,
    val type: MessageType,
    val file: FileDetails?,
    val timestamp: Long,
    val isFromMe: Boolean
)
