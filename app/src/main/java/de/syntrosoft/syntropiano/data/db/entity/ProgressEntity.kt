package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long? = null,
    val lessonId: Long? = null,
    val completedAt: Long,
    val stars: Int = 0,
    val accuracy: Float = 0f,
    val xpEarned: Int = 0,
)
