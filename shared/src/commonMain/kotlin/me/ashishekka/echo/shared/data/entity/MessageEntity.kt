package me.ashishekka.echo.shared.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId

enum class MessageTypeEntity { TEXT, FILE }
enum class MessageSenderEntity { USER, AGENT }

/**
 * Represents a chat message in the database.
 *
 * @property id The unique identifier for the message.
 * @property chatId The ID of the chat this message belongs to.
 * @property senderId The ID of the [ParticipantEntity] who sent the message.
 * @property message The text content or caption of the message.
 * @property type The type of message (e.g., [MessageTypeEntity.TEXT] or [MessageTypeEntity.FILE]).
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
    val id: MessageId,
    val chatId: ChatId,
    val senderId: ParticipantId,
    val message: String,
    val type: MessageTypeEntity,
    @Embedded(prefix = "file_")
    val file: FileDetailsEntity?,
    val timestamp: Long
)

/**
 * Details of a file attached to a message.
 *
 * @property path The local path or remote URL of the file.
 * @property fileSize The size of the file in bytes.
 * @property thumbnail Optional details of a generated thumbnail for this file.
 */
data class FileDetailsEntity(
    val path: String,
    val fileSize: Long,
    @Embedded(prefix = "thumbnail_")
    val thumbnail: ThumbnailDetailsEntity?
)

/**
 * Details of a thumbnail generated for a media file.
 *
 * @property path The local path to the thumbnail image.
 */
data class ThumbnailDetailsEntity(
    val path: String
)
