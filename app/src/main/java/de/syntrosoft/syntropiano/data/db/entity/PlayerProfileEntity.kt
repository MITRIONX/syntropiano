package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1, // singleton
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPlayedAt: Long? = null,
    val totalPlayTimeMinutes: Int = 0,
)
