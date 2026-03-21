package de.syntrosoft.syntropiano.domain.model

data class Lesson(
    val id: Long = 0,
    val level: Int,
    val order: Int,
    val title: String,
    val type: LessonType,
    val content: String = "",  // JSON content
    val songId: Long? = null,  // linked song for SONG/TEST types
)
