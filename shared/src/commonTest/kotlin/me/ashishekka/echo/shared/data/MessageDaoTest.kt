package me.ashishekka.echo.shared.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.dao.ChatDao
import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.dao.ParticipantDao
import me.ashishekka.echo.shared.data.entity.*
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MessageDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var participantDao: ParticipantDao
    private lateinit var messageDao: MessageDao

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
        val chatId = ChatId("chat_1")
        val participantId = ParticipantId("user_1")
        val chat = ChatEntity(chatId, "Test Chat", null, 1000L, 1000L, 1000L)
        val user = ParticipantEntity(participantId, "Alice", null, false)
        
        chatDao.insertChat(chat)
        participantDao.insertParticipant(user)
        
        val message = MessageEntity(
            id = MessageId("msg_1"),
            chatId = chatId,
            senderId = participantId,
            message = "Hello World",
            type = MessageTypeEntity.TEXT,
            file = null,
            timestamp = 1001L
        )
        messageDao.insertMessage(message)
        
        val messages = messageDao.getMessagesForChat(chatId).getData()
        assertEquals(1, messages.size)
        assertEquals("Hello World", messages[0].message.message)
        assertEquals("Alice", messages[0].sender.name)
    }

    @Test
    fun testInsertFileMessage() = runTest {
        val chatId = ChatId("chat_1")
        val participantId = ParticipantId("user_1")
        val chat = ChatEntity(chatId, "Test Chat", null, 1000L, 1000L, 1000L)
        val user = ParticipantEntity(participantId, "Alice", null, false)
        
        chatDao.insertChat(chat)
        participantDao.insertParticipant(user)
        
        val message = MessageEntity(
            id = MessageId("msg_2"),
            chatId = chatId,
            senderId = participantId,
            message = "Image caption",
            type = MessageTypeEntity.FILE,
            file = FileDetailsEntity(
                path = "path/to/file.jpg",
                fileSize = 1024L,
                thumbnail = ThumbnailDetailsEntity("path/to/thumb.jpg")
            ),
            timestamp = 1002L
        )
        messageDao.insertMessage(message)
        
        val messages = messageDao.getMessagesForChat(chatId).getData()
        assertEquals(1, messages.size)
        assertEquals(MessageTypeEntity.FILE, messages[0].message.type)
        assertEquals("path/to/file.jpg", messages[0].message.file?.path)
        assertEquals("path/to/thumb.jpg", messages[0].message.file?.thumbnail?.path)
    }
}
