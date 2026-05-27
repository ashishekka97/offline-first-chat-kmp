package me.ashishekka.echo.shared.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ChatWithParticipants(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            ChatParticipantCrossRef::class,
            parentColumn = "chatId",
            entityColumn = "participantId"
        )
    )
    val participants: List<ParticipantEntity>
)

data class MessageWithSender(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "senderId",
        entityColumn = "id"
    )
    val sender: ParticipantEntity
)
