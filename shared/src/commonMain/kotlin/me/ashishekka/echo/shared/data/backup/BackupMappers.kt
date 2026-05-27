package me.ashishekka.echo.shared.data.backup

import me.ashishekka.echo.shared.data.entity.ChatEntity
import me.ashishekka.echo.shared.data.entity.ChatParticipantCrossRef
import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageEntity
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.data.entity.ParticipantEntity
import me.ashishekka.echo.shared.data.entity.ThumbnailDetails

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
fun MessageDto.toEntity(chatId: String, baseTime: Long): MessageEntity = MessageEntity(
    id = this.id,
    chatId = chatId,
    senderId = this.sender,
    message = this.message,
    type = if (this.type.lowercase() == "file") MessageType.FILE else MessageType.TEXT,
    file = this.file?.toEntity(),
    timestamp = baseTime + this.timestampOffsetMs
)

/**
 * Maps a [FileDto] to [FileDetails].
 * Prefers [bundledAssetName] if available to support zero-network bootstrap.
 */
fun FileDto.toEntity(): FileDetails = FileDetails(
    path = this.bundledAssetName ?: this.path ?: "",
    fileSize = this.fileSize,
    thumbnail = this.thumbnail?.toEntity()
)

/**
 * Maps a [ThumbnailDto] to [ThumbnailDetails].
 */
fun ThumbnailDto.toEntity(): ThumbnailDetails = ThumbnailDetails(
    path = this.bundledAssetName ?: this.path ?: ""
)

/**
 * Creates cross-references linking a chat to its participants based on the DTO.
 */
fun ChatDto.toCrossRefs(): List<ChatParticipantCrossRef> = participantIds.map { participantId ->
    ChatParticipantCrossRef(chatId = this.id, participantId = participantId)
}
