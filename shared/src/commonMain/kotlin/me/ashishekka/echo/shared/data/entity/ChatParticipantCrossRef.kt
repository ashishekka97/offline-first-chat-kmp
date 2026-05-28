package me.ashishekka.echo.shared.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.ashishekka.echo.shared.domain.model.ChatId
import me.ashishekka.echo.shared.domain.model.ParticipantId

/**
 * A cross-reference table that links [ChatEntity] and [ParticipantEntity] in a many-to-many relationship.
 *
 * @property chatId The ID of the chat.
 * @property participantId The ID of the participant in that chat.
 */
@Entity(
    tableName = "chat_participants",
    primaryKeys = ["chatId", "participantId"],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["participantId"])]
)
data class ChatParticipantCrossRef(
    val chatId: ChatId,
    val participantId: ParticipantId
)
