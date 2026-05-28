package me.ashishekka.echo.shared.domain.service

import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.FileDetails
import me.ashishekka.echo.shared.domain.model.ThumbnailDetails

/**
 * Default implementation of [MediaService].
 */
class DefaultMediaService(
    private val mediaProcessor: MediaProcessor,
    private val localAssetManager: LocalAssetManager,
    private val dispatcherProvider: DispatcherProvider
) : MediaService {

    override suspend fun processImage(bytes: ByteArray, originalPath: String): Result<FileDetails, MediaError> {
        return try {
            val fileName = originalPath.substringAfterLast("/")
            
            val thumbnailBytes = when (val thumbResult = mediaProcessor.generateThumbnail(bytes)) {
                is Result.Success -> thumbResult.data
                is Result.Failure -> return Result.Failure(thumbResult.error)
            }
            
            val thumbName = "thumb_$fileName"
            val thumbWriteResult = localAssetManager.writeBytes(thumbName, thumbnailBytes)
            
            if (thumbWriteResult is Result.Failure) {
                return Result.Failure(MediaError.ProcessingFailed)
            }

            // In a real app, we might also downscale the main image and save it
            val mainResult = localAssetManager.writeBytes(fileName, bytes)
            if (mainResult is Result.Failure) {
                return Result.Failure(MediaError.ProcessingFailed)
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
