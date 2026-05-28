package me.ashishekka.echo.shared.data.repository

import me.ashishekka.echo.shared.data.dao.ParticipantDao
import me.ashishekka.echo.shared.data.mapper.toDomain
import me.ashishekka.echo.shared.data.mapper.toEntity
import me.ashishekka.echo.shared.domain.DatabaseError
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.Participant
import me.ashishekka.echo.shared.domain.model.ParticipantId
import me.ashishekka.echo.shared.domain.repository.ParticipantRepository

/**
 * Offline-first implementation of [ParticipantRepository].
 */
class OfflineFirstParticipantRepository(
    private val participantDao: ParticipantDao
) : ParticipantRepository {

    override suspend fun getParticipantById(id: ParticipantId): Result<Participant, DatabaseError> {
        return safeDatabaseCall { participantDao.getParticipantById(id) }.flatMap { entity ->
            entity?.toDomain()?.let { Result.Success(it) } ?: Result.Failure(DatabaseError.NotFound)
        } as Result<Participant, DatabaseError>
    }

    override suspend fun getAllParticipants(): Result<List<Participant>, DatabaseError> {
        return safeDatabaseCall {
            participantDao.getAllParticipants().map { it.toDomain() }
        }
    }

    override suspend fun saveParticipant(participant: Participant): Result<Unit, DatabaseError> {
        return safeDatabaseCall {
            participantDao.insertParticipant(participant.toEntity())
        }
    }
}
