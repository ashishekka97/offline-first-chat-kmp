package me.ashishekka.echo.shared.domain.service

import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.FileDetails

/**
 * Interface for processing media assets (e.g., images).
 */
interface MediaService {
    /**
     * Processes an image (resizing, thumbnail generation) and returns its details.
     * @param bytes The raw image data.
     * @param originalPath The original path of the image (used for naming).
     * @return [Result] containing [FileDetails] if successful.
     */
    suspend fun processImage(bytes: ByteArray, originalPath: String): Result<FileDetails, MediaError>
}
