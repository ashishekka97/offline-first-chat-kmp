package me.ashishekka.echo.shared.domain.usecase

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.repository.ChatRepository

/**
 * Use case for retrieving a paginated list of chats.
 */
class GetPagedChatsUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Returns a [Flow] of [PagingData] containing [Chat] domain models.
     */
    operator fun invoke(): Flow<PagingData<Chat>> {
        return chatRepository.getPagedChats()
    }
}
