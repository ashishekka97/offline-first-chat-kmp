package me.ashishekka.echo.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity

/**
 * Specialized DAO for atomic restoration operations.
 */
@Dao
interface RestorationDao {

    @Transaction
    suspend fun insertSeedData(
        participants: List<ParticipantEntity>,
        chats: List<ChatEntity>,
        crossRefs: List<ChatParticipantCrossRef>,
        messages: List<MessageEntity>
    ) {
        insertParticipants(participants)
        insertChats(chats)
        insertChatParticipantCrossRefs(crossRefs)
        insertMessages(messages)
    }

    @Transaction
    suspend fun clearAllData() {
        // Cascading deletes will handle messages and cross-refs
        deleteAllParticipants()
        deleteAllChats()
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatParticipantCrossRefs(crossRefs: List<ChatParticipantCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM participants")
    suspend fun deleteAllParticipants()

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()
}
