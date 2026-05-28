package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.withContext
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.ThumbnailDetailsEntity
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.Result
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath

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

                // 2. Write main file to local storage using SHA-256 hash for deduplication
                val hash = fileBytes.toByteString().sha256().hex()
                val localFileName = "img_$hash.jpg"
                val writeResult = localAssetManager.writeBytes(localFileName, fileBytes)
                if (writeResult is Result.Failure) return@map message
                
                // 3. Generate thumbnail
                val thumbResult = mediaProcessor.generateThumbnail(fileBytes)
                val thumbName = if (thumbResult is Result.Success) {
                    val thumbBytes = thumbResult.data
                    val thumbFileName = "thumb_$localFileName"
                    val thumbWriteResult = localAssetManager.writeBytes(thumbFileName, thumbBytes)
                    if (thumbWriteResult is Result.Success) thumbFileName else ""
                } else {
                    ""
                }

                // 4. Update message with relative local paths (essential for iOS sandbox stability)
                message.copy(
                    file = fileDetails.copy(
                        path = localFileName,
                        thumbnail = if (thumbName.isNotBlank()) {
                            ThumbnailDetailsEntity(thumbName)
                        } else {
                            null
                        }
                    )
                )
            } catch (e: Exception) {
                // Fallback to original message if processing fails
                message
            }
        }
    }
}
