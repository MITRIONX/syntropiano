package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Pitch

interface PitchDetector {
    /**
     * Detect the fundamental frequency from an audio buffer.
     * @param audioBuffer PCM 16-bit samples
     * @param sampleRate sample rate in Hz (typically 44100)
     * @return detected Pitch or null if no clear pitch found
     */
    fun detect(audioBuffer: ShortArray, sampleRate: Int): Pitch?
}
