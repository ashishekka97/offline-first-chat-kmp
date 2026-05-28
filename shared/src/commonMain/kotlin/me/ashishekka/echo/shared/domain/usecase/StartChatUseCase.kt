package me.ashishekka.echo.shared.domain.usecase

import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.service.AgentService

/**
 * Use case for starting a new chat with an initial message.
 * This ensures the chat, its participants, and the first message are created atomically
 * and triggers the AI agent simulation.
 */
class StartChatUseCase(
    private val chatRepository: ChatRepository,
    private val agentService: AgentService
) {
    /**
     * Creates a new chat and its first message, then triggers the AI simulation.
     */
    suspend operator fun invoke(
        chatId: ChatId,
        title: String,
        participantIds: List<ParticipantId>,
        messageId: MessageId,
        message: String,
        senderId: ParticipantId,
        type: MessageType = MessageType.TEXT,
        file: FileDetails? = null,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): Result<Unit, AppError> {
        return chatRepository.createChatWithMessage(
            chatId = chatId,
            title = title,
            participantIds = participantIds,
            messageId = messageId,
            message = message,
            senderId = senderId,
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
