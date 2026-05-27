package me.ashishekka.echo.shared.data.file

import android.content.Context
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.Result
import okio.Source
import okio.source

/**
 * Android-specific implementation of [AssetReader] using the [android.content.res.AssetManager].
 */
class AndroidAssetReader(private val context: Context) : AssetReader {
    override fun readAsset(fileName: String): Result<String, AssetError> {
        return try {
            val content = context.assets.open(fileName).bufferedReader().use { it.readText() }
            Result.Success(content)
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }

    override fun readAssetBytes(fileName: String): Result<ByteArray, AssetError> {
        return try {
            val bytes = context.assets.open(fileName).use { it.readBytes() }
            Result.Success(bytes)
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }

    override fun readAssetSource(fileName: String): Result<Source, AssetError> {
        return try {
            val source = context.assets.open(fileName).source()
            Result.Success(source)
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }
}
