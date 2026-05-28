package me.ashishekka.echo.shared.data.file

import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.Result
import okio.FileSystem
import okio.Source

class FakeLocalAssetManager : LocalAssetManager {
    val storedFiles = mutableMapOf<String, ByteArray>()
    
    override fun readText(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
    
    override fun writeText(fileName: String, content: String): Result<Unit, AssetError> {
        storedFiles[fileName] = content.encodeToByteArray()
        return Result.Success(Unit)
    }
    
    override fun readBytes(fileName: String): Result<ByteArray, AssetError> {
        val bytes = storedFiles[fileName]
        return if (bytes != null) Result.Success(bytes) else Result.Failure(AssetError.NotFound)
    }
    
    override fun readUriBytes(uriPath: String): Result<ByteArray, AssetError> = readBytes(uriPath)
    
    override fun writeBytes(fileName: String, bytes: ByteArray): Result<Unit, AssetError> {
        storedFiles[fileName] = bytes
        return Result.Success(Unit)
    }
    
    override fun deleteFile(fileName: String): Result<Unit, AssetError> {
        storedFiles.remove(fileName)
        return Result.Success(Unit)
    }
    
    override fun getAbsolutePath(fileName: String): String = "/local/$fileName"
    
    override fun exists(fileName: String): Boolean = storedFiles.containsKey(fileName)
    
    override fun readBundledAsset(fileName: String): Result<String, AssetError> = Result.Failure(AssetError.NotFound)
    
    override fun readBundledAssetBytes(fileName: String): Result<ByteArray, AssetError> = Result.Failure(AssetError.NotFound)
    
    override fun bundledAssetSource(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
    
    override suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError> {
        storedFiles[fileName] = byteArrayOf(0) // Simulate copy
        return Result.Success(Unit)
    }
    
    override fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError> = Result.Failure(AssetError.NotFound)
    
    override fun source(fileName: String): Result<Source, AssetError> = Result.Failure(AssetError.NotFound)
}
