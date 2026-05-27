package me.ashishekka.echo.shared.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.withContext
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.MediaError
import me.ashishekka.echo.shared.domain.Result
import java.io.ByteArrayOutputStream

class AndroidMediaProcessor(
    private val dispatcherProvider: DispatcherProvider
) : MediaProcessor {

    override suspend fun downsizeImage(
        imageData: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): Result<ByteArray, MediaError> = withContext(dispatcherProvider.default) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext Result.Failure(MediaError.InvalidData)
            }

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false

            val decodedBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
                ?: return@withContext Result.Failure(MediaError.ProcessingFailed)

            val scaledBitmap = if (decodedBitmap.width > maxWidth || decodedBitmap.height > maxHeight) {
                val ratio = Math.min(
                    maxWidth.toFloat() / decodedBitmap.width,
                    maxHeight.toFloat() / decodedBitmap.height
                )
                val targetWidth = (decodedBitmap.width * ratio).toInt()
                val targetHeight = (decodedBitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
            } else {
                decodedBitmap
            }

            val outputStream = ByteArrayOutputStream()
            val success = scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            
            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()

            if (success) {
                Result.Success(outputStream.toByteArray())
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

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
