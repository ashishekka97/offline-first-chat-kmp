package me.ashishekka.echo.shared.data.file

import android.content.Context
import android.net.Uri
import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.Result

/**
 * Android implementation of [UriReader] that can handle content:// and file:// URIs.
 */
class AndroidUriReader(private val context: Context) : UriReader {
    override fun readUriBytes(uriPath: String): Result<ByteArray, AssetError> {
        return try {
            val uri = Uri.parse(uriPath)
            if (uri.scheme == "content" || uri.scheme == "file") {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    Result.Success(inputStream.readBytes())
                } ?: Result.Failure(AssetError.NotFound)
            } else {
                // Not a handled URI, return failure so it falls back to standard reading
                Result.Failure(AssetError.NotFound)
            }
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }
}
