package me.ashishekka.echo.shared.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.di.DefaultDispatcherProvider
import me.ashishekka.echo.shared.domain.Result
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AndroidMediaProcessorTest {

    private val processor = AndroidMediaProcessor(DefaultDispatcherProvider())

    @Test
    fun testDownsizeImage() = runTest {
        // 1. Create a large dummy bitmap (2000x2000)
        val originalWidth = 2000
        val originalHeight = 2000
        val bitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val originalBytes = stream.toByteArray()

        // 2. Downsize to 1024x1024
        val maxWidth = 1024
        val maxHeight = 1024
        val result = processor.downsizeImage(originalBytes, maxWidth, maxHeight)

        // 3. Verify
        assertTrue(result is Result.Success)
        val downsizedBytes = result.data
        val downsizedBitmap = BitmapFactory.decodeByteArray(downsizedBytes, 0, downsizedBytes.size)
        assertNotNull(downsizedBitmap)
        
        assertTrue(downsizedBitmap.width <= maxWidth)
        assertTrue(downsizedBitmap.height <= maxHeight)
        // Ensure aspect ratio is maintained (should be 1:1 since original was 2000x2000)
        assertTrue(downsizedBitmap.width == downsizedBitmap.height)
    }

    @Test
    fun testGenerateThumbnail() = runTest {
        val originalWidth = 1000
        val originalHeight = 500
        val bitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val originalBytes = stream.toByteArray()

        val maxDimension = 256
        val result = processor.generateThumbnail(originalBytes, maxDimension)

        assertTrue(result is Result.Success)
        val thumbBytes = result.data
        val thumbBitmap = BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size)
        assertNotNull(thumbBitmap)

        assertTrue(thumbBitmap.width <= maxDimension)
        assertTrue(thumbBitmap.height <= maxDimension)
        // Maintain aspect ratio 2:1
        assertTrue(thumbBitmap.width == maxDimension)
        assertTrue(thumbBitmap.height == maxDimension / 2)
    }

    @Test
    fun testSmallImageNotUpscaled() = runTest {
        val originalWidth = 100
        val originalHeight = 100
        val bitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val originalBytes = stream.toByteArray()

        val maxWidth = 1024
        val maxHeight = 1024
        val result = processor.downsizeImage(originalBytes, maxWidth, maxHeight)

        assertTrue(result is Result.Success)
        val processedBytes = result.data
        val processedBitmap = BitmapFactory.decodeByteArray(processedBytes, 0, processedBytes.size)
        assertEquals(originalWidth, processedBitmap.width)
        assertEquals(originalHeight, processedBitmap.height)
    }

    @Test
    fun testExtremeAspectRatio() = runTest {
        // Very wide image: 2000x100
        val originalWidth = 2000
        val originalHeight = 100
        val bitmap = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val originalBytes = stream.toByteArray()

        val maxDimension = 500
        val result = processor.downsizeImage(originalBytes, maxDimension, maxDimension)

        assertTrue(result is Result.Success)
        val processedBytes = result.data
        val processedBitmap = BitmapFactory.decodeByteArray(processedBytes, 0, processedBytes.size)
        
        // Should be scaled down to 500 width, and height should follow aspect ratio (500 / 20 = 25)
        assertEquals(500, processedBitmap.width)
        assertEquals(25, processedBitmap.height)
    }

    @Test
    fun testInvalidImageData() = runTest {
        val invalidBytes = byteArrayOf(1, 2, 3, 4, 5)
        val result = processor.downsizeImage(invalidBytes)
        assertTrue(result is Result.Failure)
    }

    private fun assertEquals(expected: Int, actual: Int) {
        kotlin.test.assertEquals(expected, actual)
    }
}
