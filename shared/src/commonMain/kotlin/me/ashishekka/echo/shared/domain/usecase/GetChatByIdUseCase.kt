package me.ashishekka.echo.shared.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.repository.ChatRepository

/**
 * Use case for retrieving a single chat by its ID.
 */
class GetChatByIdUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Returns a [Flow] of [Chat] domain model for the given [id], or null if not found.
     */
    operator fun invoke(id: String): Flow<Chat?> {
        return chatRepository.getChatById(id)
    }
}
