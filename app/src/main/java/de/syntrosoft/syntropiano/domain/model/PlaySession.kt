package de.syntrosoft.syntropiano.domain.model

data class PlaySession(
    val song: Song,
    val mode: PlayMode,
    val noteResults: List<NoteResult> = emptyList(),
    val currentNoteIndex: Int = 0,
    val isFinished: Boolean = false,
) {
    val totalNotes: Int get() = song.notes.size
    val correctNotes: Int get() = noteResults.count { it.status == NoteResult.Status.CORRECT }
    val accuracy: Float get() = if (noteResults.isEmpty()) 0f else correctNotes.toFloat() / noteResults.size
    val stars: Int get() = when {
        accuracy >= 0.95f -> 3
        accuracy >= 0.80f -> 2
        accuracy >= 0.60f -> 1
        else -> 0
    }
}
