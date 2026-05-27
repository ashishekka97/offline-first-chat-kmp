package me.ashishekka.echo.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageWithSender

/**
 * Data Access Object for message operations.
 */
@Dao
interface MessageDao {
    /**
     * Returns a [Flow] of all messages for a specific [chatId], ordered by timestamp.
     * Each [MessageWithSender] includes the message details and the sender's profile.
     */
    @Transaction
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageWithSender>>

    /**
     * Inserts a new message or replaces an existing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Deletes all messages for a specific [chatId].
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)
}
