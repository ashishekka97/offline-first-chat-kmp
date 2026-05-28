package me.ashishekka.echo.shared.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*

/**
 * Repository interface for message operations.
 */
interface MessageRepository {
    /**
     * Returns a paginated [Flow] of [Message] domain models for a specific [chatId].
     */
    fun getPagedMessagesForChat(chatId: ChatId): Flow<PagingData<Message>>

    /**
     * Sends a new message in a chat.
     */
    suspend fun sendMessage(
        id: MessageId,
        chatId: ChatId,
        senderId: ParticipantId,
        message: String,
        type: MessageType = MessageType.TEXT,
        file: FileDetails? = null,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): Result<Unit, DatabaseError>

    /**
     * Returns a list of all physical file paths associated with messages in a chat.
     */
    suspend fun getFilePathsForChat(chatId: ChatId): Result<List<String>, DatabaseError>

    /**
     * Returns a list of all local file paths for messages containing media across all chats.
     */
    suspend fun getAllLocalMediaPaths(): Result<List<String>, DatabaseError>

    /**
     * Deletes all messages for a specific [chatId].
     */
    suspend fun deleteMessagesForChat(chatId: ChatId): Result<Unit, DatabaseError>
}
