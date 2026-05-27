package me.ashishekka.echo.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

/**
 * Interface for accessing application preferences.
 */
interface PreferenceStorage {
    /** Observable flow of the initial data restoration status. */
    val isRestoreCompleted: Flow<Boolean>

    /** Updates the initial data restoration status. */
    suspend fun setRestoreCompleted(completed: Boolean)
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

    override suspend fun setRestoreCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_RESTORE_COMPLETED] = completed
        }
    }

    companion object {
        private val IS_RESTORE_COMPLETED = booleanPreferencesKey("is_restore_completed")
    }
}
