package me.ashishekka.echo.shared.domain.usecase

import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.service.AgentService

/**
 * Use case for sending a message in a chat and orchestrating the AI response simulation.
 */
class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val agentService: AgentService
) {
    /**
     * Sends a message and triggers the AI agent simulation if the sender is the current user.
     */
    suspend operator fun invoke(
        id: String,
        chatId: String,
        senderId: String,
        message: String,
        type: MessageType = MessageType.TEXT,
        file: FileDetails? = null,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): Result<Unit, AppError> {
        return messageRepository.sendMessage(
            id = id,
            chatId = chatId,
            senderId = senderId,
            message = message,
            type = type,
            file = file,
            timestamp = timestamp
        ).onSuccess {
            // Trigger AI simulation only if the sender is the current user
            if (senderId == Constants.CURRENT_USER_ID) {
                agentService.triggerReply(chatId)
            }
        }
    }
}
