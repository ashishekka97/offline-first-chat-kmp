package me.ashishekka.echo.shared.data.file

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Interface for managing local application assets and internal storage files.
 * Provides a unified API for reading and writing files across platforms.
 */
interface LocalAssetManager {
    /** Reads the text content of a file from internal storage. */
    fun readText(fileName: String): String?

    /** Writes text content to a file in internal storage, overwriting any existing content. */
    fun writeText(fileName: String, content: String)

    /** Reads the raw bytes of a file from internal storage. */
    fun readBytes(fileName: String): ByteArray?

    /** Writes raw bytes to a file in internal storage, overwriting any existing content. */
    fun writeBytes(fileName: String, bytes: ByteArray)

    /** Deletes a file from internal storage. Returns true if successful. */
    fun deleteFile(fileName: String): Boolean

    /** Returns the absolute system path for a given file name in internal storage. */
    fun getAbsolutePath(fileName: String): String

    /** Checks if a file exists in internal storage. */
    fun exists(fileName: String): Boolean

    /** Reads the text content of an asset bundled within the application (e.g., APK assets or iOS Bundle). */
    fun readBundledAsset(fileName: String): String?
}

/**
 * Platform-agnostic interface for reading bundled application assets.
 */
interface AssetReader {
    /** Reads the content of a bundled asset as a String. */
    fun readAsset(fileName: String): String?
}

/**
 * Default implementation of [LocalAssetManager] using Okio for file operations.
 *
 * @property baseDirPath The root directory path for internal storage.
 * @property assetReader The platform-specific reader for bundled assets.
 * @property fileSystem The Okio [FileSystem] to use (defaults to SYSTEM).
 */
class DefaultLocalAssetManager(
    private val baseDirPath: String,
    private val assetReader: AssetReader,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
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
}
