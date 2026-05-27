package me.ashishekka.echo.shared.data

import androidx.room.RoomDatabase

expect fun getTestDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>
