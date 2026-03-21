package de.syntrosoft.syntropiano.ui.screens.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.audio.NoteMatcher
import de.syntrosoft.syntropiano.domain.engine.AchievementEngine
import de.syntrosoft.syntropiano.domain.engine.ScoreCalculator
import de.syntrosoft.syntropiano.domain.model.*
import de.syntrosoft.syntropiano.ui.audio.AudioCaptureService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    private val progressRepository: ProgressRepository,
    private val playerRepository: PlayerRepository,
    private val noteMatcher: NoteMatcher,
    private val scoreCalculator: ScoreCalculator,
    private val achievementEngine: AchievementEngine,
    val audioCaptureService: AudioCaptureService,
) : ViewModel() {

    private val songId: Long = savedStateHandle["songId"] ?: 0L
    private val modeString: String = savedStateHandle["mode"] ?: "PRACTICE"

    private val _session = MutableStateFlow<PlaySession?>(null)
    val session: StateFlow<PlaySession?> = _session

    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult

    init {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId) ?: return@launch
            val mode = PlayMode.valueOf(modeString)
            _session.value = PlaySession(song = song, mode = mode)
        }

        // Listen to pitch detection
        viewModelScope.launch {
            audioCaptureService.detectedPitch.collect { pitch ->
                if (pitch != null) onPitchDetected(pitch)
            }
        }
    }

    fun startListening() {
        audioCaptureService.start(viewModelScope)
    }

    fun stopListening() {
        audioCaptureService.stop()
    }

    private fun onPitchDetected(pitch: Pitch) {
        val current = _session.value ?: return
        if (current.isFinished) return
        if (current.currentNoteIndex >= current.song.notes.size) return

        val expectedNote = current.song.notes[current.currentNoteIndex]
        val result = noteMatcher.match(expectedNote, pitch)

        // In practice mode, show wrong note feedback but don't advance
        if (current.mode == PlayMode.PRACTICE && result.status != NoteResult.Status.CORRECT) {
            // Show the wrong note (red feedback) but stay on current note
            _session.value = current.copy(
                noteResults = current.noteResults + result,
            )
            return
        }

        val updatedResults = current.noteResults + result
        val nextIndex = current.currentNoteIndex + 1
        val isFinished = nextIndex >= current.song.notes.size

        _session.value = current.copy(
            noteResults = updatedResults,
            currentNoteIndex = nextIndex,
            isFinished = isFinished,
        )

        if (isFinished) {
            onSessionFinished(current.copy(noteResults = updatedResults, isFinished = true))
        }
    }

    private fun onSessionFinished(session: PlaySession) {
        viewModelScope.launch {
            stopListening()
            _showResult.value = true

            val stars = scoreCalculator.calculateStars(session.accuracy)
            val xp = scoreCalculator.calculateXp(session.accuracy, stars, isLevelTest = false)

            progressRepository.saveProgress(
                songId = session.song.id,
                lessonId = null,
                stars = stars,
                accuracy = session.accuracy,
                xpEarned = xp,
            )

            playerRepository.addXp(xp)
            playerRepository.updateStreak()

            // Check achievements
            if (achievementEngine.check(AchievementType.FIRST_NOTE, totalCorrectNotes = session.correctNotes)) {
                playerRepository.unlockAchievement(AchievementType.FIRST_NOTE)
            }
            if (session.accuracy >= 1.0f && achievementEngine.check(AchievementType.PERFECTIONIST, hasPerfectSong = true)) {
                playerRepository.unlockAchievement(AchievementType.PERFECTIONIST)
            }
        }
    }

    fun retry() {
        val current = _session.value ?: return
        _session.value = PlaySession(song = current.song, mode = current.mode)
        _showResult.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
