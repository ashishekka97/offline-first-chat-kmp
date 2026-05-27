package me.ashishekka.echo.shared.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val profileImageUrl: String?,
    val isAgent: Boolean
)
