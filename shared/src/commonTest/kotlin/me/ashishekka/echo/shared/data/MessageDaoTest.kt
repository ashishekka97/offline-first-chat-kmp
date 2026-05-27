package me.ashishekka.echo.shared.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MessageDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var chatDao: me.ashishekka.echo.shared.data.dao.ChatDao
    private lateinit var participantDao: me.ashishekka.echo.shared.data.dao.ParticipantDao
    private lateinit var messageDao: me.ashishekka.echo.shared.data.dao.MessageDao

    @BeforeTest
    fun setup() {
        val builder = getTestDatabaseBuilder()
        db = createDatabase(builder, kotlinx.coroutines.Dispatchers.Unconfined)
        chatDao = db.chatDao()
        participantDao = db.participantDao()
        messageDao = db.messageDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndGetMessages() = runTest {
        val chat = ChatEntity("chat_1", "Test Chat", null, 1000L, 1000L, 1000L)
        val user = ParticipantEntity("user_1", "Alice", null, false)
        
        chatDao.insertChat(chat)
        participantDao.insertParticipant(user)
        
        val message = MessageEntity(
            id = "msg_1",
            chatId = "chat_1",
            senderId = "user_1",
            message = "Hello World",
            type = MessageType.TEXT,
            file = null,
            timestamp = 1001L
        )
        messageDao.insertMessage(message)
        
        val messages = messageDao.getMessagesForChat("chat_1").first()
        assertEquals(1, messages.size)
        assertEquals("Hello World", messages[0].message.message)
        assertEquals("Alice", messages[0].sender.name)
    }

    @Test
    fun testInsertFileMessage() = runTest {
        val chat = ChatEntity("chat_1", "Test Chat", null, 1000L, 1000L, 1000L)
        val user = ParticipantEntity("user_1", "Alice", null, false)
        
        chatDao.insertChat(chat)
        participantDao.insertParticipant(user)
        
        val message = MessageEntity(
            id = "msg_2",
            chatId = "chat_1",
            senderId = "user_1",
            message = "Image caption",
            type = MessageType.FILE,
            file = FileDetails(
                path = "path/to/file.jpg",
                fileSize = 1024L,
                thumbnail = ThumbnailDetails("path/to/thumb.jpg")
            ),
            timestamp = 1002L
        )
        messageDao.insertMessage(message)
        
        val messages = messageDao.getMessagesForChat("chat_1").first()
        assertEquals(1, messages.size)
        assertEquals(MessageType.FILE, messages[0].message.type)
        assertEquals("path/to/file.jpg", messages[0].message.file?.path)
        assertEquals("path/to/thumb.jpg", messages[0].message.file?.thumbnail?.path)
    }
}
