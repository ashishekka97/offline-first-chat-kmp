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
) {
    /**
     * Helper to get the "other" participant (the agent or another user).
     * For 1-on-1 chats, this returns the participant that is not the current user.
     */
    fun getOtherParticipant(currentUserId: String): ParticipantEntity? {
        return participants.find { it.id != currentUserId }
    }
}

data class MessageWithSender(
    @Embedded val message: MessageEntity,
    @Relation(
        parentColumn = "senderId",
        entityColumn = "id"
    )
    val sender: ParticipantEntity
) {
    /**
     * Helper to check if the message was sent by the current user.
     */
    fun isFromUser(currentUserId: String): Boolean {
        return message.senderId == currentUserId
    }
}
