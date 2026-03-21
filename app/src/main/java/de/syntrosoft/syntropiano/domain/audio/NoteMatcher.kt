package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.NoteResult
import de.syntrosoft.syntropiano.domain.model.Pitch

class NoteMatcher {

    fun match(expected: Note, detected: Pitch?): NoteResult {
        if (detected == null) {
            return NoteResult(expected = expected, detected = null, status = NoteResult.Status.MISSED)
        }

        val expectedPitch = expected.toPitch()
        val isCorrect = expectedPitch == detected

        return NoteResult(
            expected = expected,
            detected = detected,
            status = if (isCorrect) NoteResult.Status.CORRECT else NoteResult.Status.WRONG,
        )
    }
}
