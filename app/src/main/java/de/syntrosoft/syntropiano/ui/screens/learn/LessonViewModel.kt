package de.syntrosoft.syntropiano.ui.screens.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.engine.ScoreCalculator
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.domain.model.Song
import de.syntrosoft.syntropiano.ui.components.QuizQuestion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LessonStep { THEORY, QUIZ, SONG_PRACTICE, TEST, COMPLETED }

data class LessonState(
    val level: Int = 1,
    val currentStep: LessonStep = LessonStep.THEORY,
    val theoryTitle: String = "",
    val theoryContent: String = "",
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val currentQuizIndex: Int = 0,
    val quizCorrect: Int = 0,
    val quizTotal: Int = 0,
    val levelSongs: List<Song> = emptyList(),
    val testPassed: Boolean = false,
)

@HiltViewModel
class LessonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    private val progressRepository: ProgressRepository,
    private val playerRepository: PlayerRepository,
    private val scoreCalculator: ScoreCalculator,
) : ViewModel() {

    private val level: Int = savedStateHandle["lessonLevel"] ?: 1

    private val _state = MutableStateFlow(LessonState(level = level))
    val state: StateFlow<LessonState> = _state

    init {
        loadLessonContent()
    }

    private fun loadLessonContent() {
        viewModelScope.launch {
            // Generate theory content based on level
            val (title, content) = getTheoryForLevel(level)
            val quizzes = generateQuizQuestions(level)

            songRepository.getSongsByLevel(level).collect { songs ->
                _state.value = _state.value.copy(
                    theoryTitle = title,
                    theoryContent = content,
                    quizQuestions = quizzes,
                    levelSongs = songs,
                )
            }
        }
    }

    fun advanceToQuiz() {
        _state.value = _state.value.copy(currentStep = LessonStep.QUIZ)
    }

    fun onQuizAnswer(correct: Boolean) {
        val current = _state.value
        val newCorrect = if (correct) current.quizCorrect + 1 else current.quizCorrect
        val newTotal = current.quizTotal + 1
        val nextIndex = current.currentQuizIndex + 1

        _state.value = current.copy(
            quizCorrect = newCorrect,
            quizTotal = newTotal,
            currentQuizIndex = nextIndex,
        )

        if (nextIndex >= current.quizQuestions.size) {
            _state.value = _state.value.copy(currentStep = LessonStep.SONG_PRACTICE)
        }
    }

    fun advanceToTest() {
        _state.value = _state.value.copy(currentStep = LessonStep.TEST)
    }

    fun onTestCompleted(accuracy: Float) {
        val passed = accuracy >= 0.70f
        viewModelScope.launch {
            if (passed) {
                val testLessonId = level.toLong() * 100 + 99
                val xp = 200 // level test XP
                progressRepository.saveProgress(
                    songId = null,
                    lessonId = testLessonId,
                    stars = scoreCalculator.calculateStars(accuracy),
                    accuracy = accuracy,
                    xpEarned = xp,
                )
                playerRepository.addXp(xp)
            }
            _state.value = _state.value.copy(
                currentStep = LessonStep.COMPLETED,
                testPassed = passed,
            )
        }
    }

    private fun getTheoryForLevel(level: Int): Pair<String, String> = when (level) {
        1 -> "Noten Entdecker" to """
            Willkommen in der Welt der Musik!

            Das Notensystem besteht aus 5 Linien. Darauf werden die Noten platziert.

            Die 7 Grundnoten heißen: C - D - E - F - G - A - H

            Auf dem Keyboard findest du sie als weiße Tasten.
            Die Note C ist immer links neben den 2 schwarzen Tasten.

            Probiere es aus: Finde das C auf deinem Keyboard!
        """.trimIndent()
        2 -> "Erste Melodien" to """
            Super, du kennst jetzt die Noten!

            Noten haben verschiedene Längen:
            • Ganze Note = 4 Schläge ○
            • Halbe Note = 2 Schläge 𝅗𝅥
            • Viertelnote = 1 Schlag ♩

            Die meisten Lieder sind im 4/4-Takt: 4 Schläge pro Takt.

            Jetzt spielen wir deine ersten Melodien!
        """.trimIndent()
        3 -> "Rhythmus & Vorzeichen" to """
            Zeit für Vorzeichen!

            ♯ (Kreuz) = einen Halbton höher → schwarze Taste rechts
            ♭ (Be) = einen Halbton tiefer → schwarze Taste links

            Neue Notenwerte:
            • Achtel = halber Schlag ♪
            • Punktierte Note = 1,5× so lang
            • Pause = Stille (gleiche Längen wie Noten)
        """.trimIndent()
        4 -> "Beide Hände" to """
            Jetzt wird es spannend: Beide Hände!

            Die linke Hand spielt im Bassschlüssel (unteres System).
            Die rechte Hand spielt im Violinschlüssel (oberes System).

            Fange langsam an: Erst links alleine üben,
            dann rechts, dann zusammen.
        """.trimIndent()
        else -> "Fortgeschritten" to """
            Du bist jetzt ein erfahrener Spieler!

            Akkorde: Mehrere Noten gleichzeitig spielen.
            Dynamik: laut (f) und leise (p) spielen.

            Übe die Stücke in verschiedenen Tempi!
        """.trimIndent()
    }

    private fun generateQuizQuestions(level: Int): List<QuizQuestion> {
        val notesForLevel = when (level) {
            1 -> listOf(Pitch.C4, Pitch.D4, Pitch.E4, Pitch.F4, Pitch.G4, Pitch.A4, Pitch.B4)
            2 -> listOf(Pitch.C4, Pitch.D4, Pitch.E4, Pitch.F4, Pitch.G4, Pitch.A4, Pitch.B4, Pitch.C5)
            else -> Pitch.entries.filter { it.frequency >= Pitch.C3.frequency && it.frequency <= Pitch.C6.frequency }
        }

        return (1..5).map {
            val correct = notesForLevel.random()
            val wrongOptions = notesForLevel.filter { it != correct }.shuffled().take(3)
            QuizQuestion(
                correctPitch = correct,
                options = (wrongOptions + correct).shuffled(),
            )
        }
    }
}
