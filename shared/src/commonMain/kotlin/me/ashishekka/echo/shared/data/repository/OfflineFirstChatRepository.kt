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
import me.ashishekka.echo.shared.data.mapper.toEntity
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
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

    override fun getChatById(id: ChatId): Flow<Chat?> {
        return chatDao.getChatById(id).map { it?.toDomain(Constants.CURRENT_USER_ID) }
    }

    override suspend fun createChat(id: ChatId, title: String, participantIds: List<ParticipantId>): Result<Unit, DatabaseError> {
        return safeDatabaseCall {
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
        }
    }

    override suspend fun createChatWithMessage(
        chatId: ChatId,
        title: String,
        participantIds: List<ParticipantId>,
        messageId: MessageId,
        message: String,
        senderId: ParticipantId,
        type: MessageType,
        file: FileDetails?,
        timestamp: Long
    ): Result<Unit, DatabaseError> {
        return safeDatabaseCall {
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
                type = type.toEntity(),
                file = file?.toEntity(),
                timestamp = timestamp
            )
            chatDao.insertChatWithMessage(chatEntity, participantIds, messageEntity, messageDao)
        }
    }

    override suspend fun updateLastMessage(chatId: ChatId, message: String, timestamp: Long): Result<Unit, DatabaseError> {
        return safeDatabaseCall {
            chatDao.updateLastMessage(chatId, message, timestamp)
        }
    }

    override suspend fun updateChatTitle(chatId: ChatId, newTitle: String): Result<Unit, DatabaseError> {
        return safeDatabaseCall {
            chatDao.updateChatTitle(chatId, newTitle)
        }
    }

    override suspend fun deleteChat(chatId: ChatId): Result<Unit, DatabaseError> {
        val chatWithParticipants = chatDao.getChatById(chatId).firstOrNull()
        return chatWithParticipants?.chat?.let {
            safeDatabaseCall { chatDao.deleteChat(it) }
        } ?: Result.Failure(DatabaseError.NotFound)
    }
}
