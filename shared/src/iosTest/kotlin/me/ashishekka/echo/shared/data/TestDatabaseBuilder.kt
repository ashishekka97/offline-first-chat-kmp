package me.ashishekka.echo.shared.data

import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.inMemoryDatabaseBuilder<AppDatabase>()
}
