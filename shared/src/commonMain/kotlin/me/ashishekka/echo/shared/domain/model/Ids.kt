package me.ashishekka.echo.shared.domain.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * Strongly typed identifier for a Chat.
 */
@JvmInline
@Serializable
value class ChatId(val value: String) {
    override fun toString(): String = value
}

/**
 * Strongly typed identifier for a Message.
 */
@JvmInline
@Serializable
value class MessageId(val value: String) {
    override fun toString(): String = value
}

/**
 * Strongly typed identifier for a Participant.
 */
@JvmInline
@Serializable
value class ParticipantId(val value: String) {
    override fun toString(): String = value
}
