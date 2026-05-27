package me.ashishekka.echo.shared.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/**
 * Creates and builds the [AppDatabase] using the provided [builder] and [queryContext].
 *
 * @param builder The platform-specific Room database builder.
 * @param queryContext The [CoroutineContext] to be used for database queries (e.g., Dispatchers.IO).
 * @return A fully initialized [AppDatabase] instance.
 */
fun createDatabase(
    builder: RoomDatabase.Builder<AppDatabase>,
    queryContext: CoroutineContext
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .build()
}
