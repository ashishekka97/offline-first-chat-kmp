package me.ashishekka.echo.shared.domain.model

/**
 * The type of message.
 */
enum class MessageType { TEXT, FILE }

/**
 * Details of a file attached to a message.
 */
data class FileDetails(
    val path: String,
    val fileSize: Long,
    val thumbnail: ThumbnailDetails?,
    val fullPath: String = ""
)

/**
 * Details of a thumbnail generated for a media file.
 */
data class ThumbnailDetails(
    val path: String,
    val fullPath: String = ""
)

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
    val id: MessageId,
    val chatId: ChatId,
    val sender: Participant,
    val message: String,
    val type: MessageType,
    val file: FileDetails?,
    val timestamp: Long,
    val isFromMe: Boolean,
    val displayTimestamp: String = "",
    val displaySize: String = ""
)
