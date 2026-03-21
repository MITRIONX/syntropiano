package de.syntrosoft.syntropiano.domain.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String = "",
    val difficulty: Int = 1,      // 1-5
    val bpm: Int = 100,
    val timeSignature: String = "4/4",
    val level: Int = 1,
    val notes: List<Note>,
    val isBuiltIn: Boolean = true,
    val importedAt: Long? = null,
)
