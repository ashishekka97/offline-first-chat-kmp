package me.ashishekka.echo.shared.di

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.ashishekka.echo.shared.data.AppDatabase
import me.ashishekka.echo.shared.data.AppDatabaseConstructor
import me.ashishekka.echo.shared.data.DATA_STORE_FILE_NAME
import me.ashishekka.echo.shared.data.DatabaseConstants
import me.ashishekka.echo.shared.data.createDataStore
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getPlatformDataModule(): Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        val dbFilePath = documentDirectory?.path + "/" + DatabaseConstants.DB_NAME
        Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
            factory = { AppDatabaseConstructor.initialize() }
        )
    }

    single {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        val path = documentDirectory?.path + "/" + DATA_STORE_FILE_NAME
        val dispatcherProvider = get<DispatcherProvider>()
        createDataStore(
            coroutineScope = CoroutineScope(dispatcherProvider.io + SupervisorJob()),
            producePath = { path }
        )
    }
}
