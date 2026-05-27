package me.ashishekka.echo.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.ashishekka.echo.shared.data.entity.ParticipantEntity

/**
 * Data Access Object for participant operations.
 */
@Dao
interface ParticipantDao {
    /**
     * Inserts a new participant or replaces an existing one.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)

    /**
     * Returns a participant by their [id].
     */
    @Query("SELECT * FROM participants WHERE id = :id")
    suspend fun getParticipantById(id: String): ParticipantEntity?

    /**
     * Returns a list of all participants.
     */
    @Query("SELECT * FROM participants")
    suspend fun getAllParticipants(): List<ParticipantEntity>

    /**
     * Deletes all participants from the database.
     */
    @Query("DELETE FROM participants")
    suspend fun deleteAllParticipants()
}
