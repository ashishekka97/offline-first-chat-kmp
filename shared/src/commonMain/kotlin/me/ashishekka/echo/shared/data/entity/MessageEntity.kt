package me.ashishekka.echo.shared.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageType { TEXT, FILE }
enum class MessageSender { USER, AGENT }

/**
 * Represents a chat message in the database.
 *
 * @property id The unique identifier for the message.
 * @property chatId The ID of the chat this message belongs to.
 * @property senderId The ID of the [ParticipantEntity] who sent the message.
 * @property message The text content or caption of the message.
 * @property type The type of message (e.g., [MessageType.TEXT] or [MessageType.FILE]).
 * @property file Optional details if the message contains a file or image.
 * @property timestamp The time the message was sent.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["senderId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val senderId: String,
    val message: String,
    val type: MessageType,
    @Embedded(prefix = "file_")
    val file: FileDetails?,
    val timestamp: Long
)

/**
 * Details of a file attached to a message.
 *
 * @property path The local path or remote URL of the file.
 * @property fileSize The size of the file in bytes.
 * @property thumbnail Optional details of a generated thumbnail for this file.
 */
data class FileDetails(
    val path: String,
    val fileSize: Long,
    @Embedded(prefix = "thumbnail_")
    val thumbnail: ThumbnailDetails?
)

/**
 * Details of a thumbnail generated for a media file.
 *
 * @property path The local path to the thumbnail image.
 */
data class ThumbnailDetails(
    val path: String
)
