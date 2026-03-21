package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getSongsByLevel(level: Int): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun importSong(song: Song): Long
    suspend fun seedBuiltInSongs()
}
