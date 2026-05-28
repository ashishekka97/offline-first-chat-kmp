package me.ashishekka.echo.shared.data.backup

import me.ashishekka.echo.shared.data.entity.*
import me.ashishekka.echo.shared.domain.model.ChatId

/**
 * Maps a [ParticipantDto] to a [ParticipantEntity].
 */
fun ParticipantDto.toEntity(): ParticipantEntity = ParticipantEntity(
    id = this.id,
    name = this.name,
    profileImageUrl = this.profileImageUrl,
    isAgent = this.isAgent
)

/**
 * Maps a [ChatDto] to a [ChatEntity].
 *
 * @param baseTime The current system time to use as a base for relative offsets.
 */
fun ChatDto.toEntity(baseTime: Long): ChatEntity = ChatEntity(
    id = this.id,
    title = this.title,
    lastMessage = this.lastMessage,
    lastMessageTimestamp = baseTime + this.lastMessageTimestampOffsetMs,
    createdAt = baseTime + this.createdAtOffsetMs,
    updatedAt = baseTime + this.updatedAtOffsetMs
)

/**
 * Maps a [MessageDto] to a [MessageEntity].
 *
 * @param chatId The ID of the chat this message belongs to.
 * @param baseTime The current system time to use as a base for relative offsets.
 */
fun MessageDto.toEntity(chatId: ChatId, baseTime: Long): MessageEntity = MessageEntity(
    id = this.id,
    chatId = chatId,
    senderId = this.sender,
    message = this.message,
    type = if (this.type.lowercase() == "file") MessageTypeEntity.FILE else MessageTypeEntity.TEXT,
    file = this.file?.toEntity(),
    timestamp = baseTime + this.timestampOffsetMs
)

/**
 * Maps a [FileDto] to [FileDetailsEntity].
 * Prefers [bundledAssetName] if available to support zero-network bootstrap.
 */
fun FileDto.toEntity(): FileDetailsEntity = FileDetailsEntity(
    path = this.bundledAssetName ?: this.path ?: "",
    fileSize = this.fileSize,
    thumbnail = this.thumbnail?.toEntity()
)

/**
 * Maps a [ThumbnailDto] to [ThumbnailDetailsEntity].
 */
fun ThumbnailDto.toEntity(): ThumbnailDetailsEntity = ThumbnailDetailsEntity(
    path = this.bundledAssetName ?: this.path ?: ""
)

/**
 * Creates cross-references linking a chat to its participants based on the DTO.
 */
fun ChatDto.toCrossRefs(): List<ChatParticipantCrossRef> = participantIds.map { participantId ->
    ChatParticipantCrossRef(chatId = this.id, participantId = participantId)
}
