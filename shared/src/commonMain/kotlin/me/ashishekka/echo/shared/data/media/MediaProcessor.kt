package me.ashishekka.echo.shared.data.media

import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result

/**
 * Interface for processing media files, specifically images for downsizing and thumbnail generation.
 */
interface MediaProcessor {
    /**
     * Downsizes an image to fit within the specified [maxWidth] and [maxHeight] while maintaining aspect ratio.
     *
     * @param imageData The raw bytes of the image.
     * @param maxWidth The maximum width of the resulting image.
     * @param maxHeight The maximum height of the resulting image.
     * @param quality The compression quality (0-100).
     * @return A [Result] containing the bytes of the downsized image or a [MediaError].
     */
    suspend fun downsizeImage(
        imageData: ByteArray,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 80
    ): Result<ByteArray, MediaError>

    /**
     * Generates a thumbnail for an image.
     *
     * @param imageData The raw bytes of the image.
     * @param maxDimension The maximum width or height of the resulting thumbnail.
     * @param quality The compression quality (0-100).
     * @return A [Result] containing the bytes of the thumbnail or a [MediaError].
     */
    suspend fun generateThumbnail(
        imageData: ByteArray,
        maxDimension: Int = 256,
        quality: Int = 60
    ): Result<ByteArray, MediaError>
}
