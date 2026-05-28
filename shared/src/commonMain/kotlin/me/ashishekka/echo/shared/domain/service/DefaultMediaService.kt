package me.ashishekka.echo.shared.domain.service

import kotlinx.coroutines.withContext
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.FileDetails
import me.ashishekka.echo.shared.domain.model.ThumbnailDetails
import okio.ByteString.Companion.toByteString

/**
 * Default implementation of [MediaService].
 */
class DefaultMediaService(
    private val mediaProcessor: MediaProcessor,
    private val localAssetManager: LocalAssetManager,
    private val dispatcherProvider: DispatcherProvider
) : MediaService {

    override suspend fun processImage(bytes: ByteArray, originalPath: String): Result<FileDetails, MediaError> =
        withContext(dispatcherProvider.io) {
            try {
                // Deduplicate files using SHA-256 hash of the content
                val hash = bytes.toByteString().sha256().hex()
                val fileName = "img_$hash.jpg"

                val thumbnailBytes = when (val thumbResult = mediaProcessor.generateThumbnail(bytes)) {
                    is Result.Success -> thumbResult.data
                    is Result.Failure -> return@withContext Result.Failure(thumbResult.error)
                }

                val thumbName = "thumb_$fileName"
                val thumbWriteResult = localAssetManager.writeBytes(thumbName, thumbnailBytes)

                if (thumbWriteResult is Result.Failure) {
                    return@withContext Result.Failure(MediaError.ProcessingFailed)
                }

                // Save the main image locally
                val mainResult = localAssetManager.writeBytes(fileName, bytes)
                if (mainResult is Result.Failure) {
                    return@withContext Result.Failure(MediaError.ProcessingFailed)
                }

                Result.Success(
                    FileDetails(
                        path = fileName,
                        fileSize = bytes.size.toLong(),
                        thumbnail = ThumbnailDetails(path = thumbName)
                    )
                )
            } catch (e: Exception) {
                Result.Failure(MediaError.Unknown(e))
            }
        }
}
