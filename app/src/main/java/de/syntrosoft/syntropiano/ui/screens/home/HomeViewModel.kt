package de.syntrosoft.syntropiano.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val songRepository: SongRepository,
) : ViewModel() {

    val profile = playerRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfile())

    init {
        viewModelScope.launch { songRepository.seedBuiltInSongs() }
    }
}
