package me.ashishekka.echo.shared.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.withContext
import me.ashishekka.echo.shared.di.DispatcherProvider
import java.io.ByteArrayOutputStream

class AndroidMediaProcessor(
    private val dispatcherProvider: DispatcherProvider
) : MediaProcessor {

    override suspend fun downsizeImage(
        imageData: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int
    ): ByteArray? = withContext(dispatcherProvider.default) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)

            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false

            val decodedBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
                ?: return@withContext null

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
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            
            if (scaledBitmap != decodedBitmap) {
                scaledBitmap.recycle()
            }
            decodedBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun generateThumbnail(
        imageData: ByteArray,
        maxDimension: Int,
        quality: Int
    ): ByteArray? = downsizeImage(imageData, maxDimension, maxDimension, quality)

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
