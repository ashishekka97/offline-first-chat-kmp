package me.ashishekka.echo.shared.data.file

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS-specific implementation of [AssetReader] using the [NSBundle.mainBundle].
 */
@OptIn(ExperimentalForeignApi::class)
class IosAssetReader : AssetReader {
    override fun readAsset(fileName: String): String? {
        val nameParts = fileName.split(".")
        if (nameParts.size < 2) return null
        
        val resource = nameParts[0]
        val type = nameParts[1]
        
        val path = NSBundle.mainBundle.pathForResource(resource, type) ?: return null
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
    }
}
