package me.ashishekka.echo.shared.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.ashishekka.echo.shared.domain.model.Message
import me.ashishekka.echo.shared.domain.repository.MessageRepository

/**
 * Use case for retrieving a paginated list of messages for a specific chat.
 */
class GetPagedMessagesUseCase(
    private val messageRepository: MessageRepository
) {
    /**
     * Returns a [Flow] of [PagingData] containing [Message] domain models for the given [chatId].
     */
    operator fun invoke(chatId: String): Flow<PagingData<Message>> {
        return messageRepository.getPagedMessagesForChat(chatId)
    }
}
