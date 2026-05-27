package me.ashishekka.echo.shared.data.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import okio.buffer
import okio.use

/**
 * Interface for parsing and validating the initial seed data from a bundled asset.
 */
interface BackupParser {
    /**
     * Reads and parses the seed data from the specified [fileName].
     * Uses memory-efficient streaming.
     */
    fun parseSeedData(fileName: String): SeedDataDto?

    /**
     * Validates the integrity of the [SeedDataDto].
     */
    fun validateSeedData(data: SeedDataDto): Boolean
}

/**
 * Default implementation of [BackupParser] using [LocalAssetManager] and kotlinx-serialization-okio.
 */
class DefaultBackupParser(
    private val localAssetManager: LocalAssetManager,
    private val json: Json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
) : BackupParser {
    
    @OptIn(ExperimentalSerializationApi::class)
    override fun parseSeedData(fileName: String): SeedDataDto? {
        val source = localAssetManager.bundledAssetSource(fileName) ?: return null
        return try {
            source.buffer().use { bufferedSource ->
                // Use true streaming decoding from BufferedSource
                val data = json.decodeFromBufferedSource<SeedDataDto>(bufferedSource)
                if (validateSeedData(data)) data else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun validateSeedData(data: SeedDataDto): Boolean {
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

        // 4. Physical Asset Validation: Ensure all bundled files actually exist
        val filesValid = data.messages.values.flatten().all { message ->
            val file = message.file ?: return@all true
            val mainFileExists = file.bundledAssetName?.let { localAssetManager.bundledAssetSource(it) != null } ?: true
            val thumbExists = file.thumbnail?.bundledAssetName?.let { localAssetManager.bundledAssetSource(it) != null } ?: true
            mainFileExists && thumbExists
        }

        return chatsValid && messagesValid && messageKeysValid && filesValid
    }
}
