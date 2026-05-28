package me.ashishekka.echo.shared.domain.service

import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result

/**
 * Service responsible for high-level media operations, bridging the gap between
 * platform-specific picking/capture and the persistent data layer.
 */
interface MediaService {
    /**
     * Processes an image by downsizing it and generating a thumbnail,
     * then saving both to local application storage.
     *
     * @param bytes The raw bytes of the image.
     * @param fileName A suggestive name for the file (e.g., "capture.jpg").
     * @return A [Result] containing [FileDetails] with local paths, or a [MediaError].
     */
    suspend fun processImage(
        bytes: ByteArray,
        fileName: String
    ): Result<FileDetails, MediaError>
}
