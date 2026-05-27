package me.ashishekka.echo.shared.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.data.mapper.toDomain
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Message
import me.ashishekka.echo.shared.domain.repository.MessageRepository

/**
 * Offline-first implementation of [MessageRepository].
 */
class OfflineFirstMessageRepository(
    private val messageDao: MessageDao
) : MessageRepository {

    override fun getPagedMessagesForChat(chatId: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { messageDao.getMessagesForChat(chatId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(Constants.CURRENT_USER_ID) }
        }
    }

    override suspend fun sendMessage(
        id: String,
        chatId: String,
        senderId: String,
        message: String,
        type: MessageType,
        file: FileDetails?,
        timestamp: Long
    ): Result<Unit, DatabaseError> {
        return try {
            val messageEntity = MessageEntity(
                id = id,
                chatId = chatId,
                senderId = senderId,
                message = message,
                type = type,
                file = file,
                timestamp = timestamp
            )
            messageDao.insertMessage(messageEntity)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DatabaseError.Unknown(e))
        }
    }

    override suspend fun deleteMessagesForChat(chatId: String): Result<Unit, DatabaseError> {
        return try {
            messageDao.deleteMessagesForChat(chatId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DatabaseError.Unknown(e))
        }
    }
}
