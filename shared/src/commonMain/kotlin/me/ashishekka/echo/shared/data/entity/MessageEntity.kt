package me.ashishekka.echo.shared.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

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
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isFromUser: Boolean,
    val type: String, // "text" or "image"
    val mediaUrl: String?
)
