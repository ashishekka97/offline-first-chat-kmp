package me.ashishekka.echo.shared.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.ashishekka.echo.shared.data.entity.ParticipantEntity

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)

    @Query("SELECT * FROM participants WHERE id = :id")
    suspend fun getParticipantById(id: String): ParticipantEntity?

    @Query("SELECT * FROM participants")
    suspend fun getAllParticipants(): List<ParticipantEntity>
}
