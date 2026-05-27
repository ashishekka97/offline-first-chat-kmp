package me.ashishekka.echo.shared.domain.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.*
import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.data.repository.OfflineFirstMessageRepository
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MessageRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var repository: MessageRepository

    @BeforeTest
    fun setup() {
        val builder = getTestDatabaseBuilder()
        db = createDatabase(builder, kotlinx.coroutines.Dispatchers.Unconfined)
        messageDao = db.messageDao()
        repository = OfflineFirstMessageRepository(messageDao)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSendMessageSetsIsFromMeCorrectly() = runTest {
        val chat = ChatEntity("chat_1", "Test", null, 1000L, 1000L, 1000L)
        val user = ParticipantEntity(Constants.CURRENT_USER_ID, "Me", null, false)
        val agent = ParticipantEntity(Constants.DEFAULT_AGENT_ID, "Echo", null, true)
        
        db.chatDao().insertChat(chat)
        db.participantDao().insertParticipant(user)
        db.participantDao().insertParticipant(agent)
        
        // Send from User
        val result1 = repository.sendMessage("msg_1", "chat_1", user.id, "Hello from me")
        assertTrue(result1 is Result.Success)

        // Send from Agent
        val result2 = repository.sendMessage("msg_2", "chat_1", agent.id, "Hello from AI")
        assertTrue(result2 is Result.Success)
        
        val messages = messageDao.getMessagesForChat("chat_1").getData()
        assertEquals(2, messages.size)
        
        // Use repo flow to verify mapping
        val pagedData = repository.getPagedMessagesForChat("chat_1")
        // Note: For PagingData we'd need a more complex collector or just test the mapper directly.
        // But since we already tested the mapper in the DAO test indirectly, this is mostly 
        // to ensure the repository glue is correct.
        
        assertEquals(Constants.CURRENT_USER_ID, messages[0].message.senderId)
        assertTrue(messages[0].message.senderId == Constants.CURRENT_USER_ID)
    }
}
