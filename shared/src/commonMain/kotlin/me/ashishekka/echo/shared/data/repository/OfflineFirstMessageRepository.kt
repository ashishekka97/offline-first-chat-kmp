package me.ashishekka.echo.shared.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.mapper.toDomain
import me.ashishekka.echo.shared.data.mapper.toEntity
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.util.StringProvider

/**
 * Offline-first implementation of [MessageRepository].
 */
class OfflineFirstMessageRepository(
    private val messageDao: MessageDao,
    private val stringProvider: StringProvider,
    private val localAssetManager: LocalAssetManager
) : MessageRepository {

    override fun getPagedMessagesForChat(chatId: ChatId): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { messageDao.getMessagesForChat(chatId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(Constants.CURRENT_USER_ID, stringProvider, localAssetManager) }
        }
    }

    override suspend fun sendMessage(
        id: MessageId,
        chatId: ChatId,
        senderId: ParticipantId,
        message: String,
        type: MessageType,
        file: FileDetails?,
        timestamp: Long
    ): Result<Unit, DatabaseError> {
        return safeDatabaseCall {
            val messageEntity = MessageEntity(
                id = id,
                chatId = chatId,
                senderId = senderId,
                message = message,
                type = type.toEntity(),
                file = file?.toEntity(),
                timestamp = timestamp
            )
            messageDao.insertMessageAndUpdateChat(messageEntity)
        }
    }

    override suspend fun getFilePathsForChat(chatId: ChatId): Result<List<String>, DatabaseError> {
        return safeDatabaseCall {
            val messages = messageDao.getMessagesByChatId(chatId)
            messages.mapNotNull { it.file }
                .flatMap { listOfNotNull(it.path, it.thumbnail?.path) }
                .filter { it.isNotBlank() }
        }
    }

    override suspend fun getAllLocalMediaPaths(): Result<List<String>, DatabaseError> {
        return safeDatabaseCall {
            val messages = messageDao.getAllMediaMessages()
            messages.mapNotNull { it.file }
                .flatMap { listOfNotNull(it.path, it.thumbnail?.path) }
                .filter { it.isNotBlank() }
        }
    }

    override suspend fun deleteMessagesForChat(chatId: ChatId): Result<Unit, DatabaseError> {
        return safeDatabaseCall {
            messageDao.deleteMessagesForChat(chatId)
        }
    }
}
