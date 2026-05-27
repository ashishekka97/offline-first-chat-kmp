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
    fun testSendMessageSavesToDatabase() = runTest {
        val chat = ChatEntity("chat_1", "Test", null, 1000L, 1000L, 1000L)
        val user = ParticipantEntity(Constants.CURRENT_USER_ID, "Me", null, false)
        
        db.chatDao().insertChat(chat)
        db.participantDao().insertParticipant(user)
        
        val result = repository.sendMessage("msg_1", "chat_1", user.id, "Hello")
        assertTrue(result is Result.Success)
        
        val messages = messageDao.getMessagesForChat("chat_1").getData()
        assertEquals(1, messages.size)
        assertEquals("Hello", messages[0].message.message)
    }
}
