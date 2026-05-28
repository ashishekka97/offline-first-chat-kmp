package me.ashishekka.echo.shared.di

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import me.ashishekka.echo.shared.data.AppDatabase
import me.ashishekka.echo.shared.data.DATA_STORE_FILE_NAME
import me.ashishekka.echo.shared.data.DatabaseConstants
import me.ashishekka.echo.shared.data.createDataStore
import me.ashishekka.echo.shared.data.file.*
import me.ashishekka.echo.shared.data.media.AndroidMediaProcessor
import me.ashishekka.echo.shared.data.media.MediaProcessor
import me.ashishekka.echo.shared.util.AndroidLogger
import me.ashishekka.echo.shared.util.AndroidStringProvider
import me.ashishekka.echo.shared.util.Logger
import me.ashishekka.echo.shared.util.StringProvider
import okio.FileSystem
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformDataModule(): Module = module {
    single<RoomDatabase.Builder<AppDatabase>> {
        val appContext = androidContext().applicationContext
        val dbFile = appContext.getDatabasePath(DatabaseConstants.DB_NAME)
        Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }

    single {
        val appContext = androidContext().applicationContext
        val dispatcherProvider = get<DispatcherProvider>()
        createDataStore(
            coroutineScope = CoroutineScope(dispatcherProvider.io + SupervisorJob()),
            producePath = { appContext.preferencesDataStoreFile(DATA_STORE_FILE_NAME).absolutePath }
        )
    }

    single<AssetReader> { AndroidAssetReader(androidContext()) }

    single<Logger> { AndroidLogger() }
    
    single<UriReader> { AndroidUriReader(androidContext()) }
    
    single<StringProvider> { AndroidStringProvider(androidContext()) }

    single<LocalAssetManager> {
        DefaultLocalAssetManager(
            baseDirPath = androidContext().filesDir.absolutePath,
            assetReader = get(),
            fileSystem = FileSystem.SYSTEM,
            uriReader = get()
        )
    }

    single<MediaProcessor> { AndroidMediaProcessor(get()) }
}
