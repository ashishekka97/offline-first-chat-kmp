package me.ashishekka.echo.shared.data

import androidx.room.TypeConverter
import me.ashishekka.echo.shared.data.entity.MessageSender
import me.ashishekka.echo.shared.data.entity.MessageType

class Converters {
    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name.lowercase()

    @TypeConverter
    fun toMessageType(value: String): MessageType = 
        MessageType.entries.find { it.name.lowercase() == value } ?: MessageType.TEXT

    @TypeConverter
    fun fromMessageSender(value: MessageSender): String = value.name.lowercase()

    @TypeConverter
    fun toMessageSender(value: String): MessageSender = 
        MessageSender.entries.find { it.name.lowercase() == value } ?: MessageSender.USER
}
