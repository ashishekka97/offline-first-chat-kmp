package me.ashishekka.echo.shared.data.dao

import androidx.paging.PagingSource
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
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.ParticipantId

/**
 * Data Access Object for chat operations.
 */
@Dao
interface ChatDao {
    /**
     * Returns a [PagingSource] of all chats, ordered by the most recent message timestamp.
     * Each [ChatWithParticipants] includes the chat details and all its participants.
     */
    @Transaction
    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    fun getAllChats(): PagingSource<Int, ChatWithParticipants>

    /**
     * Returns a [Flow] of a single chat by its [id].
     */
    @Transaction
    @Query("SELECT * FROM chats WHERE id = :id")
    fun getChatById(id: ChatId): Flow<ChatWithParticipants?>

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
     * Updates the last message details for a chat.
     */
    @Query("UPDATE chats SET lastMessage = :message, lastMessageTimestamp = :timestamp, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: ChatId, message: String, timestamp: Long)

    /**
     * Updates the title for a chat.
     */
    @Query("UPDATE chats SET title = :newTitle, updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: ChatId, newTitle: String, timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())

    /**
     * Atomic transaction to create a chat with its participants.
     */
    @Transaction
    suspend fun insertChatWithParticipants(chat: ChatEntity, participantIds: List<ParticipantId>) {
        insertChat(chat)
        participantIds.forEach { id ->
            insertChatParticipantCrossRef(ChatParticipantCrossRef(chat.id, id))
        }
    }

    /**
     * Atomic transaction to create a chat, its participants, and its first message.
     */
    @Transaction
    suspend fun insertChatWithMessage(
        chat: ChatEntity,
        participantIds: List<ParticipantId>,
        message: me.ashishekka.echo.shared.data.entity.MessageEntity,
        messageDao: me.ashishekka.echo.shared.data.dao.MessageDao
    ) {
        insertChatWithParticipants(chat, participantIds)
        messageDao.insertMessageAndUpdateChat(message)
    }

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
    suspend fun getAllChatIds(): List<ChatId>
}
