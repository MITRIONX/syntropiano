package de.syntrosoft.syntropiano.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

class ToneGenerator {

    companion object {
        const val SAMPLE_RATE = 44100

        private val HARMONICS = floatArrayOf(
            1.0f,    // fundamental
            0.45f,   // 2nd harmonic
            0.20f,   // 3rd
            0.10f,   // 4th
            0.05f,   // 5th
        )
    }

    private var audioTrack: AudioTrack? = null

    fun startStream() {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    /**
     * Writes a tone into the running stream. Blocks until all samples are written.
     * Must call startStream() first.
     */
    fun writeTone(frequencyHz: Float, durationMs: Long) {
        val track = audioTrack ?: return
        val numSamples = (SAMPLE_RATE * durationMs / 1000).toInt()
        val samples = ShortArray(numSamples)

        val attackSamples = min((SAMPLE_RATE * 0.003).toInt(), numSamples / 4)
        val releaseSamples = min((SAMPLE_RATE * 0.015).toInt(), numSamples / 4)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            var sample = 0.0
            for ((h, amp) in HARMONICS.withIndex()) {
                val harmonicFreq = frequencyHz * (h + 1)
                if (harmonicFreq > SAMPLE_RATE / 2) break
                val decay = exp(-t * (1.8 + h * 2.0))
                sample += amp * decay * sin(2.0 * PI * harmonicFreq * t)
            }

            // Attack
            if (i < attackSamples) {
                sample *= i.toDouble() / attackSamples
            }
            // Release
            if (i > numSamples - releaseSamples) {
                sample *= (numSamples - i).toDouble() / releaseSamples
            }

            val clamped = sample.coerceIn(-1.0, 1.0)
            samples[i] = (clamped * Short.MAX_VALUE * 0.7).toInt().toShort()
        }

        track.write(samples, 0, samples.size)
    }

    /**
     * Writes silence into the stream for a given duration.
     */
    fun writeSilence(durationMs: Long) {
        val track = audioTrack ?: return
        val numSamples = (SAMPLE_RATE * durationMs / 1000).toInt()
        track.write(ShortArray(numSamples), 0, numSamples)
    }

    fun stopStream() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
