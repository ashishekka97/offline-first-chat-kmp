package me.ashishekka.echo.shared.data.file

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.openZip

/**
 * Interface for managing local application assets and internal storage files.
 */
interface LocalAssetManager {
    fun readText(fileName: String): String?
    fun writeText(fileName: String, content: String)
    fun readBytes(fileName: String): ByteArray?
    fun writeBytes(fileName: String, bytes: ByteArray)
    fun deleteFile(fileName: String): Boolean
    fun getAbsolutePath(fileName: String): String
    fun exists(fileName: String): Boolean
    fun readBundledAsset(fileName: String): String?
    fun readBundledAssetBytes(fileName: String): ByteArray?
    
    /** Returns a [Source] for a bundled asset, suitable for memory-efficient streaming. */
    fun bundledAssetSource(fileName: String): Source?

    /** Copies a bundled asset to the local application storage. */
    suspend fun copyBundledAssetToLocal(fileName: String): Boolean

    /** Returns an Okio [FileSystem] for a ZIP archive stored locally. */
    fun getZipFileSystem(fileName: String): FileSystem?

    /** Returns a [Source] for a file in local application storage. */
    fun source(fileName: String): Source?
}

/**
 * Platform-agnostic interface for reading bundled application assets.
 */
interface AssetReader {
    fun readAsset(fileName: String): String?
    fun readAssetBytes(fileName: String): ByteArray?
    
    /** Returns a [Source] for a bundled asset. */
    fun readAssetSource(fileName: String): Source?
}

class DefaultLocalAssetManager(
    private val baseDirPath: String,
    private val assetReader: AssetReader,
    private val fileSystem: FileSystem
) : LocalAssetManager {

    private val basePath: Path = baseDirPath.toPath()

    init {
        if (!fileSystem.exists(basePath)) {
            fileSystem.createDirectories(basePath)
        }
    }

    override fun readText(fileName: String): String? {
        val filePath = basePath.resolve(fileName)
        return if (fileSystem.exists(filePath)) {
            fileSystem.read(filePath) { readUtf8() }
        } else {
            null
        }
    }

    override fun writeText(fileName: String, content: String) {
        val filePath = basePath.resolve(fileName)
        fileSystem.write(filePath) { writeUtf8(content) }
    }

    override fun readBytes(fileName: String): ByteArray? {
        val filePath = basePath.resolve(fileName)
        return if (fileSystem.exists(filePath)) {
            fileSystem.read(filePath) { readByteArray() }
        } else {
            null
        }
    }

    override fun writeBytes(fileName: String, bytes: ByteArray) {
        val filePath = basePath.resolve(fileName)
        fileSystem.write(filePath) { write(bytes) }
    }

    override fun deleteFile(fileName: String): Boolean {
        val filePath = basePath.resolve(fileName)
        return if (fileSystem.exists(filePath)) {
            fileSystem.delete(filePath)
            true
        } else {
            false
        }
    }

    override fun getAbsolutePath(fileName: String): String {
        return basePath.resolve(fileName).toString()
    }

    override fun exists(fileName: String): Boolean {
        return fileSystem.exists(basePath.resolve(fileName))
    }

    override fun readBundledAsset(fileName: String): String? {
        return assetReader.readAsset(fileName)
    }

    override fun readBundledAssetBytes(fileName: String): ByteArray? {
        return assetReader.readAssetBytes(fileName)
    }

    override fun bundledAssetSource(fileName: String): Source? {
        return assetReader.readAssetSource(fileName)
    }

    override suspend fun copyBundledAssetToLocal(fileName: String): Boolean {
        val source = bundledAssetSource(fileName) ?: return false
        val destPath = basePath.resolve(fileName)
        return try {
            fileSystem.write(destPath) {
                writeAll(source)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            source.close()
        }
    }

    override fun getZipFileSystem(fileName: String): FileSystem? {
        val filePath = basePath.resolve(fileName)
        return if (fileSystem.exists(filePath)) {
            fileSystem.openZip(filePath)
        } else {
            null
        }
    }

    override fun source(fileName: String): Source? {
        val filePath = basePath.resolve(fileName)
        return if (fileSystem.exists(filePath)) {
            fileSystem.source(filePath)
        } else {
            null
        }
    }
}
