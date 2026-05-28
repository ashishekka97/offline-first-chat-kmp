package me.ashishekka.echo.shared.domain.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.*
import me.ashishekka.echo.shared.data.dao.ParticipantDao
import me.ashishekka.echo.shared.data.repository.OfflineFirstParticipantRepository
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Participant
import me.ashishekka.echo.shared.domain.model.ParticipantId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var participantDao: ParticipantDao
    private lateinit var repository: ParticipantRepository

    @BeforeTest
    fun setup() {
        val builder = getTestDatabaseBuilder()
        db = createDatabase(builder, kotlinx.coroutines.Dispatchers.Unconfined)
        participantDao = db.participantDao()
        repository = OfflineFirstParticipantRepository(participantDao)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSaveAndGetParticipant() = runTest {
        val participantId = ParticipantId("u1")
        val participant = Participant(participantId, "Alice", null, false)
        
        val saveResult = repository.saveParticipant(participant)
        assertTrue(saveResult is Result.Success)
        
        val getResult = repository.getParticipantById(participantId)
        assertTrue(getResult is Result.Success)
        assertEquals(participant, getResult.data)
    }

    @Test
    fun testGetAllParticipants() = runTest {
        repository.saveParticipant(Participant(ParticipantId("u1"), "Alice", null, false))
        repository.saveParticipant(Participant(ParticipantId("u2"), "Bob", null, true))
        
        val result = repository.getAllParticipants()
        assertTrue(result is Result.Success)
        assertEquals(2, result.data.size)
    }

    @Test
    fun testGetNonExistentParticipantFails() = runTest {
        val result = repository.getParticipantById(ParticipantId("unknown"))
        assertTrue(result is Result.Failure)
    }
}
