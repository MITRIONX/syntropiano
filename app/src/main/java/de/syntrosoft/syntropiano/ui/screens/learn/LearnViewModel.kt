package de.syntrosoft.syntropiano.ui.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelInfo(
    val level: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _levels = MutableStateFlow<List<LevelInfo>>(emptyList())
    val levels: StateFlow<List<LevelInfo>> = _levels

    private val levelDefinitions = listOf(
        Triple(1, "Noten Entdecker", "Notenschlüssel, Notenlinien, C-H"),
        Triple(2, "Erste Melodien", "Taktarten, Notenwerte, Kinderlieder"),
        Triple(3, "Rhythmus & Vorzeichen", "Pausen, Punktierungen, ♯ ♭"),
        Triple(4, "Beide Hände", "Bassschlüssel, Koordination"),
        Triple(5, "Fortgeschritten", "Akkorde, Dynamik, Pedalnutzung"),
    )

    init {
        loadLevels()
    }

    fun loadLevels() {
        viewModelScope.launch {
            val levelInfos = levelDefinitions.map { (level, title, desc) ->
                // Level-Test lesson IDs follow pattern: level * 100 + 99 (e.g. 199, 299)
                val testLessonId = level.toLong() * 100 + 99
                val isCompleted = progressRepository.isLessonCompleted(testLessonId)
                LevelInfo(
                    level = level,
                    title = title,
                    description = desc,
                    isUnlocked = level == 1 || progressRepository.isLessonCompleted((level - 1).toLong() * 100 + 99),
                    isCompleted = isCompleted,
                )
            }
            _levels.value = levelInfos
        }
    }
}
