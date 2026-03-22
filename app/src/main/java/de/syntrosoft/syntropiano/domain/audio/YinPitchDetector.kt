package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Pitch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * YIN pitch detection algorithm.
 * Reference: De Cheveigné & Kawahara (2002) "YIN, a fundamental frequency estimator"
 */
class YinPitchDetector(
    private val threshold: Float = 0.15f,
    private val rmsThreshold: Float = 0.04f, // Minimum volume to consider (filters background noise / speech)
) : PitchDetector {

    override fun detect(audioBuffer: ShortArray, sampleRate: Int): Pitch? {
        val floatBuffer = FloatArray(audioBuffer.size) { audioBuffer[it].toFloat() / Short.MAX_VALUE }

        // RMS amplitude check – reject quiet signals (noise, distant speech)
        var sumSquares = 0f
        for (sample in floatBuffer) sumSquares += sample * sample
        val rms = sqrt(sumSquares / floatBuffer.size)
        if (rms < rmsThreshold) return null

        val halfSize = floatBuffer.size / 2

        // Step 1 & 2: Difference function
        val diff = FloatArray(halfSize)
        for (tau in 1 until halfSize) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = floatBuffer[i] - floatBuffer[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 3: Cumulative mean normalized difference
        val cmndf = FloatArray(halfSize)
        cmndf[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfSize) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum == 0f) 1f else diff[tau] * tau / runningSum
        }

        // Step 4: Absolute threshold
        var tau = 2
        while (tau < halfSize) {
            if (cmndf[tau] < threshold) {
                while (tau + 1 < halfSize && cmndf[tau + 1] < cmndf[tau]) {
                    tau++
                }
                break
            }
            tau++
        }

        if (tau >= halfSize) return null

        // Step 5: Parabolic interpolation
        val betterTau = if (tau > 0 && tau < halfSize - 1) {
            val s0 = cmndf[tau - 1]
            val s1 = cmndf[tau]
            val s2 = cmndf[tau + 1]
            val adjustment = (s2 - s0) / (2 * (2 * s1 - s2 - s0))
            if (abs(adjustment) < 1) tau + adjustment else tau.toFloat()
        } else {
            tau.toFloat()
        }

        val frequency = sampleRate / betterTau
        return Pitch.fromFrequency(frequency)
    }
}
