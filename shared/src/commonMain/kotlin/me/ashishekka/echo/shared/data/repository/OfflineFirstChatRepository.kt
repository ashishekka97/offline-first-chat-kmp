package me.ashishekka.echo.shared.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.dao.ChatDao
import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.mapper.toDomain
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.repository.ChatRepository

/**
 * Offline-first implementation of [ChatRepository].
 */
class OfflineFirstChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun getPagedChats(): Flow<PagingData<Chat>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { chatDao.getAllChats() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(Constants.CURRENT_USER_ID) }
        }
    }

    override fun getChatById(id: String): Flow<Chat?> {
        return chatDao.getChatById(id).map { it?.toDomain(Constants.CURRENT_USER_ID) }
    }

    override suspend fun createChat(id: String, title: String, participantIds: List<String>): Result<Unit, DatabaseError> {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val chatEntity = ChatEntity(
                id = id,
                title = title,
                lastMessage = null,
                lastMessageTimestamp = now,
                createdAt = now,
                updatedAt = now
            )
            chatDao.insertChatWithParticipants(chatEntity, participantIds)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DatabaseError.Unknown(e))
        }
    }

    override suspend fun createChatWithMessage(
        chatId: String,
        title: String,
        participantIds: List<String>,
        messageId: String,
        message: String,
        senderId: String,
        type: me.ashishekka.echo.shared.data.entity.MessageType,
        file: me.ashishekka.echo.shared.data.entity.FileDetails?,
        timestamp: Long
    ): Result<Unit, DatabaseError> {
        return try {
            val chatEntity = ChatEntity(
                id = chatId,
                title = title,
                lastMessage = null,
                lastMessageTimestamp = timestamp,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            val messageEntity = MessageEntity(
                id = messageId,
                chatId = chatId,
                senderId = senderId,
                message = message,
                type = type,
                file = file,
                timestamp = timestamp
            )
            chatDao.insertChatWithMessage(chatEntity, participantIds, messageEntity, messageDao)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DatabaseError.Unknown(e))
        }
    }

    override suspend fun updateLastMessage(chatId: String, message: String, timestamp: Long): Result<Unit, DatabaseError> {
        return try {
            chatDao.updateLastMessage(chatId, message, timestamp)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DatabaseError.Unknown(e))
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit, DatabaseError> {
        return try {
            // ChatDao uses CASCADE for messages, so deleting the chat deletes its messages too.
            val chatWithParticipants = chatDao.getChatById(chatId).firstOrNull()
            chatWithParticipants?.chat?.let { 
                chatDao.deleteChat(it)
                Result.Success(Unit)
            } ?: Result.Failure(DatabaseError.NotFound)
        } catch (e: Exception) {
            Result.Failure(DatabaseError.Unknown(e))
        }
    }
}
