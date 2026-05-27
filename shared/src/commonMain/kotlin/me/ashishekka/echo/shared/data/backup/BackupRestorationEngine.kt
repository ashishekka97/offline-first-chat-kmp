package me.ashishekka.echo.shared.data.backup

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.PreferenceStorage
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.Result

/**
 * Result of the backup restoration process.
 */
sealed class RestorationResult {
    object Success : RestorationResult()
    object AlreadyCompleted : RestorationResult()
    data class Failure(val message: String, val throwable: Throwable? = null) : RestorationResult()
}

/**
 * Engine responsible for orchestrating the entire backup restoration process.
 */
interface BackupRestorationEngine {
    /**
     * Restores seed data from a bundled ZIP file.
     * @param zipFileName The name of the ZIP file in bundled assets.
     * @return [RestorationResult] indicating the outcome of the operation.
     */
    suspend fun restore(zipFileName: String = "seed_backup.zip"): RestorationResult
}

/**
 * Default implementation of [BackupRestorationEngine].
 */
class DefaultBackupRestorationEngine(
    private val backupParser: BackupParser,
    private val mediaRestorationService: MediaRestorationService,
    private val seedDataRepository: SeedDataRepository,
    private val localAssetManager: LocalAssetManager,
    private val preferenceStorage: PreferenceStorage,
    private val dispatcherProvider: DispatcherProvider,
    private val clock: Clock = Clock.System
) : BackupRestorationEngine {

    override suspend fun restore(zipFileName: String): RestorationResult = withContext(dispatcherProvider.io) {
        // 1. Check if already completed
        if (preferenceStorage.isRestoreCompleted.first()) {
            return@withContext RestorationResult.AlreadyCompleted
        }

        try {
            // 2. Hardening: Clear any existing partial data before starting
            val clearResult = seedDataRepository.clearExistingData()
            if (clearResult is Result.Failure) {
                return@withContext RestorationResult.Failure("Failed to clear existing data: ${clearResult.error}")
            }

            // 3. Copy ZIP from assets to local storage
            val copyResult = localAssetManager.copyBundledAssetToLocal(zipFileName)
            if (copyResult is Result.Failure) {
                return@withContext RestorationResult.Failure("Failed to copy bundled asset: $zipFileName, error: ${copyResult.error}")
            }

            // 4. Mount ZIP FileSystem
            val zipFs = when (val zipFsResult = localAssetManager.getZipFileSystem(zipFileName)) {
                is Result.Failure -> return@withContext RestorationResult.Failure("Failed to open ZIP filesystem: $zipFileName, error: ${zipFsResult.error}")
                is Result.Success -> zipFsResult.data
            }
            
            // 5. Parse Seed Data
            val seedDataDto = backupParser.parseSeedData(zipFs) 
                ?: return@withContext RestorationResult.Failure("Failed to parse or validate seed data from ZIP")

            // 6. Map DTOs to Entities (using relative timestamps)
            val baseTime = clock.now().toEpochMilliseconds()
            
            val participants = seedDataDto.participants.map { it.toEntity() }
            val chats = seedDataDto.chats.map { it.toEntity(baseTime) }
            val crossRefs = seedDataDto.chats.flatMap { it.toCrossRefs() }
            
            val allMessages = seedDataDto.messages.flatMap { (chatId, messages) ->
                messages.map { it.toEntity(chatId, baseTime) }
            }

            // 7. Process Media
            val processedMessages = mediaRestorationService.processMedia(allMessages, zipFs)

            // 8. Persist to Database
            val saveResult = seedDataRepository.saveSeedData(
                participants = participants,
                chats = chats,
                chatCrossRefs = crossRefs,
                messages = processedMessages
            )
            if (saveResult is Result.Failure) {
                return@withContext RestorationResult.Failure("Failed to save seed data: ${saveResult.error}")
            }

            // 9. Mark as completed
            preferenceStorage.setRestoreCompleted(true)
            RestorationResult.Success
        } catch (e: Exception) {
            RestorationResult.Failure("Restoration failed due to unexpected error: ${e.message}", e)
        }
    }
}
