package me.ashishekka.echo.shared.data.file

import android.content.Context

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
}
