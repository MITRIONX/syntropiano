package de.syntrosoft.syntropiano.data.`import`

import de.syntrosoft.syntropiano.domain.model.Song
import org.junit.Assert.*
import org.junit.Test

class SongParserTest {
    private val parser = SongParser()

    private val validJson = """
        {
            "title": "Test Song",
            "artist": "Test Artist",
            "difficulty": 2,
            "bpm": 120,
            "timeSignature": "4/4",
            "notes": [
                {"pitch": "C4", "duration": 1.0, "beat": 0, "hand": "R"},
                {"pitch": "D4", "duration": 1.0, "beat": 1, "hand": "R"}
            ]
        }
    """.trimIndent()

    @Test
    fun `parses valid song JSON`() {
        val result = parser.parse(validJson)
        assertTrue(result.isSuccess)
        val song = result.getOrThrow()
        assertEquals("Test Song", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals(2, song.difficulty)
        assertEquals(120, song.bpm)
        assertEquals(2, song.notes.size)
        assertEquals("C4", song.notes[0].pitch)
    }

    @Test
    fun `fails on missing title`() {
        val json = """{"notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("title") == true)
    }

    @Test
    fun `fails on empty notes`() {
        val json = """{"title": "Empty", "notes": []}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on missing notes`() {
        val json = """{"title": "No Notes"}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on invalid pitch`() {
        val json = """{"title": "Bad", "notes": [{"pitch": "X9", "duration": 1.0, "beat": 0}]}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("pitch") == true)
    }

    @Test
    fun `defaults artist to empty string`() {
        val json = """{"title": "Minimal", "notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val song = parser.parse(json).getOrThrow()
        assertEquals("", song.artist)
    }

    @Test
    fun `defaults difficulty to 1`() {
        val json = """{"title": "Minimal", "notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val song = parser.parse(json).getOrThrow()
        assertEquals(1, song.difficulty)
    }

    @Test
    fun `defaults hand to R`() {
        val json = """{"title": "Test", "notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val song = parser.parse(json).getOrThrow()
        assertEquals("R", song.notes[0].hand)
    }
}
