package me.ashishekka.echo.shared.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId

/**
 * Repository interface for chat operations.
 */
interface ChatRepository {
    /**
     * Returns a paginated [Flow] of [Chat] domain models.
     */
    fun getPagedChats(): Flow<PagingData<Chat>>

    /**
     * Returns a [Flow] of a single [Chat] by its [id].
     */
    fun getChatById(id: ChatId): Flow<Chat?>

    /**
     * Creates a new chat with the given [title] and [participantIds].
     */
    suspend fun createChat(id: ChatId, title: String, participantIds: List<ParticipantId>): Result<Unit, DatabaseError>

    /**
     * Creates a new chat along with its first message atomically.
     */
    suspend fun createChatWithMessage(
        chatId: ChatId,
        title: String,
        participantIds: List<ParticipantId>,
        messageId: MessageId,
        message: String,
        senderId: ParticipantId,
        type: me.ashishekka.echo.shared.data.entity.MessageType = me.ashishekka.echo.shared.data.entity.MessageType.TEXT,
        file: me.ashishekka.echo.shared.data.entity.FileDetails? = null,
        timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    ): Result<Unit, DatabaseError>

    /**
     * Updates the last message details for a chat.
     */
    suspend fun updateLastMessage(chatId: ChatId, message: String, timestamp: Long): Result<Unit, DatabaseError>

    /**
     * Deletes a chat and all its messages.
     */
    suspend fun deleteChat(chatId: ChatId): Result<Unit, DatabaseError>
}
