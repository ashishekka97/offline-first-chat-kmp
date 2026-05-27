package me.ashishekka.echo.shared.data.file

import android.content.Context
import okio.Source
import okio.source

/**
 * Android-specific implementation of [AssetReader] using the [android.content.res.AssetManager].
 */
class AndroidAssetReader(private val context: Context) : AssetReader {
    override fun readAsset(fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }

    override fun readAssetBytes(fileName: String): ByteArray? {
        return try {
            context.assets.open(fileName).use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    override fun readAssetSource(fileName: String): Source? {
        return try {
            context.assets.open(fileName).source()
        } catch (e: Exception) {
            null
        }
    }
}
