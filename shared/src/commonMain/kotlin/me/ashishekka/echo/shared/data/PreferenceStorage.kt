package me.ashishekka.echo.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.ashishekka.echo.shared.domain.PreferenceError
import me.ashishekka.echo.shared.domain.Result
import okio.IOException

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.ashishekka.echo.shared.domain.model.ChatId

/**
 * Interface for accessing application preferences.
 */
interface PreferenceStorage {
    /** Observable flow of the initial data restoration status. */
    val isRestoreCompleted: Flow<Boolean>

    /** Updates the initial data restoration status. */
    suspend fun setRestoreCompleted(completed: Boolean): Result<Unit, PreferenceError>

    /** Observable flow of message drafts for all chats. */
    val drafts: Flow<Map<ChatId, String>>

    /** Updates the draft for a specific chat. */
    suspend fun saveDraft(chatId: ChatId, text: String): Result<Unit, PreferenceError>

    /** Clears the draft for a specific chat. */
    suspend fun clearDraft(chatId: ChatId): Result<Unit, PreferenceError>
}

/**
 * [PreferenceStorage] implementation using Jetpack DataStore.
 */
class DataStorePreferenceStorage(
    private val dataStore: DataStore<Preferences>
) : PreferenceStorage {

    override val isRestoreCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_RESTORE_COMPLETED] ?: false
        }

    override suspend fun setRestoreCompleted(completed: Boolean): Result<Unit, PreferenceError> {
        return try {
            dataStore.edit { preferences ->
                preferences[IS_RESTORE_COMPLETED] = completed
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(PreferenceError.Unknown(e))
        }
    }

    override val drafts: Flow<Map<ChatId, String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val json = preferences[MESSAGE_DRAFTS] ?: return@map emptyMap()
            try {
                Json.decodeFromString<Map<String, String>>(json).mapKeys { ChatId(it.key) }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    override suspend fun saveDraft(chatId: ChatId, text: String): Result<Unit, PreferenceError> {
        return try {
            dataStore.edit { preferences ->
                val currentJson = preferences[MESSAGE_DRAFTS]
                val currentMap = if (currentJson != null) {
                    Json.decodeFromString<Map<String, String>>(currentJson).toMutableMap()
                } else {
                    mutableMapOf()
                }
                currentMap[chatId.value] = text
                preferences[MESSAGE_DRAFTS] = Json.encodeToString(currentMap)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(PreferenceError.Unknown(e))
        }
    }

    override suspend fun clearDraft(chatId: ChatId): Result<Unit, PreferenceError> {
        return try {
            dataStore.edit { preferences ->
                val currentJson = preferences[MESSAGE_DRAFTS] ?: return@edit
                val currentMap = Json.decodeFromString<Map<String, String>>(currentJson).toMutableMap()
                currentMap.remove(chatId.value)
                preferences[MESSAGE_DRAFTS] = Json.encodeToString(currentMap)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(PreferenceError.Unknown(e))
        }
    }

    companion object {
        private val IS_RESTORE_COMPLETED = booleanPreferencesKey("is_restore_completed")
        private val MESSAGE_DRAFTS = stringPreferencesKey("message_drafts")
    }
}
