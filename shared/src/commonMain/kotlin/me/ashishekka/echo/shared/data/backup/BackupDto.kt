package me.ashishekka.echo.shared.data.backup

import kotlinx.serialization.Serializable
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.MessageId
import me.ashishekka.echo.shared.domain.model.ParticipantId

/**
 * Root DTO for the initial seed data backup.
 */
@Serializable
data class SeedDataDto(
    val participants: List<ParticipantDto>,
    val chats: List<ChatDto>,
    val messages: Map<ChatId, List<MessageDto>>
)

@Serializable
data class ParticipantDto(
    val id: ParticipantId,
    val name: String,
    val profileImageUrl: String? = null,
    val isAgent: Boolean
)

@Serializable
data class ChatDto(
    val id: ChatId,
    val title: String,
    val participantIds: List<ParticipantId>,
    val lastMessage: String?,
    /** Milliseconds offset from the time of restoration (e.g., -3600000 for 1 hour ago) */
    val lastMessageTimestampOffsetMs: Long,
    val createdAtOffsetMs: Long,
    val updatedAtOffsetMs: Long
)

@Serializable
data class MessageDto(
    val id: MessageId,
    val message: String,
    val type: String,
    val sender: ParticipantId,
    /** Milliseconds offset from the time of restoration */
    val timestampOffsetMs: Long,
    val file: FileDto? = null
)

@Serializable
data class FileDto(
    /** Remote URL of the file (optional fallback) */
    val path: String? = null,
    /** Name of the file bundled in app assets (for zero-network bootstrap) */
    val bundledAssetName: String? = null,
    val fileSize: Long,
    val thumbnail: ThumbnailDto? = null
)

@Serializable
data class ThumbnailDto(
    val path: String? = null,
    val bundledAssetName: String? = null
)
