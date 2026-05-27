package me.ashishekka.echo.shared.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider

actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder<AppDatabase>(
        context = context,
        name = AppDatabase::class.java.canonicalName!!
    )
}
