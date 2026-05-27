package me.ashishekka.echo.shared.data.file

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.Result
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
    override fun readAsset(fileName: String): Result<String, AssetError> {
        val nameParts = fileName.split(".")
        val name = nameParts.getOrNull(0) ?: return Result.Failure(AssetError.NotFound)
        val extension = nameParts.getOrNull(1)
        val path = NSBundle.mainBundle.pathForResource(name, extension) ?: return Result.Failure(AssetError.NotFound)
        val content = platform.Foundation.NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        return if (content != null) {
            Result.Success(content)
        } else {
            Result.Failure(AssetError.Unreadable)
        }
    }

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    override fun readAssetBytes(fileName: String): Result<ByteArray, AssetError> {
        val nameParts = fileName.split(".")
        val name = nameParts.getOrNull(0) ?: return Result.Failure(AssetError.NotFound)
        val extension = nameParts.getOrNull(1)
        val path = NSBundle.mainBundle.pathForResource(name, extension) ?: return Result.Failure(AssetError.NotFound)
        val data = NSData.dataWithContentsOfFile(path) ?: return Result.Failure(AssetError.Unreadable)
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return Result.Success(bytes)
    }

    override fun readAssetSource(fileName: String): Result<Source, AssetError> {
        val nameParts = fileName.split(".")
        val name = nameParts.getOrNull(0) ?: return Result.Failure(AssetError.NotFound)
        val extension = nameParts.getOrNull(1)
        val path = NSBundle.mainBundle.pathForResource(name, extension) ?: return Result.Failure(AssetError.NotFound)
        return try {
            Result.Success(FileSystem.SYSTEM.source(path.toPath()))
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }
}
