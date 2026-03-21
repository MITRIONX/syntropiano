package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val type: String, // AchievementType.name
    val unlockedAt: Long? = null,
    val progress: Float = 0f,
)
