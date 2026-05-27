package me.ashishekka.echo.shared.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import me.ashishekka.echo.shared.data.dao.ChatDao
import me.ashishekka.echo.shared.data.dao.MessageDao
import me.ashishekka.echo.shared.data.dao.ParticipantDao
import me.ashishekka.echo.shared.data.dao.RestorationDao
import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.ParticipantEntity

/**
 * The main Room database for the Echo Chat App.
 *
 * This database handles persistence for chats, messages, and participants.
 * It is a multiplatform Room database using the SQLite driver.
 */
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        ParticipantEntity::class,
        ChatParticipantCrossRef::class
    ],
    version = 2,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /** Provides access to chat-related database operations. */
    abstract fun chatDao(): ChatDao

    /** Provides access to message-related database operations. */
    abstract fun messageDao(): MessageDao

    /** Provides access to participant-related database operations. */
    abstract fun participantDao(): ParticipantDao

    /** Provides access to specialized restoration operations. */
    abstract fun restorationDao(): RestorationDao
}

/**
 * A Room-required constructor object for generating the database implementation
 * on non-Android platforms (like iOS).
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
