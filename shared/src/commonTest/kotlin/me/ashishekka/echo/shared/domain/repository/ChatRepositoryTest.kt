package me.ashishekka.echo.shared.domain.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.data.*
import me.ashishekka.echo.shared.data.dao.ChatDao
import me.ashishekka.echo.shared.data.dao.ParticipantDao
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.data.repository.OfflineFirstChatRepository
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import me.ashishekka.echo.shared.util.FakeStringProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var chatDao: ChatDao
    private lateinit var participantDao: ParticipantDao
    private lateinit var repository: ChatRepository

    @BeforeTest
    fun setup() {
        val builder = getTestDatabaseBuilder()
        db = createDatabase(builder, kotlinx.coroutines.Dispatchers.Unconfined)
        chatDao = db.chatDao()
        participantDao = db.participantDao()
        repository = OfflineFirstChatRepository(chatDao, db.messageDao(), FakeStringProvider())
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun testGetChatByIdMapsCorrectly() = runTest {
        val chatId = ChatId("chat_1")
        val chat = ChatEntity(chatId, "Original Title", "Msg", 1000L, 1000L, 1000L)
        chatDao.insertChat(chat)
        
        val result = repository.getChatById(chatId).first()
        assertEquals(chatId, result?.id)
        assertEquals("Original Title", result?.title)
    }

    @Test
    fun testOneOnOneChatTitleResolution() = runTest {
        val chatId = ChatId("chat_1")
        // Create a chat with the local user and an agent
        val chat = ChatEntity(chatId, "Group Title", null, 1000L, 1000L, 1000L)
        val user = ParticipantEntity(Constants.CURRENT_USER_ID, "Me", null, false)
        val agent = ParticipantEntity(Constants.DEFAULT_AGENT_ID, "Echo Agent", null, true)
        
        participantDao.insertParticipant(user)
        participantDao.insertParticipant(agent)
        val createResult = repository.createChat(chatId, "Group Title", listOf(user.id, agent.id))
        assertTrue(createResult is Result.Success)
        
        // The title should be resolved to the agent's name because it's 1-on-1
        val result = repository.getChatById(chatId).first()
        assertEquals("Echo Agent", result?.title)
    }

    @Test
    fun testGroupChatTitleRemainsSame() = runTest {
        val chatId = ChatId("chat_1")
        // Create a chat with 3 participants (Group)
        val chat = ChatEntity(chatId, "Team Alpha", null, 1000L, 1000L, 1000L)
        val p1 = ParticipantEntity(Constants.CURRENT_USER_ID, "Me", null, false)
        val p2 = ParticipantEntity(ParticipantId("user_2"), "Bob", null, false)
        val p3 = ParticipantEntity(ParticipantId("user_3"), "Charlie", null, false)
        
        participantDao.insertParticipant(p1)
        participantDao.insertParticipant(p2)
        participantDao.insertParticipant(p3)
        val createResult = repository.createChat(chatId, "Team Alpha", listOf(p1.id, p2.id, p3.id))
        assertTrue(createResult is Result.Success)
        
        // The title should remain "Team Alpha"
        val result = repository.getChatById(chatId).first()
        assertEquals("Team Alpha", result?.title)
    }
}
