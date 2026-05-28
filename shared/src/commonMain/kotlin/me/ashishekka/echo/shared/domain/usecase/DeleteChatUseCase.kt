package me.ashishekka.echo.shared.domain.usecase

import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.repository.ChatRepository
import me.ashishekka.echo.shared.domain.repository.MessageRepository

/**
 * Use case for deleting a chat and all its associated messages and physical files.
 */
class DeleteChatUseCase(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val localAssetManager: LocalAssetManager
) {
    /**
     * Deletes the chat, its messages, and any local media files associated with those messages.
     */
    suspend operator fun invoke(chatId: ChatId): Result<Unit, AppError> {
        // 1. Fetch all associated file paths via the Repository (not DAO)
        messageRepository.getFilePathsForChat(chatId).onSuccess { filesToDelete ->
            // 2. Delete physical files from disk
            filesToDelete.forEach { path ->
                // Extract filename from path if it's a full path, or just use as is
                val fileName = path.substringAfterLast("/")
                localAssetManager.deleteFile(fileName)
            }
        }

        // 3. Delete the chat and messages from database
        return chatRepository.deleteChat(chatId)
    }
}
