package me.ashishekka.echo.shared.data.file

import me.ashishekka.echo.shared.domain.AssetError
import me.ashishekka.echo.shared.domain.Result
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.openZip

/**
 * Interface for managing local application assets and internal storage files.
 */
interface LocalAssetManager {
    fun readText(fileName: String): Result<String, AssetError>
    fun writeText(fileName: String, content: String): Result<Unit, AssetError>
    fun readBytes(fileName: String): Result<ByteArray, AssetError>
    
    /**
     * Reads bytes from a platform-specific URI (e.g., content:// on Android).
     * If the path is not a URI, it falls back to standard file reading.
     */
    fun readUriBytes(uriPath: String): Result<ByteArray, AssetError>
    
    fun writeBytes(fileName: String, bytes: ByteArray): Result<Unit, AssetError>
    fun deleteFile(fileName: String): Result<Unit, AssetError>
    fun getAbsolutePath(fileName: String): String
    fun exists(fileName: String): Boolean
    fun readBundledAsset(fileName: String): Result<String, AssetError>
    fun readBundledAssetBytes(fileName: String): Result<ByteArray, AssetError>

    /** Returns a [Source] for a bundled asset, suitable for memory-efficient streaming. */
    fun bundledAssetSource(fileName: String): Result<Source, AssetError>

    /** Copies a bundled asset to the local application storage. */
    suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError>

    /** Returns an Okio [FileSystem] for a ZIP archive stored locally. */
    fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError>

    /** Returns a [Source] for a file in local application storage. */
    fun source(fileName: String): Result<Source, AssetError>
}

/**
 * Platform-agnostic interface for reading bundled application assets.
 */
interface AssetReader {
    fun readAsset(fileName: String): Result<String, AssetError>
    fun readAssetBytes(fileName: String): Result<ByteArray, AssetError>

    /** Returns a [Source] for a bundled asset. */
    fun readAssetSource(fileName: String): Result<Source, AssetError>
}

/**
 * Interface for reading bytes from platform-specific URIs.
 */
interface UriReader {
    fun readUriBytes(uriPath: String): Result<ByteArray, AssetError>
}

class DefaultLocalAssetManager(
    private val baseDirPath: String,
    private val assetReader: AssetReader,
    private val fileSystem: FileSystem,
    private val uriReader: UriReader? = null
) : LocalAssetManager {

    private val basePath: Path = baseDirPath.toPath()

    init {
        if (!fileSystem.exists(basePath)) {
            fileSystem.createDirectories(basePath)
        }
    }

    override fun readText(fileName: String): Result<String, AssetError> {
        val filePath = basePath.resolve(fileName)
        return try {
            if (fileSystem.exists(filePath)) {
                Result.Success(fileSystem.read(filePath) { readUtf8() })
            } else {
                Result.Failure(AssetError.NotFound)
            }
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }

    override fun writeText(fileName: String, content: String): Result<Unit, AssetError> {
        val filePath = basePath.resolve(fileName)
        return try {
            fileSystem.write(filePath) { writeUtf8(content) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AssetError.WriteFailure(e.message ?: "Unknown error"))
        }
    }

    override fun readBytes(fileName: String): Result<ByteArray, AssetError> {
        val filePath = basePath.resolve(fileName)
        return try {
            if (fileSystem.exists(filePath)) {
                Result.Success(fileSystem.read(filePath) { readByteArray() })
            } else {
                Result.Failure(AssetError.NotFound)
            }
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }

    override fun readUriBytes(uriPath: String): Result<ByteArray, AssetError> {
        return uriReader?.readUriBytes(uriPath) ?: readBytes(uriPath)
    }

    override fun writeBytes(fileName: String, bytes: ByteArray): Result<Unit, AssetError> {
        val filePath = basePath.resolve(fileName)
        return try {
            fileSystem.write(filePath) { write(bytes) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AssetError.WriteFailure(e.message ?: "Unknown error"))
        }
    }

    override fun deleteFile(fileName: String): Result<Unit, AssetError> {
        val filePath = basePath.resolve(fileName)
        return try {
            if (fileSystem.exists(filePath)) {
                fileSystem.delete(filePath)
                Result.Success(Unit)
            } else {
                Result.Failure(AssetError.NotFound)
            }
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }

    override fun getAbsolutePath(fileName: String): String {
        return basePath.resolve(fileName).toString()
    }

    override fun exists(fileName: String): Boolean {
        return fileSystem.exists(basePath.resolve(fileName))
    }

    override fun readBundledAsset(fileName: String): Result<String, AssetError> {
        return assetReader.readAsset(fileName)
    }

    override fun readBundledAssetBytes(fileName: String): Result<ByteArray, AssetError> {
        return assetReader.readAssetBytes(fileName)
    }

    override fun bundledAssetSource(fileName: String): Result<Source, AssetError> {
        return assetReader.readAssetSource(fileName)
    }

    override suspend fun copyBundledAssetToLocal(fileName: String): Result<Unit, AssetError> {
        return when (val sourceResult = bundledAssetSource(fileName)) {
            is Result.Failure -> sourceResult
            is Result.Success -> {
                val source = sourceResult.data
                val destPath = basePath.resolve(fileName)
                try {
                    fileSystem.write(destPath) {
                        writeAll(source)
                    }
                    Result.Success(Unit)
                } catch (e: Exception) {
                    Result.Failure(AssetError.WriteFailure(e.message ?: "Unknown error"))
                } finally {
                    source.close()
                }
            }
        }
    }

    override fun getZipFileSystem(fileName: String): Result<FileSystem, AssetError> {
        val filePath = basePath.resolve(fileName)
        return try {
            if (fileSystem.exists(filePath)) {
                Result.Success(fileSystem.openZip(filePath))
            } else {
                Result.Failure(AssetError.NotFound)
            }
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }

    override fun source(fileName: String): Result<Source, AssetError> {
        val filePath = basePath.resolve(fileName)
        return try {
            if (fileSystem.exists(filePath)) {
                Result.Success(fileSystem.source(filePath))
            } else {
                Result.Failure(AssetError.NotFound)
            }
        } catch (e: Exception) {
            Result.Failure(AssetError.Unknown(e))
        }
    }
}
