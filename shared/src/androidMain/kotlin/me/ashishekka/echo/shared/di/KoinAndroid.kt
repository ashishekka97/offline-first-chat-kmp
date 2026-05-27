package me.ashishekka.echo.shared.di

import androidx.room.Room
import androidx.room.RoomDatabase
import me.ashishekka.echo.shared.data.AppDatabase
import me.ashishekka.echo.shared.data.DatabaseConstants
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
}
