package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.NoteResult
import de.syntrosoft.syntropiano.domain.model.Pitch
import org.junit.Assert.*
import org.junit.Test

class NoteMatcherTest {
    private val matcher = NoteMatcher()

    @Test
    fun `exact pitch match is CORRECT`() {
        val expected = Note(pitch = "C4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, Pitch.C4)
        assertEquals(NoteResult.Status.CORRECT, result.status)
    }

    @Test
    fun `wrong pitch is WRONG`() {
        val expected = Note(pitch = "C4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, Pitch.D4)
        assertEquals(NoteResult.Status.WRONG, result.status)
        assertEquals(Pitch.D4, result.detected)
    }

    @Test
    fun `null pitch is MISSED`() {
        val expected = Note(pitch = "C4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, null)
        assertEquals(NoteResult.Status.MISSED, result.status)
    }

    @Test
    fun `enharmonic equivalent is CORRECT`() {
        // C#4 and Db4 are the same pitch
        val expected = Note(pitch = "C#4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, Pitch.Cs4)
        assertEquals(NoteResult.Status.CORRECT, result.status)
    }
}
