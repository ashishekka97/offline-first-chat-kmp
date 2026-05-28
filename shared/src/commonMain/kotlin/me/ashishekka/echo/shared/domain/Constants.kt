package me.ashishekka.echo.shared.domain

import me.ashishekka.echo.shared.domain.model.ParticipantId

/**
 * Application-wide constants.
 */
object Constants {
    /** The ID of the local user for the MVP. */
    val CURRENT_USER_ID = ParticipantId("user")

    /** The ID of the default AI agent for the MVP. */
    val DEFAULT_AGENT_ID = ParticipantId("agent")
}
