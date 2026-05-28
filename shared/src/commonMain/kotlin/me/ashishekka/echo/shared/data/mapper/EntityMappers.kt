package me.ashishekka.echo.shared.data.mapper

import me.ashishekka.echo.shared.data.entity.*
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.util.DateTimeUtils
import me.ashishekka.echo.shared.domain.util.FileSizeUtils
import me.ashishekka.echo.shared.util.StringProvider

/**
 * Maps [ParticipantEntity] to [Participant] domain model.
 */
fun ParticipantEntity.toDomain(): Participant {
    return Participant(
        id = id,
        name = name,
        profileImageUrl = profileImageUrl,
        isAgent = isAgent
    )
}

/**
 * Maps [Participant] domain model to [ParticipantEntity].
 */
fun Participant.toEntity(): ParticipantEntity {
    return ParticipantEntity(
        id = id,
        name = name,
        profileImageUrl = profileImageUrl,
        isAgent = isAgent
    )
}

/**
 * Maps [ChatWithParticipants] to [Chat] domain model.
 *
 * @param currentUserId The ID of the local user to resolve titles for 1-on-1 chats.
 * @param stringProvider The provider for localized smart timestamps.
 */
fun ChatWithParticipants.toDomain(currentUserId: ParticipantId, stringProvider: StringProvider): Chat {
    val displayTitle = if (participants.size == 2) {
        participants.find { it.id.value != currentUserId.value }?.name ?: chat.title
    } else {
        chat.title
    }

    return Chat(
        id = chat.id,
        title = displayTitle,
        lastMessage = chat.lastMessage,
        lastMessageTimestamp = chat.lastMessageTimestamp,
        createdAt = chat.createdAt,
        updatedAt = chat.updatedAt,
        displayTimestamp = DateTimeUtils.formatSmartTimestamp(chat.lastMessageTimestamp, stringProvider)
    )
}

/**
 * Maps [MessageWithSender] to [Message] domain model.
 *
 * @param currentUserId The ID of the local user to set [Message.isFromMe].
 * @param stringProvider The provider for localized smart timestamps.
 */
fun MessageWithSender.toDomain(currentUserId: ParticipantId, stringProvider: StringProvider): Message {
    return Message(
        id = message.id,
        chatId = message.chatId,
        sender = sender.toDomain(),
        message = message.message,
        type = message.type.toDomain(),
        file = message.file?.toDomain(),
        timestamp = message.timestamp,
        isFromMe = message.senderId.value == currentUserId.value,
        displayTimestamp = DateTimeUtils.formatSmartTimestamp(message.timestamp, stringProvider),
        displaySize = message.file?.fileSize?.let { FileSizeUtils.formatFileSize(it) } ?: ""
    )
}

/**
 * Maps [MessageTypeEntity] to [MessageType] domain model.
 */
fun MessageTypeEntity.toDomain(): MessageType = when (this) {
    MessageTypeEntity.TEXT -> MessageType.TEXT
    MessageTypeEntity.FILE -> MessageType.FILE
}

/**
 * Maps [MessageType] domain model to [MessageTypeEntity].
 */
fun MessageType.toEntity(): MessageTypeEntity = when (this) {
    MessageType.TEXT -> MessageTypeEntity.TEXT
    MessageType.FILE -> MessageTypeEntity.FILE
}

/**
 * Maps [FileDetailsEntity] to [FileDetails] domain model.
 */
fun FileDetailsEntity.toDomain(): FileDetails {
    return FileDetails(
        path = path,
        fileSize = fileSize,
        thumbnail = thumbnail?.toDomain()
    )
}

/**
 * Maps [FileDetails] domain model to [FileDetailsEntity].
 */
fun FileDetails.toEntity(): FileDetailsEntity {
    return FileDetailsEntity(
        path = path,
        fileSize = fileSize,
        thumbnail = thumbnail?.toEntity()
    )
}

/**
 * Maps [ThumbnailDetailsEntity] to [ThumbnailDetails] domain model.
 */
fun ThumbnailDetailsEntity.toDomain(): ThumbnailDetails {
    return ThumbnailDetails(
        path = path
    )
}

/**
 * Maps [ThumbnailDetails] domain model to [ThumbnailDetailsEntity].
 */
fun ThumbnailDetails.toEntity(): ThumbnailDetailsEntity {
    return ThumbnailDetailsEntity(
        path = path
    )
}
