package me.ashishekka.echo.shared.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var chatDao: me.ashishekka.echo.shared.data.dao.ChatDao
    private lateinit var participantDao: me.ashishekka.echo.shared.data.dao.ParticipantDao

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
        val chat = ChatEntity(
            id = "chat_1",
            lastMessage = "Hello",
            lastMessageTimestamp = 1000L
        )
        chatDao.insertChat(chat)
        
        val chats = chatDao.getAllChats().first()
        assertEquals(1, chats.size)
        assertEquals("chat_1", chats[0].chat.id)
        assertEquals("Hello", chats[0].chat.lastMessage)
    }

    @Test
    fun testChatWithParticipants() = runTest {
        val chat = ChatEntity("chat_1", "Hi", 1000L)
        val p1 = ParticipantEntity("user_1", "Alice", null, false)
        val p2 = ParticipantEntity("user_2", "Bob", null, true)
        
        chatDao.insertChat(chat)
        participantDao.insertParticipant(p1)
        participantDao.insertParticipant(p2)
        
        chatDao.insertChatParticipantCrossRef(ChatParticipantCrossRef("chat_1", "user_1"))
        chatDao.insertChatParticipantCrossRef(ChatParticipantCrossRef("chat_1", "user_2"))
        
        val chats = chatDao.getAllChats().first()
        assertEquals(1, chats.size)
        assertEquals(2, chats[0].participants.size)
        
        val other = chats[0].getOtherParticipant("user_1")
        assertEquals("Bob", other?.name)
    }

    @Test
    fun testDeleteChatCascadesToMessages() = runTest {
        val chat = ChatEntity("chat_1", "Hi", 1000L)
        val user = ParticipantEntity("user_1", "Alice", null, false)
        val message = MessageEntity("msg_1", "chat_1", "user_1", "Hello", MessageType.TEXT, null, 1001L)
        
        chatDao.insertChat(chat)
        db.participantDao().insertParticipant(user)
        db.messageDao().insertMessage(message)
        
        // Verify message exists
        assertEquals(1, db.messageDao().getMessagesForChat("chat_1").first().size)
        
        // Delete chat
        chatDao.deleteChat(chat)
        
        // Verify message is gone (cascaded)
        assertTrue(db.messageDao().getMessagesForChat("chat_1").first().isEmpty())
    }

    @Test
    fun testChatsAreSortedByTimestamp() = runTest {
        val chat1 = ChatEntity("chat_1", "Old", 1000L)
        val chat2 = ChatEntity("chat_2", "New", 2000L)
        
        chatDao.insertChat(chat1)
        chatDao.insertChat(chat2)
        
        val chats = chatDao.getAllChats().first()
        assertEquals("chat_2", chats[0].chat.id) // Newest first
        assertEquals("chat_1", chats[1].chat.id)
    }
}
