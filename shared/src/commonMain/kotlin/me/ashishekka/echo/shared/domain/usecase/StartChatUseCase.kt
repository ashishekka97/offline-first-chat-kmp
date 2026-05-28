package me.ashishekka.echo.shared.domain.usecase

import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.service.AgentService
import me.ashishekka.echo.shared.domain.service.MediaService

import me.ashishekka.echo.shared.util.EchoString
import me.ashishekka.echo.shared.util.StringProvider

/**
 * Use case for starting a new chat with an initial message.
 * This ensures the chat, its participants, and the first message are created atomically
 * and triggers the AI agent simulation.
 */
class StartChatUseCase(
    private val chatRepository: ChatRepository,
    private val agentService: AgentService,
    private val mediaService: MediaService,
    private val localAssetManager: LocalAssetManager,
    private val stringProvider: StringProvider
) {
    /**
     * Creates a new chat and its first message, then triggers the AI simulation.
     */
    suspend operator fun invoke(
        chatId: ChatId,
        participantIds: List<ParticipantId>,
        messageId: MessageId,
        message: String,
        senderId: ParticipantId,
        type: MessageType = MessageType.TEXT,
        localMediaPath: String? = null,
        timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ): Result<Unit, AppError> {
        var finalType = type
        var finalFileDetails: FileDetails? = null

        if (localMediaPath != null) {
            val bytesResult = localAssetManager.readUriBytes(localMediaPath)
            if (bytesResult is Result.Success) {
                val processResult = mediaService.processImage(bytesResult.data, localMediaPath)
                if (processResult is Result.Success) {
                    finalFileDetails = processResult.data
                    finalType = MessageType.FILE
                }
            }
        }

        // Assignment: Chat title auto-generated from first message
        val autoTitle = if (message.isNotBlank()) {
            message.take(20).let { if (it.length < message.length) "$it..." else it }
        } else if (finalType == MessageType.FILE) {
            stringProvider.get(EchoString.ImageChat)
        } else {
            stringProvider.get(EchoString.NewChat)
        }

        return chatRepository.createChatWithMessage(
            chatId = chatId,
            title = autoTitle,
            participantIds = participantIds,
            messageId = messageId,
            message = message,
            senderId = senderId,
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
