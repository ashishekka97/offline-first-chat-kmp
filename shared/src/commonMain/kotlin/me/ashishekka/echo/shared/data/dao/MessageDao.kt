package me.ashishekka.echo.shared.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageWithSender
import me.ashishekka.echo.shared.domain.model.ChatId

/**
 * Data Access Object for message operations.
 */
@Dao
interface MessageDao {
    /**
     * Returns a [PagingSource] of all messages for a specific [chatId], ordered by timestamp.
     * Each [MessageWithSender] includes the message details and the sender's profile.
     */
    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: ChatId): PagingSource<Int, MessageWithSender>

    /**
     * Returns a list of all messages for a specific [chatId].
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId")
    suspend fun getMessagesByChatId(chatId: ChatId): List<MessageEntity>

    /**
     * Inserts a new message or replaces an existing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Updates the last message details for a chat.
     */
    @Query("UPDATE chats SET lastMessage = :message, lastMessageTimestamp = :timestamp, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateChatLastMessage(chatId: ChatId, message: String, timestamp: Long)

    /**
     * Atomic transaction to insert a message and update the corresponding chat's last message.
     */
    @Transaction
    suspend fun insertMessageAndUpdateChat(message: MessageEntity) {
        insertMessage(message)
        val previewMessage = if (message.type == me.ashishekka.echo.shared.data.entity.MessageType.FILE && message.message.isBlank()) {
            "Photo"
        } else {
            message.message
        }
        updateChatLastMessage(
            chatId = message.chatId,
            message = previewMessage,
            timestamp = message.timestamp
        )
    }

    /**
     * Deletes all messages for a specific [chatId].
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: ChatId)
}
