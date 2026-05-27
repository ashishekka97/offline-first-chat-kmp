package me.ashishekka.echo.shared.data.media

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.test.runTest
import me.ashishekka.echo.shared.di.DefaultDispatcherProvider
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosMediaProcessorTest {

    private val processor = IosMediaProcessor(DefaultDispatcherProvider())

    @Test
    fun testDownsizeImage() = runTest {
        // 1. Create a large dummy image (2000x2000)
        val originalSize = 2000.0
        val originalBytes = createDummyImageBytes(originalSize, originalSize)

        // 2. Downsize to 1024x1024
        val maxWidth = 1024
        val maxHeight = 1024
        val downsizedBytes = processor.downsizeImage(originalBytes, maxWidth, maxHeight)

        // 3. Verify
        assertNotNull(downsizedBytes)
        val downsizedImage = downsizedBytes.toUIImage()
        assertNotNull(downsizedImage)
        
        val width = downsizedImage.size.useContents { width }
        val height = downsizedImage.size.useContents { height }
        
        assertTrue(width <= maxWidth.toDouble())
        assertTrue(height <= maxHeight.toDouble())
        assertEquals(width, height)
    }

    @Test
    fun testGenerateThumbnail() = runTest {
        val originalWidth = 1000.0
        val originalHeight = 500.0
        val originalBytes = createDummyImageBytes(originalWidth, originalHeight)

        val maxDimension = 256
        val thumbBytes = processor.generateThumbnail(originalBytes, maxDimension)

        assertNotNull(thumbBytes)
        val thumbImage = thumbBytes.toUIImage()
        assertNotNull(thumbImage)

        val width = thumbImage.size.useContents { width }
        val height = thumbImage.size.useContents { height }

        assertTrue(width <= maxDimension.toDouble())
        assertTrue(height <= maxDimension.toDouble())
        // Maintain aspect ratio 2:1
        assertEquals(maxDimension.toDouble(), width)
        assertEquals((maxDimension / 2).toDouble(), height)
    }

    @Test
    fun testSmallImageNotUpscaled() = runTest {
        val originalSize = 100.0
        val originalBytes = createDummyImageBytes(originalSize, originalSize)

        val maxWidth = 1024
        val maxHeight = 1024
        val processedBytes = processor.downsizeImage(originalBytes, maxWidth, maxHeight)

        assertNotNull(processedBytes)
        val processedImage = processedBytes.toUIImage()
        assertNotNull(processedImage)
        
        val width = processedImage.size.useContents { width }
        val height = processedImage.size.useContents { height }
        
        assertEquals(originalSize, width)
        assertEquals(originalSize, height)
    }

    @Test
    fun testInvalidImageData() = runTest {
        val invalidBytes = byteArrayOf(1, 2, 3, 4, 5)
        val result = processor.downsizeImage(invalidBytes)
        assertNull(result)
    }

    private fun createDummyImageBytes(width: Double, height: Double): ByteArray {
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), false, 1.0)
        val context = platform.UIKit.UIGraphicsGetCurrentContext()
        platform.CoreGraphics.CGContextSetFillColorWithColor(context, platform.UIKit.UIColor.redColor.CGColor)
        platform.CoreGraphics.CGContextFillRect(context, CGRectMake(0.0, 0.0, width, height))
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return UIImageJPEGRepresentation(image!!, 1.0)!!.toByteArray()
    }

    @OptIn(BetaInteropApi::class)
    private fun NSData.toByteArray(): ByteArray = bytes?.let {
        it.readBytes(length.convert())
    } ?: ByteArray(0)

    @OptIn(BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned {
        NSData.dataWithBytes(it.addressOf(0), size.convert())
    }

    private fun ByteArray.toUIImage(): UIImage? = toNSData().let { UIImage.imageWithData(it) }
}
