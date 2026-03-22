package de.syntrosoft.syntropiano.domain.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.io.File

sealed interface SongPlayerState {
    data object Idle : SongPlayerState
    data object Playing : SongPlayerState
    data object Finished : SongPlayerState
}

class SongPlayer(
    private val context: Context,
) {
    private val _state = MutableStateFlow<SongPlayerState>(SongPlayerState.Idle)
    val state: StateFlow<SongPlayerState> = _state

    private val _currentNoteIndex = MutableStateFlow(-1)
    val currentNoteIndex: StateFlow<Int> = _currentNoteIndex

    private val _playbackProgress = MutableStateFlow(-1f)
    val playbackProgress: StateFlow<Float> = _playbackProgress

    private var mediaPlayer: MediaPlayer? = null
    private var playJob: Job? = null
    private var noteStartTimes: List<Long> = emptyList()
    private var currentSong: Song? = null

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume

    private var _speed = 1.0f

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(vol, vol)
    }

    private var usingMidiForTempo = false

    fun setSpeed(speed: Float) {
        _speed = speed.coerceIn(0.25f, 2f)
        // Only apply PlaybackParams if playing MP3 (not MIDI-at-adjusted-tempo)
        if (!usingMidiForTempo) {
            try {
                mediaPlayer?.playbackParams = PlaybackParams().setSpeed(_speed)
            } catch (_: Exception) {}
        }
    }

    fun play(song: Song, scope: CoroutineScope, onFinished: () -> Unit = {}, startFromNoteIndex: Int = 0, midiHands: Set<String> = setOf("L", "R")) {
        stop()

        currentSong = song
        _state.value = SongPlayerState.Playing
        _currentNoteIndex.value = startFromNoteIndex
        _playbackProgress.value = startFromNoteIndex.toFloat()

        val speed = _speed

        playJob = scope.launch(Dispatchers.IO) {
            try {
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mediaPlayer = player

                // Use MIDI when tempo != 1.0, no audio file, or hand-specific playback
                val useMidi = song.audioFile == null || speed != 1.0f || midiHands.size < 2
                usingMidiForTempo = useMidi

                if (useMidi) {
                    val effectiveBpm = (song.bpm * speed).toInt().coerceAtLeast(10)
                    // Filter to only the hands that should produce sound
                    val midiSong = if (midiHands.size < 2) song.copy(notes = song.notes.filter { it.hand in midiHands }) else song
                    val midiFile = generateMidiFile(midiSong, effectiveBpm)
                    player.setDataSource(midiFile.absolutePath)
                } else {
                    val afd: AssetFileDescriptor = context.assets.openFd(song.audioFile!!)
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }

                val vol = _volume.value
                player.setVolume(vol, vol)
                player.prepare()

                player.setOnCompletionListener {
                    _state.value = SongPlayerState.Finished
                    _currentNoteIndex.value = -1
                    _playbackProgress.value = -1f
                    onFinished()
                }

                player.start()

                // Note start times: MIDI has tempo baked in, MP3 uses original BPM
                val effectiveBpmForTracking = if (useMidi) song.bpm * speed else song.bpm.toFloat()
                noteStartTimes = song.notes.map { note ->
                    (note.beat * 60_000 / effectiveBpmForTracking).toLong()
                }

                // Seek to position if resuming mid-song
                if (startFromNoteIndex > 0 && startFromNoteIndex in noteStartTimes.indices) {
                    val seekMs = noteStartTimes[startFromNoteIndex].toInt()
                    player.seekTo(seekMs)
                }

                // Cursor tracking
                while (isActive) {
                    val p = mediaPlayer ?: break
                    try {
                        if (!p.isPlaying) break
                        val pos = p.currentPosition.toLong()
                        val times = noteStartTimes
                        val index = times.indexOfLast { it <= pos }
                        if (index >= 0) {
                            _currentNoteIndex.value = index
                            // Interpolate floating progress between notes
                            val progress = if (index < times.lastIndex) {
                                val start = times[index]
                                val end = times[index + 1]
                                val span = end - start
                                if (span > 0) index + (pos - start).toFloat() / span
                                else index.toFloat()
                            } else {
                                index.toFloat()
                            }
                            _playbackProgress.value = progress
                        }
                    } catch (_: IllegalStateException) {
                        break
                    }
                    delay(30)
                }
            } catch (_: CancellationException) {
                // stopped externally
            } catch (e: Exception) {
                _state.value = SongPlayerState.Idle
                _currentNoteIndex.value = -1
                _playbackProgress.value = -1f
            }
        }
    }

    fun seekTo(noteIndex: Int) {
        val times = noteStartTimes
        if (noteIndex !in times.indices) return
        val ms = times[noteIndex].toInt()
        try {
            mediaPlayer?.seekTo(ms)
        } catch (_: Exception) {
            return
        }
        _currentNoteIndex.value = noteIndex
        _playbackProgress.value = noteIndex.toFloat()
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        currentSong = null
        usingMidiForTempo = false
        _state.value = SongPlayerState.Idle
        _currentNoteIndex.value = -1
        _playbackProgress.value = -1f
    }

    // --- MIDI fallback for songs without audioFile ---

    private fun generateMidiFile(song: Song, bpm: Int = song.bpm): File {
        val bytes = buildMidi(song, bpm)
        val file = File(context.cacheDir, "playback.mid")
        file.writeBytes(bytes)
        return file
    }

    private fun buildMidi(song: Song, bpm: Int): ByteArray {
        val ticksPerBeat = 480
        val usPerBeat = 60_000_000 / bpm

        // Alle MIDI-Events mit absolutem Tick sammeln
        data class MidiEvent(val tick: Int, val data: ByteArray, val sortPriority: Int = 0)

        val events = mutableListOf<MidiEvent>()

        for (note in song.notes) {
            val midi = pitchNameToMidi(note.pitch) ?: continue
            val startTick = (note.beat * ticksPerBeat).toInt()
            val durTicks = (note.duration * ticksPerBeat).toInt()

            // Note-On (priority 1 = nach Note-Off bei gleichem Tick)
            events.add(MidiEvent(startTick, byteArrayOf(0x90.toByte(), midi.toByte(), 100), 1))
            // Note-Off (priority 0 = vor Note-On bei gleichem Tick)
            events.add(MidiEvent(startTick + durTicks, byteArrayOf(0x80.toByte(), midi.toByte(), 64), 0))
        }

        // Nach Tick und Priorität sortieren
        events.sortWith(compareBy({ it.tick }, { it.sortPriority }))

        val track = ByteArrayOutputStream()

        // Tempo
        track.writeEvent(0, byteArrayOf(0xFF.toByte(), 0x51, 0x03,
            (usPerBeat shr 16 and 0xFF).toByte(),
            (usPerBeat shr 8 and 0xFF).toByte(),
            (usPerBeat and 0xFF).toByte(),
        ))

        // Program change: piano
        track.writeEvent(0, byteArrayOf(0xC0.toByte(), 0x00))

        // Events mit Delta-Times schreiben
        var lastTick = 0
        for (event in events) {
            val delta = (event.tick - lastTick).coerceAtLeast(0)
            track.writeEvent(delta, event.data)
            lastTick = event.tick
        }

        track.writeEvent(0, byteArrayOf(0xFF.toByte(), 0x2F, 0x00))
        val trackBytes = track.toByteArray()

        val out = ByteArrayOutputStream()
        out.write("MThd".toByteArray())
        out.write(int32(6))
        out.write(int16(0))
        out.write(int16(1))
        out.write(int16(ticksPerBeat))
        out.write("MTrk".toByteArray())
        out.write(int32(trackBytes.size))
        out.write(trackBytes)
        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.writeEvent(delta: Int, data: ByteArray) {
        write(varLen(delta))
        write(data)
    }

    private fun pitchNameToMidi(name: String): Int? {
        val semitones = mapOf(
            "C" to 0, "C#" to 1, "D" to 2, "D#" to 3, "E" to 4, "F" to 5,
            "F#" to 6, "G" to 7, "G#" to 8, "A" to 9, "A#" to 10, "B" to 11,
        )
        val octave = name.lastOrNull()?.digitToIntOrNull() ?: return null
        val noteName = name.dropLast(1)
        val semitone = semitones[noteName] ?: return null
        return (octave + 1) * 12 + semitone
    }

    private fun varLen(value: Int): ByteArray {
        if (value <= 0) return byteArrayOf(0)
        if (value < 0x80) return byteArrayOf(value.toByte())
        val bytes = mutableListOf<Byte>()
        var v = value
        bytes.add(0, (v and 0x7F).toByte())
        v = v shr 7
        while (v > 0) {
            bytes.add(0, ((v and 0x7F) or 0x80).toByte())
            v = v shr 7
        }
        return bytes.toByteArray()
    }

    private fun int32(v: Int) = byteArrayOf(
        (v shr 24 and 0xFF).toByte(), (v shr 16 and 0xFF).toByte(),
        (v shr 8 and 0xFF).toByte(), (v and 0xFF).toByte(),
    )

    private fun int16(v: Int) = byteArrayOf(
        (v shr 8 and 0xFF).toByte(), (v and 0xFF).toByte(),
    )
}
