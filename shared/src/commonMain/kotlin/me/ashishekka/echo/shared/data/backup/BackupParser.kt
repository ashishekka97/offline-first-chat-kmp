package me.ashishekka.echo.shared.data.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Interface for parsing and validating the initial seed data from a bundled asset.
 */
interface BackupParser {
    /**
     * Reads and parses the seed data from the specified [jsonFileName] within the [fileSystem].
     * Uses memory-efficient streaming.
     */
    fun parseSeedData(fileSystem: FileSystem, jsonFileName: String = "data.json"): SeedDataDto?

    /**
     * Validates the integrity of the [SeedDataDto] and ensures referenced media exists in [fileSystem].
     */
    fun validateSeedData(data: SeedDataDto, fileSystem: FileSystem): Boolean
}

/**
 * Default implementation of [BackupParser] using kotlinx-serialization-okio.
 */
class DefaultBackupParser(
    private val json: Json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
) : BackupParser {
    
    @OptIn(ExperimentalSerializationApi::class)
    override fun parseSeedData(fileSystem: FileSystem, jsonFileName: String): SeedDataDto? {
        return try {
            fileSystem.source(jsonFileName.toPath()).buffer().use { bufferedSource ->
                // Use true streaming decoding from BufferedSource
                val data = json.decodeFromBufferedSource<SeedDataDto>(bufferedSource)
                if (validateSeedData(data, fileSystem)) data else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun validateSeedData(data: SeedDataDto, fileSystem: FileSystem): Boolean {
        val participantIds = data.participants.map { it.id }.toSet()

        // 1. Validate that all participants in chats exist
        val chatsValid = data.chats.all { chat ->
            chat.participantIds.all { it in participantIds }
        }

        // 2. Validate that all message senders exist
        val messagesValid = data.messages.values.flatten().all { message ->
            message.sender in participantIds
        }

        // 3. Validate that every chat in 'messages' map exists in 'chats' list
        val chatIds = data.chats.map { it.id }.toSet()
        val messageKeysValid = data.messages.keys.all { it in chatIds }

        // 4. Physical Asset Validation: Ensure all bundled files actually exist in the provided FileSystem
        val filesValid = data.messages.values.flatten().all { message ->
            val file = message.file ?: return@all true
            val mainFileExists = file.bundledAssetName?.let { fileSystem.exists(it.toPath()) } ?: true
            val thumbExists = file.thumbnail?.bundledAssetName?.let { fileSystem.exists(it.toPath()) } ?: true
            mainFileExists && thumbExists
        }

        return chatsValid && messagesValid && messageKeysValid && filesValid
    }
}
