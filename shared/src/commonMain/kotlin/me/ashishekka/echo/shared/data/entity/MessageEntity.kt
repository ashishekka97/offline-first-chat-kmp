package me.ashishekka.echo.shared.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageType { TEXT, FILE }
enum class MessageSender { USER, AGENT }

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chatId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val message: String,
    val type: MessageType,
    @Embedded(prefix = "file_")
    val file: FileDetails?,
    val sender: MessageSender,
    val timestamp: Long
)

data class FileDetails(
    val path: String,
    val fileSize: Long,
    @Embedded(prefix = "thumbnail_")
    val thumbnail: ThumbnailDetails?
)

data class ThumbnailDetails(
    val path: String
)
