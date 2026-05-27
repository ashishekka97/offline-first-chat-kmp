package me.ashishekka.echo.shared.domain.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.ashishekka.echo.shared.domain.model.Chat

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
    fun getChatById(id: String): Flow<Chat?>

    /**
     * Creates a new chat with the given [title] and [participantIds].
     */
    suspend fun createChat(id: String, title: String, participantIds: List<String>)

    /**
     * Updates the last message details for a chat.
     */
    suspend fun updateLastMessage(chatId: String, message: String, timestamp: Long)

    /**
     * Deletes a chat and all its messages.
     */
    suspend fun deleteChat(chatId: String)
}
