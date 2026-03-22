package de.syntrosoft.syntropiano.data.importer

import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SongJson(
    val title: String? = null,
    val artist: String = "",
    val difficulty: Int = 1,
    val bpm: Int = 100,
    val timeSignature: String = "4/4",
    val level: Int = 1,
    val notes: List<Note>? = null,
    val audioFile: String? = null,
)

class SongParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String): Result<Song> = runCatching {
        val raw = json.decodeFromString<SongJson>(jsonString)

        requireNotNull(raw.title) { "Missing required field: title" }
        requireNotNull(raw.notes) { "Missing required field: notes" }
        require(raw.notes.isNotEmpty()) { "Song must have at least one note" }

        raw.notes.forEach { note ->
            requireNotNull(Pitch.fromName(note.pitch)) {
                "Invalid pitch: '${note.pitch}'. Must be in range A0-C8 (e.g. C4, F#3)"
            }
        }

        Song(
            title = raw.title,
            artist = raw.artist,
            difficulty = raw.difficulty.coerceIn(1, 5),
            bpm = raw.bpm,
            timeSignature = raw.timeSignature,
            level = raw.level,
            notes = raw.notes,
            audioFile = raw.audioFile,
            isBuiltIn = false,
            importedAt = System.currentTimeMillis(),
        )
    }
}
