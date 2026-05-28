package me.ashishekka.echo.shared.data

import androidx.room.TypeConverter
import me.ashishekka.echo.shared.data.entity.MessageSenderEntity
import me.ashishekka.echo.shared.data.entity.MessageTypeEntity
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId

/**
 * Room TypeConverters for the Echo Chat App.
 *
 * This class handles the conversion between complex Kotlin types (like Enums and Value Classes)
 * and primitive types that SQLite can store.
 */
class Converters {
    /** Converts a [MessageTypeEntity] enum to its lowercase string representation for storage. */
    @TypeConverter
    fun fromMessageType(value: MessageTypeEntity): String = value.name.lowercase()

    /** Converts a stored lowercase string back into its corresponding [MessageTypeEntity] enum. */
    @TypeConverter
    fun toMessageType(value: String): MessageTypeEntity = 
        MessageTypeEntity.entries.find { it.name.lowercase() == value } ?: MessageTypeEntity.TEXT

    /** Converts a [MessageSenderEntity] enum to its lowercase string representation for storage. */
    @TypeConverter
    fun fromMessageSender(value: MessageSenderEntity): String = value.name.lowercase()

    /** Converts a stored lowercase string back into its corresponding [MessageSenderEntity] enum. */
    @TypeConverter
    fun toMessageSender(value: String): MessageSenderEntity = 
        MessageSenderEntity.entries.find { it.name.lowercase() == value } ?: MessageSenderEntity.USER

    @TypeConverter
    fun fromChatId(id: ChatId): String = id.value

    @TypeConverter
    fun toChatId(value: String): ChatId = ChatId(value)

    @TypeConverter
    fun fromMessageId(id: MessageId): String = id.value

    @TypeConverter
    fun toMessageId(value: String): MessageId = MessageId(value)

    @TypeConverter
    fun fromParticipantId(id: ParticipantId): String = id.value

    @TypeConverter
    fun toParticipantId(value: String): ParticipantId = ParticipantId(value)
}
