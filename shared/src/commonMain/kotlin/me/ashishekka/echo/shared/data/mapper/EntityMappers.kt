package me.ashishekka.echo.shared.data.mapper

import me.ashishekka.echo.shared.data.entity.ChatWithParticipants
import me.ashishekka.echo.shared.data.entity.MessageWithSender
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.domain.model.Chat
import me.ashishekka.echo.shared.domain.model.Message
import me.ashishekka.echo.shared.domain.model.Participant
import me.ashishekka.echo.shared.domain.model.ParticipantId
import me.ashishekka.echo.shared.domain.util.DateTimeUtils
import me.ashishekka.echo.shared.domain.util.FileSizeUtils

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
 */
fun ChatWithParticipants.toDomain(currentUserId: ParticipantId): Chat {
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
        displayTimestamp = DateTimeUtils.formatSmartTimestamp(chat.lastMessageTimestamp)
    )
}

/**
 * Maps [MessageWithSender] to [Message] domain model.
 *
 * @param currentUserId The ID of the local user to set [Message.isFromMe].
 */
fun MessageWithSender.toDomain(currentUserId: ParticipantId): Message {
    return Message(
        id = message.id,
        chatId = message.chatId,
        sender = sender.toDomain(),
        message = message.message,
        type = message.type,
        file = message.file,
        timestamp = message.timestamp,
        isFromMe = message.senderId.value == currentUserId.value,
        displayTimestamp = DateTimeUtils.formatSmartTimestamp(message.timestamp),
        displaySize = message.file?.fileSize?.let { FileSizeUtils.formatFileSize(it) } ?: ""
    )
}
