package de.syntrosoft.syntropiano.ui.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import de.syntrosoft.syntropiano.domain.audio.PitchDetector
import de.syntrosoft.syntropiano.domain.model.Pitch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCaptureService @Inject constructor(
    private val pitchDetector: PitchDetector,
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 4096
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    private val _detectedPitch = MutableStateFlow<Pitch?>(null)
    val detectedPitch: StateFlow<Pitch?> = _detectedPitch

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun start(scope: CoroutineScope) {
        if (_isListening.value) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, BUFFER_SIZE * 2),
        )

        audioRecord?.startRecording()
        _isListening.value = true

        captureJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(BUFFER_SIZE)
            while (isActive && _isListening.value) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: break
                if (read > 0) {
                    val pitch = pitchDetector.detect(buffer, SAMPLE_RATE)
                    _detectedPitch.value = pitch
                }
            }
        }
    }

    fun stop() {
        _isListening.value = false
        captureJob?.cancel()
        captureJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _detectedPitch.value = null
    }
}
