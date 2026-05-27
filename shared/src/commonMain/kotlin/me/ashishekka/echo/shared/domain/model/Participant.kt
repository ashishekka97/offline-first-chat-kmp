package me.ashishekka.echo.shared.domain.model

/**
 * Domain model representing a participant (user or AI agent).
 *
 * @property id The unique identifier for the participant.
 * @property name The display name of the participant.
 * @property profileImageUrl The URL or local path to the participant's profile image.
 * @property isAgent True if the participant is an AI agent, false if they are a human user.
 */
data class Participant(
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val isAgent: Boolean
)
