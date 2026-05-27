package me.ashishekka.echo.shared.data.media

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
     * @return The bytes of the downsized image, or null if processing fails.
     */
    suspend fun downsizeImage(
        imageData: ByteArray,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024,
        quality: Int = 80
    ): ByteArray?

    /**
     * Generates a thumbnail for an image.
     *
     * @param imageData The raw bytes of the image.
     * @param maxDimension The maximum width or height of the resulting thumbnail.
     * @param quality The compression quality (0-100).
     * @return The bytes of the thumbnail, or null if processing fails.
     */
    suspend fun generateThumbnail(
        imageData: ByteArray,
        maxDimension: Int = 256,
        quality: Int = 60
    ): ByteArray?
}
