package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "",
    val difficulty: Int = 1,
    val bpm: Int = 100,
    val timeSignature: String = "4/4",
    val level: Int = 1,
    val notesJson: String,    // Serialized List<Note> as JSON
    val audioFile: String? = null,
    val isBuiltIn: Boolean = true,
    val importedAt: Long? = null,
)
