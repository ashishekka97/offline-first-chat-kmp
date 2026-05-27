package me.ashishekka.echo.shared.data.media

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.withContext
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
class IosMediaProcessor(
    private val dispatcherProvider: DispatcherProvider
) : MediaProcessor {

    override suspend fun downsizeImage(
        imageData: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): Result<ByteArray, MediaError> = withContext(dispatcherProvider.default) {
        try {
            val uiImage = imageData.toUIImage() ?: return@withContext Result.Failure(MediaError.InvalidData)
            
            val currentWidth = uiImage.size.useContents { width }
            val currentHeight = uiImage.size.useContents { height }
            
            val scaledImage = if (currentWidth > maxWidth.toDouble() || currentHeight > maxHeight.toDouble()) {
                val ratio = min(
                    maxWidth.toDouble() / currentWidth,
                    maxHeight.toDouble() / currentHeight
                )
                val targetWidth = currentWidth * ratio
                val targetHeight = currentHeight * ratio
                
                UIGraphicsBeginImageContextWithOptions(
                    size = CGSizeMake(targetWidth, targetHeight),
                    opaque = false,
                    scale = 1.0
                )
                uiImage.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
                val result = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                result
            } else {
                uiImage
            }

            if (scaledImage != null) {
                val data = UIImageJPEGRepresentation(scaledImage, quality.toDouble() / 100.0)
                if (data != null) {
                    Result.Success(data.toByteArray())
                } else {
                    Result.Failure(MediaError.ProcessingFailed)
                }
            } else {
                Result.Failure(MediaError.ProcessingFailed)
            }
        } catch (e: Exception) {
            Result.Failure(MediaError.Unknown(e))
        }
    }

    override suspend fun generateThumbnail(
        imageData: ByteArray,
        maxDimension: Int,
        quality: Int
    ): Result<ByteArray, MediaError> = downsizeImage(imageData, maxDimension, maxDimension, quality)

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned {
        NSData.dataWithBytes(it.addressOf(0), size.convert())
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun NSData.toByteArray(): ByteArray = bytes?.let {
        it.readBytes(length.convert())
    } ?: ByteArray(0)

    private fun ByteArray.toUIImage(): UIImage? = toNSData().let { UIImage.imageWithData(it) }
}
