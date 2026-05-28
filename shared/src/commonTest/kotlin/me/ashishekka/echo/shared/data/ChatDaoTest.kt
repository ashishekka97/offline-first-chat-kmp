package me.ashishekka.echo.shared.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.dao.ChatDao
import me.ashishekka.echo.shared.data.dao.ParticipantDao
import me.ashishekka.echo.shared.data.entity.*
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var participantDao: ParticipantDao

    @BeforeTest
    fun setup() {
        val builder = getTestDatabaseBuilder()
        // Use UnconfinedTestDispatcher for synchronous-like testing of Room
        db = createDatabase(builder, kotlinx.coroutines.Dispatchers.Unconfined)
        chatDao = db.chatDao()
        participantDao = db.participantDao()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndGetAllChats() = runTest {
        val chatId = ChatId("chat_1")
        val chat = ChatEntity(
            id = chatId,
            title = "Test Chat",
            lastMessage = "Hello",
            lastMessageTimestamp = 1000L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        chatDao.insertChat(chat)
        
        val chats = chatDao.getAllChats().getData()
        assertEquals(1, chats.size)
        assertEquals(chatId, chats[0].chat.id)
        assertEquals("Hello", chats[0].chat.lastMessage)
    }

    @Test
    fun testChatWithParticipants() = runTest {
        val chatId = ChatId("chat_1")
        val chat = ChatEntity(chatId, "Test Chat", "Hi", 1000L, 1000L, 1000L)
        val p1Id = ParticipantId("user_1")
        val p2Id = ParticipantId("user_2")
        val p1 = ParticipantEntity(p1Id, "Alice", null, false)
        val p2 = ParticipantEntity(p2Id, "Bob", null, true)
        
        chatDao.insertChat(chat)
        participantDao.insertParticipant(p1)
        participantDao.insertParticipant(p2)
        
        chatDao.insertChatParticipantCrossRef(ChatParticipantCrossRef(chatId, p1Id))
        chatDao.insertChatParticipantCrossRef(ChatParticipantCrossRef(chatId, p2Id))
        
        val chats = chatDao.getAllChats().getData()
        assertEquals(1, chats.size)
        assertEquals(2, chats[0].participants.size)
        
        val other = chats[0].getOtherParticipant(p1Id)
        assertEquals("Bob", other?.name)
    }

    @Test
    fun testDeleteChatCascadesToMessages() = runTest {
        val chatId = ChatId("chat_1")
        val chat = ChatEntity(chatId, "Test Chat", "Hi", 1000L, 1000L, 1000L)
        val p1Id = ParticipantId("user_1")
        val user = ParticipantEntity(p1Id, "Alice", null, false)
        val message = MessageEntity(MessageId("msg_1"), chatId, p1Id, "Hello", MessageTypeEntity.TEXT, null, 1001L)
        
        chatDao.insertChat(chat)
        db.participantDao().insertParticipant(user)
        db.messageDao().insertMessage(message)
        
        // Verify message exists
        assertEquals(1, db.messageDao().getMessagesForChat(chatId).getData().size)
        
        // Delete chat
        chatDao.deleteChat(chat)
        
        // Verify message is gone (cascaded)
        assertTrue(db.messageDao().getMessagesForChat(chatId).getData().isEmpty())
    }

    @Test
    fun testChatsAreSortedByTimestamp() = runTest {
        val chat1Id = ChatId("chat_1")
        val chat2Id = ChatId("chat_2")
        val chat1 = ChatEntity(chat1Id, "Old Chat", "Old", 1000L, 1000L, 1000L)
        val chat2 = ChatEntity(chat2Id, "New Chat", "New", 2000L, 2000L, 2000L)
        
        chatDao.insertChat(chat1)
        chatDao.insertChat(chat2)
        
        val chats = chatDao.getAllChats().getData()
        assertEquals(chat2Id, chats[0].chat.id) // Newest first
        assertEquals(chat1Id, chats[1].chat.id)
    }
}
