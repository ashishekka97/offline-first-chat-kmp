package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.dao.RestorationDao
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedDataRepositoryTest {

    private lateinit var restorationDao: FakeRestorationDao
    private lateinit var repository: SeedDataRepository

    @BeforeTest
    fun setup() {
        restorationDao = FakeRestorationDao()
        repository = DefaultSeedDataRepository(restorationDao)
    }

    @Test
    fun testSaveSeedData() = runTest {
        val participants = listOf(ParticipantEntity("u1", "Alice", null, false))
        val chats = listOf(ChatEntity("c1", "Topic", "Hi", 0, 0, 0))
        val crossRefs = listOf(ChatParticipantCrossRef("c1", "u1"))
        val messages = listOf(MessageEntity("m1", "c1", "u1", "Hi", me.ashishekka.echo.shared.data.entity.MessageType.TEXT, null, 0))

        repository.saveSeedData(participants, chats, crossRefs, messages)

        assertTrue(restorationDao.insertCalled)
        assertEquals(1, restorationDao.participants.size)
    }

    @Test
    fun testClearExistingData() = runTest {
        repository.clearExistingData()
        assertTrue(restorationDao.clearCalled)
    }

    class FakeRestorationDao : RestorationDao {
        var insertCalled = false
        var clearCalled = false
        var participants = listOf<ParticipantEntity>()

        override suspend fun insertSeedData(
            participants: List<ParticipantEntity>,
            chats: List<ChatEntity>,
            crossRefs: List<ChatParticipantCrossRef>,
            messages: List<MessageEntity>
        ) {
            insertCalled = true
            this.participants = participants
        }

        override suspend fun clearAllData() {
            clearCalled = true
        }

        override suspend fun insertParticipants(participants: List<ParticipantEntity>) {}
        override suspend fun insertChats(chats: List<ChatEntity>) {}
        override suspend fun insertChatParticipantCrossRefs(crossRefs: List<ChatParticipantCrossRef>) {}
        override suspend fun insertMessages(messages: List<MessageEntity>) {}
        override suspend fun deleteAllParticipants() {}
        override suspend fun deleteAllChats() {}
    }
}
