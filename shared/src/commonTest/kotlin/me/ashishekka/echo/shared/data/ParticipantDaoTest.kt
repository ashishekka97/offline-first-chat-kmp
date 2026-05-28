package me.ashishekka.echo.shared.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.domain.model.ParticipantId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var participantDao: me.ashishekka.echo.shared.data.dao.ParticipantDao

    @BeforeTest
    fun setup() {
        val builder = getTestDatabaseBuilder()
        db = createDatabase(builder, kotlinx.coroutines.Dispatchers.Unconfined)
        participantDao = db.participantDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndGetParticipant() = runTest {
        val participantId = ParticipantId("user_1")
        val participant = ParticipantEntity(participantId, "Alice", "url", false)
        participantDao.insertParticipant(participant)
        
        val result = participantDao.getParticipantById(participantId)
        assertEquals("Alice", result?.name)
        assertEquals("url", result?.profileImageUrl)
    }

    @Test
    fun testInsertReplaceParticipant() = runTest {
        val participantId = ParticipantId("user_1")
        val participant = ParticipantEntity(participantId, "Alice", null, false)
        participantDao.insertParticipant(participant)
        
        val updated = participant.copy(name = "Alice Updated")
        participantDao.insertParticipant(updated) // REPLACE strategy
        
        val result = participantDao.getParticipantById(participantId)
        assertEquals("Alice Updated", result?.name)
    }

    @Test
    fun testGetAllParticipants() = runTest {
        participantDao.insertParticipant(ParticipantEntity(ParticipantId("user_1"), "Alice", null, false))
        participantDao.insertParticipant(ParticipantEntity(ParticipantId("user_2"), "Bob", null, true))
        
        val all = participantDao.getAllParticipants()
        assertEquals(2, all.size)
    }
}
