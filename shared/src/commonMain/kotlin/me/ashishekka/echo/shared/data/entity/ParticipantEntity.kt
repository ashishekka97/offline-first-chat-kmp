package me.ashishekka.echo.shared.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a participant (user or AI agent) in the database.
 *
 * @property id The unique identifier for the participant.
 * @property name The display name of the participant.
 * @property profileImageUrl The URL or local path to the participant's profile image.
 * @property isAgent True if the participant is an AI agent, false if they are a human user.
 */
@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val isAgent: Boolean
)
