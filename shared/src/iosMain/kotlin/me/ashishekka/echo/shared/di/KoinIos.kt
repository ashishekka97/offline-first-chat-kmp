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
import me.ashishekka.echo.shared.data.file.AssetReader
import me.ashishekka.echo.shared.data.file.DefaultLocalAssetManager
import me.ashishekka.echo.shared.data.file.IosAssetReader
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.data.media.IosMediaProcessor
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.util.IosLogger
import me.ashishekka.echo.shared.util.IosStringProvider
import me.ashishekka.echo.shared.util.Logger
import me.ashishekka.echo.shared.util.StringProvider
import okio.FileSystem
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

    single<AssetReader> { IosAssetReader() }

    single<Logger> { IosLogger() }
    
    single<StringProvider> { IosStringProvider() }

    single<LocalAssetManager> {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        DefaultLocalAssetManager(
            baseDirPath = documentDirectory?.path ?: "",
            assetReader = get(),
            fileSystem = FileSystem.SYSTEM
        )
    }

    single<MediaProcessor> { IosMediaProcessor(get()) }
}
