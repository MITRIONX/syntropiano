package de.syntrosoft.syntropiano.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.domain.engine.XpCalculator
import de.syntrosoft.syntropiano.domain.model.Achievement
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val completedSongs: Int = 0,
    val averageAccuracy: Float = 0f,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val progressRepository: ProgressRepository,
    val xpCalculator: XpCalculator,
) : ViewModel() {

    val profile = playerRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfile())

    val achievements = playerRepository.getAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats

    init {
        viewModelScope.launch {
            _stats.value = ProfileStats(
                completedSongs = progressRepository.countCompletedSongs(),
                averageAccuracy = progressRepository.averageAccuracy(),
            )
        }
    }
}
