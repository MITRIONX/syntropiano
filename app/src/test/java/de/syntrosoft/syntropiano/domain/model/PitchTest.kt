package de.syntrosoft.syntropiano.domain.model

import org.junit.Assert.*
import org.junit.Test

class PitchTest {
    @Test
    fun `C4 is middle C at 261_63 Hz`() {
        val pitch = Pitch.C4
        assertEquals(261.63f, pitch.frequency, 0.01f)
    }

    @Test
    fun `A4 is concert pitch at 440 Hz`() {
        assertEquals(440.0f, Pitch.A4.frequency, 0.01f)
    }

    @Test
    fun `fromFrequency finds nearest pitch`() {
        val pitch = Pitch.fromFrequency(442.0f)
        assertEquals(Pitch.A4, pitch)
    }

    @Test
    fun `fromFrequency returns null for out of range`() {
        assertNull(Pitch.fromFrequency(10.0f))
    }

    @Test
    fun `fromName parses C4`() {
        assertEquals(Pitch.C4, Pitch.fromName("C4"))
    }

    @Test
    fun `fromName parses sharp notes`() {
        assertEquals(Pitch.Cs4, Pitch.fromName("C#4"))
    }

    @Test
    fun `displayName formats correctly`() {
        assertEquals("C4", Pitch.C4.displayName)
        assertEquals("C#4", Pitch.Cs4.displayName)
    }

    @Test
    fun `pitches are ordered by frequency`() {
        assertTrue(Pitch.C4.frequency < Pitch.D4.frequency)
        assertTrue(Pitch.D4.frequency < Pitch.E4.frequency)
    }
}
