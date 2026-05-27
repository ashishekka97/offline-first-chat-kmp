package me.ashishekka.echo.shared.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.ChatWithParticipants

/**
 * Data Access Object for chat operations.
 */
@Dao
interface ChatDao {
    /**
     * Returns a [Flow] of all chats, ordered by the most recent message timestamp.
     * Each [ChatWithParticipants] includes the chat details and all its participants.
     */
    @Transaction
    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<ChatWithParticipants>>

    /**
     * Returns a [Flow] of a single chat by its [id].
     */
    @Transaction
    @Query("SELECT * FROM chats WHERE id = :id")
    fun getChatById(id: String): Flow<ChatWithParticipants?>

    /**
     * Inserts a new chat or replaces an existing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    /**
     * Inserts a cross-reference between a chat and a participant.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatParticipantCrossRef(crossRef: ChatParticipantCrossRef)

    /**
     * Updates an existing chat.
     */
    @Update
    suspend fun updateChat(chat: ChatEntity)

    /**
     * Deletes a chat.
     */
    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    /**
     * Deletes all chats from the database.
     */
    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    /**
     * Returns a list of all chat IDs.
     */
    @Query("SELECT id FROM chats")
    suspend fun getAllChatIds(): List<String>
}
