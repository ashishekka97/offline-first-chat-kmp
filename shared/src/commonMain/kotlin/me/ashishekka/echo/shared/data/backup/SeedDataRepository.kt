package me.ashishekka.echo.shared.data.backup

import me.ashishekka.echo.shared.data.dao.RestorationDao
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity

/**
 * Repository responsible for persisting restored seed data into the database.
 */
interface SeedDataRepository {
    /**
     * Persists all components of the seed data atomically.
     */
    suspend fun saveSeedData(
        participants: List<ParticipantEntity>,
        chats: List<ChatEntity>,
        chatCrossRefs: List<ChatParticipantCrossRef>,
        messages: List<MessageEntity>
    )

    /**
     * Clears all existing data from the database to ensure a clean restoration.
     */
    suspend fun clearExistingData()
}

/**
 * Default implementation of [SeedDataRepository] using [RestorationDao].
 */
class DefaultSeedDataRepository(
    private val restorationDao: RestorationDao
) : SeedDataRepository {

    override suspend fun saveSeedData(
        participants: List<ParticipantEntity>,
        chats: List<ChatEntity>,
        chatCrossRefs: List<ChatParticipantCrossRef>,
        messages: List<MessageEntity>
    ) {
        restorationDao.insertSeedData(participants, chats, chatCrossRefs, messages)
    }

    override suspend fun clearExistingData() {
        restorationDao.clearAllData()
    }
}
