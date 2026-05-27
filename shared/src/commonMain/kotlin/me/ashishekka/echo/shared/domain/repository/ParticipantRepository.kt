package me.ashishekka.echo.shared.domain.repository

import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Participant

/**
 * Repository interface for participant and profile operations.
 */
interface ParticipantRepository {
    /**
     * Returns a participant by their [id].
     */
    suspend fun getParticipantById(id: String): Result<Participant, DatabaseError>

    /**
     * Returns a list of all participants.
     */
    suspend fun getAllParticipants(): Result<List<Participant>, DatabaseError>

    /**
     * Inserts or updates a participant profile.
     */
    suspend fun saveParticipant(participant: Participant): Result<Unit, DatabaseError>
}
