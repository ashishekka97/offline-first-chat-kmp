package me.ashishekka.echo.shared.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Message

/**
 * Repository interface for message operations.
 */
interface MessageRepository {
    /**
     * Returns a paginated [Flow] of [Message] domain models for a specific [chatId].
     */
    fun getPagedMessagesForChat(chatId: String): Flow<PagingData<Message>>

    /**
     * Sends a new message in a chat.
     */
    suspend fun sendMessage(
        id: String,
        chatId: String,
        senderId: String,
        message: String,
        type: MessageType = MessageType.TEXT,
        file: FileDetails? = null,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): Result<Unit, DatabaseError>

    /**
     * Deletes all messages for a specific [chatId].
     */
    suspend fun deleteMessagesForChat(chatId: String): Result<Unit, DatabaseError>
}
