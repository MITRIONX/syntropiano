package de.syntrosoft.syntropiano.ui.screens.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongWithStars(val song: Song, val bestStars: Int)

@HiltViewModel
class SongListViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _songs = MutableStateFlow<List<SongWithStars>>(emptyList())
    val songs: StateFlow<List<SongWithStars>> = _songs

    init {
        viewModelScope.launch {
            songRepository.getAllSongs().collect { songList ->
                _songs.value = songList.map { song ->
                    SongWithStars(song, progressRepository.getBestStarsForSong(song.id))
                }
            }
        }
    }
}
