package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.withContext
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.di.DispatcherProvider
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Service responsible for extracting media assets from the backup ZIP and generating thumbnails.
 */
interface MediaRestorationService {
    /**
     * Processes media attachments for a list of messages.
     * Extracts files from [zipFileSystem] to local storage and generates thumbnails.
     * Returns a new list of messages with updated local file paths.
     */
    suspend fun processMedia(
        messages: List<MessageEntity>,
        zipFileSystem: FileSystem
    ): List<MessageEntity>
}

/**
 * Default implementation of [MediaRestorationService].
 */
class DefaultMediaRestorationService(
    private val localAssetManager: LocalAssetManager,
    private val mediaProcessor: MediaProcessor,
    private val dispatcherProvider: DispatcherProvider
) : MediaRestorationService {

    override suspend fun processMedia(
        messages: List<MessageEntity>,
        zipFileSystem: FileSystem
    ): List<MessageEntity> = withContext(dispatcherProvider.io) {
        messages.map { message ->
            val fileDetails = message.file ?: return@map message
            
            // If the file already has a local path and no bundled asset name, skip
            val bundledAssetName = fileDetails.path.takeIf { it.isNotBlank() } ?: return@map message
            
            try {
                // 1. Read bytes from ZIP
                val fileBytes = zipFileSystem.read(bundledAssetName.toPath()) {
                    readByteArray()
                }

                // 2. Write main file to local storage
                val localFileName = "file_${message.id}_${bundledAssetName}"
                localAssetManager.writeBytes(localFileName, fileBytes)
                val localFilePath = localAssetManager.getAbsolutePath(localFileName)

                // 3. Generate thumbnail
                val thumbBytes = mediaProcessor.generateThumbnail(fileBytes)
                val thumbPath = if (thumbBytes != null) {
                    val thumbFileName = "thumb_${message.id}_${bundledAssetName}"
                    localAssetManager.writeBytes(thumbFileName, thumbBytes)
                    localAssetManager.getAbsolutePath(thumbFileName)
                } else {
                    ""
                }

                // 4. Update message with local paths
                message.copy(
                    file = fileDetails.copy(
                        path = localFilePath,
                        thumbnail = fileDetails.thumbnail?.copy(path = thumbPath)
                            ?: if (thumbPath.isNotBlank()) me.ashishekka.echo.shared.data.entity.ThumbnailDetails(thumbPath) else null
                    )
                )
            } catch (e: Exception) {
                // Fallback to original message if processing fails
                message
            }
        }
    }
}
