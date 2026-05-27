package me.ashishekka.echo.shared.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long,
    val profileImageUrl: String?
)
