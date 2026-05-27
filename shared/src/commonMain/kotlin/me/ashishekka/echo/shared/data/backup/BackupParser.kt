package me.ashishekka.echo.shared.data.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import me.ashishekka.echo.shared.domain.BackupError
import me.ashishekka.echo.shared.domain.Result
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
    fun parseSeedData(fileSystem: FileSystem, jsonFileName: String = "data.json"): Result<SeedDataDto, BackupError>

    /**
     * Validates the integrity of the [SeedDataDto] and ensures referenced media exists in [fileSystem].
     */
    fun validateSeedData(data: SeedDataDto, fileSystem: FileSystem): Result<Unit, BackupError>
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
    override fun parseSeedData(fileSystem: FileSystem, jsonFileName: String): Result<SeedDataDto, BackupError> {
        return try {
            val source = try {
                fileSystem.source(jsonFileName.toPath())
            } catch (e: Exception) {
                return Result.Failure(BackupError.FileOpenFailure)
            }

            source.buffer().use { bufferedSource ->
                val data = try {
                    json.decodeFromBufferedSource<SeedDataDto>(bufferedSource)
                } catch (e: Exception) {
                    return Result.Failure(BackupError.InvalidJson(e))
                }

                when (val validationResult = validateSeedData(data, fileSystem)) {
                    is Result.Failure -> validationResult
                    is Result.Success -> Result.Success(data)
                }
            }
        } catch (e: Exception) {
            Result.Failure(BackupError.Unknown(e))
        }
    }

    override fun validateSeedData(data: SeedDataDto, fileSystem: FileSystem): Result<Unit, BackupError> {
        val participantIds = data.participants.map { it.id }.toSet()

        // 1. Validate that all participants in chats exist
        val chatsValid = data.chats.all { chat ->
            chat.participantIds.all { it in participantIds }
        }
        if (!chatsValid) return Result.Failure(BackupError.IntegrityCheckFailed)

        // 2. Validate that all message senders exist
        val messagesValid = data.messages.values.flatten().all { message ->
            message.sender in participantIds
        }
        if (!messagesValid) return Result.Failure(BackupError.IntegrityCheckFailed)

        // 3. Validate that every chat in 'messages' map exists in 'chats' list
        val chatIds = data.chats.map { it.id }.toSet()
        val messageKeysValid = data.messages.keys.all { it in chatIds }
        if (!messageKeysValid) return Result.Failure(BackupError.IntegrityCheckFailed)

        // 4. Physical Asset Validation: Ensure all bundled files actually exist in the provided FileSystem
        val filesValid = data.messages.values.flatten().all { message ->
            val file = message.file ?: return@all true
            val mainFileExists = file.bundledAssetName?.let { fileSystem.exists(it.toPath()) } ?: true
            val thumbExists = file.thumbnail?.bundledAssetName?.let { fileSystem.exists(it.toPath()) } ?: true
            mainFileExists && thumbExists
        }
        if (!filesValid) return Result.Failure(BackupError.IntegrityCheckFailed)

        return Result.Success(Unit)
    }
}
