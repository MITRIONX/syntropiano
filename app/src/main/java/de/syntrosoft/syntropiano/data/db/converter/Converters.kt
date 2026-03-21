package de.syntrosoft.syntropiano.data.db.converter

import androidx.room.TypeConverter
import de.syntrosoft.syntropiano.domain.model.Note
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun notesToJson(notes: List<Note>): String = json.encodeToString(notes)

    @TypeConverter
    fun jsonToNotes(jsonString: String): List<Note> = json.decodeFromString(jsonString)
}
