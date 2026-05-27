package me.ashishekka.echo.shared.data.backup

import kotlinx.serialization.Serializable

/**
 * Root DTO for the initial seed data backup.
 */
@Serializable
data class SeedDataDto(
    val participants: List<ParticipantDto>,
    val chats: List<ChatDto>,
    val messages: Map<String, List<MessageDto>>
)

@Serializable
data class ParticipantDto(
    val id: String,
    val name: String,
    val profileImageUrl: String? = null,
    val isAgent: Boolean
)

@Serializable
data class ChatDto(
    val id: String,
    val title: String,
    val participantIds: List<String>,
    val lastMessage: String?,
    /** Milliseconds offset from the time of restoration (e.g., -3600000 for 1 hour ago) */
    val lastMessageTimestampOffsetMs: Long,
    val createdAtOffsetMs: Long,
    val updatedAtOffsetMs: Long
)

@Serializable
data class MessageDto(
    val id: String,
    val message: String,
    val type: String, 
    val sender: String, 
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
