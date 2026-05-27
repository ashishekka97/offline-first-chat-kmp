package me.ashishekka.echo.shared.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath

/**
 * Creates a [DataStore] instance with the provided [producePath] lambda.
 *
 * @param coroutineScope The scope to be used for DataStore operations.
 * @param producePath A lambda that returns the absolute path to the DataStore file.
 * @return A [DataStore<Preferences>] instance.
 */
fun createDataStore(
    coroutineScope: CoroutineScope,
    producePath: () -> String
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        scope = coroutineScope,
        produceFile = { producePath().toPath() }
    )

internal const val DATA_STORE_FILE_NAME = "echo.preferences_pb"
