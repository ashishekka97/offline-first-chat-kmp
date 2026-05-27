package me.ashishekka.echo.shared.data.file

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Source
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS-specific implementation of [AssetReader] using the [NSBundle.mainBundle].
 */
class IosAssetReader : AssetReader {
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    override fun readAsset(fileName: String): String? {
        val nameParts = fileName.split(".")
        val name = nameParts.getOrNull(0) ?: return null
        val extension = nameParts.getOrNull(1)
        val path = NSBundle.mainBundle.pathForResource(name, extension) ?: return null
        return platform.Foundation.NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    override fun readAssetBytes(fileName: String): ByteArray? {
        val nameParts = fileName.split(".")
        val name = nameParts.getOrNull(0) ?: return null
        val extension = nameParts.getOrNull(1)
        val path = NSBundle.mainBundle.pathForResource(name, extension) ?: return null
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }

    override fun readAssetSource(fileName: String): Source? {
        val nameParts = fileName.split(".")
        val name = nameParts.getOrNull(0) ?: return null
        val extension = nameParts.getOrNull(1)
        val path = NSBundle.mainBundle.pathForResource(name, extension) ?: return null
        return try {
            FileSystem.SYSTEM.source(path.toPath())
        } catch (e: Exception) {
            null
        }
    }
}
