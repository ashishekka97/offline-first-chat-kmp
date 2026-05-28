package me.ashishekka.echo.shared.domain.usecase

import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.MediaService

/**
 * Use case for sending a message in a chat and orchestrating the AI response simulation.
 */
class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val agentService: AgentService,
    private val mediaService: MediaService,
    private val localAssetManager: LocalAssetManager
) {
    /**
     * Sends a message and triggers the AI agent simulation if the sender is the current user.
     */
    suspend operator fun invoke(
        id: MessageId,
        chatId: ChatId,
        senderId: ParticipantId,
        message: String,
        type: MessageType = MessageType.TEXT,
        localMediaPath: String? = null,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): Result<Unit, AppError> {
        var finalType = type
        var finalFileDetails: FileDetails? = null

        if (localMediaPath != null) {
            // If it's a local file path (from picker), process it.
            // Requirement: Don't silently fail if the user intentionally attached a file.
            val bytesResult = localAssetManager.readUriBytes(localMediaPath)
            when (bytesResult) {
                is Result.Success -> {
                    val processResult = mediaService.processImage(bytesResult.data, localMediaPath)
                    when (processResult) {
                        is Result.Success -> {
                            finalFileDetails = processResult.data
                            finalType = MessageType.FILE
                        }
                        is Result.Failure -> return Result.Failure(processResult.error)
                    }
                }
                is Result.Failure -> return Result.Failure(bytesResult.error)
            }
        }

        return messageRepository.sendMessage(
            id = id,
            chatId = chatId,
            senderId = senderId,
            message = message,
            type = finalType,
            file = finalFileDetails,
            timestamp = timestamp
        ).onSuccess {
            // Trigger AI simulation only if the sender is the current user
            if (senderId == Constants.CURRENT_USER_ID) {
                agentService.triggerReply(chatId)
            }
        }
    }
}
