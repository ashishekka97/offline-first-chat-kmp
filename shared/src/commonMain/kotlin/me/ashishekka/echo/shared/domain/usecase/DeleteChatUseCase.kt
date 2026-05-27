package me.ashishekka.echo.shared.domain.usecase

import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.onSuccess
import me.ashishekka.echo.shared.domain.repository.ChatRepository

/**
 * Use case for deleting a chat and all its associated messages and physical files.
 */
class DeleteChatUseCase(
    private val chatRepository: ChatRepository,
    private val messageDao: MessageDao,
    private val localAssetManager: LocalAssetManager
) {
    /**
     * Deletes the chat, its messages, and any local media files associated with those messages.
     */
    suspend operator fun invoke(chatId: String): Result<Unit, AppError> {
        // 1. Fetch all messages to identify files that need deletion
        val messages = messageDao.getMessagesByChatId(chatId)
        
        // 2. Identify local files (excluding network URLs)
        val filesToDelete = messages.mapNotNull { it.file }
            .flatMap { listOfNotNull(it.path, it.thumbnail?.path) }
            .filter { path -> path.isNotBlank() && !path.startsWith("http") }

        // 3. Delete physical files from disk
        filesToDelete.forEach { path ->
            // Extract filename from path if it's a full path, or just use as is
            val fileName = path.substringAfterLast("/")
            localAssetManager.deleteFile(fileName)
        }

        // 4. Delete the chat and messages from database
        return chatRepository.deleteChat(chatId)
    }
}
