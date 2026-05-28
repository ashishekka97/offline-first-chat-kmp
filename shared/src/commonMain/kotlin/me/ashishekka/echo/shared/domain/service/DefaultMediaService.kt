package me.ashishekka.echo.shared.domain.service

import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.ThumbnailDetails
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.onFailure
import me.ashishekka.echo.shared.domain.onSuccess

class DefaultMediaService(
    private val localAssetManager: LocalAssetManager,
    private val mediaProcessor: MediaProcessor,
    private val idGenerator: IdGenerator
) : MediaService {

    override suspend fun processImage(
        bytes: ByteArray,
        fileName: String
    ): Result<FileDetails, MediaError> {
        val uniqueId = idGenerator.generateUuid()
        val extension = fileName.substringAfterLast('.', "jpg")
        val localFileName = "img_${uniqueId}.$extension"
        val thumbFileName = "thumb_${uniqueId}.$extension"

        // 1. Downsize the main image
        val downsizedResult = mediaProcessor.downsizeImage(bytes)
        val finalBytes = when (downsizedResult) {
            is Result.Success -> downsizedResult.data
            is Result.Failure -> bytes // Fallback to original if downsizing fails
        }

        // 2. Save the main image
        val saveResult = localAssetManager.writeBytes(localFileName, finalBytes)
        if (saveResult is Result.Failure) {
            return Result.Failure(MediaError.ProcessingFailed)
        }

        val localPath = localAssetManager.getAbsolutePath(localFileName)

        // 3. Generate and save thumbnail (Optional per assignment, but we'll try)
        val thumbResult = mediaProcessor.generateThumbnail(finalBytes)
        val thumbnailDetails = if (thumbResult is Result.Success) {
            val thumbSaveResult = localAssetManager.writeBytes(thumbFileName, thumbResult.data)
            if (thumbSaveResult is Result.Success) {
                ThumbnailDetails(localAssetManager.getAbsolutePath(thumbFileName))
            } else null
        } else null

        return Result.Success(
            FileDetails(
                path = localPath,
                fileSize = finalBytes.size.toLong(),
                thumbnail = thumbnailDetails
            )
        )
    }
}
