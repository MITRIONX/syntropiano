package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Pitch
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {
    private val detector = YinPitchDetector()
    private val sampleRate = 44100

    private fun generateSineWave(frequency: Float, samples: Int = 4096): ShortArray {
        return ShortArray(samples) { i ->
            val sample = sin(2.0 * PI * frequency * i / sampleRate)
            (sample * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
    }

    @Test
    fun `detects A4 at 440 Hz`() {
        val buffer = generateSineWave(440f)
        val result = detector.detect(buffer, sampleRate)
        assertEquals(Pitch.A4, result)
    }

    @Test
    fun `detects C4 middle C`() {
        val buffer = generateSineWave(261.63f)
        val result = detector.detect(buffer, sampleRate)
        assertEquals(Pitch.C4, result)
    }

    @Test
    fun `detects E4`() {
        val buffer = generateSineWave(329.63f)
        val result = detector.detect(buffer, sampleRate)
        assertEquals(Pitch.E4, result)
    }

    @Test
    fun `returns null for silence`() {
        val silence = ShortArray(4096) { 0 }
        val result = detector.detect(silence, sampleRate)
        assertNull(result)
    }
}
