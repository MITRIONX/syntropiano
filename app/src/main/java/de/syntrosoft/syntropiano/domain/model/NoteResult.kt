package de.syntrosoft.syntropiano.domain.model

data class NoteResult(
    val expected: Note,
    val detected: Pitch?,
    val status: Status,
) {
    enum class Status { CORRECT, WRONG, MISSED, PENDING }
}
