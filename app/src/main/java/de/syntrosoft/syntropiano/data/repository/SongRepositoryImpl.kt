package de.syntrosoft.syntropiano.data.repository

import android.content.Context
import de.syntrosoft.syntropiano.data.db.dao.SongDao
import de.syntrosoft.syntropiano.data.db.entity.SongEntity
import de.syntrosoft.syntropiano.data.importer.SongParser
import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val songParser: SongParser,
    private val context: Context,
) : SongRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllSongs(): Flow<List<Song>> =
        songDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getSongsByLevel(level: Int): Flow<List<Song>> =
        songDao.getByLevel(level).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSongById(id: Long): Song? =
        songDao.getById(id)?.toDomain()

    override suspend fun importSong(song: Song): Long {
        val entity = song.toEntity()
        return songDao.insert(entity)
    }

    override suspend fun seedBuiltInSongs() {
        if (songDao.count() > 0) return
        val assetFiles = context.assets.list("songs") ?: return
        val songs = assetFiles.mapNotNull { filename ->
            val jsonString = context.assets.open("songs/$filename").bufferedReader().readText()
            songParser.parse(jsonString).getOrNull()?.copy(isBuiltIn = true)
        }
        songDao.insertAll(songs.map { it.toEntity() })
    }

    private fun SongEntity.toDomain() = Song(
        id = id, title = title, artist = artist, difficulty = difficulty,
        bpm = bpm, timeSignature = timeSignature, level = level,
        notes = json.decodeFromString(notesJson),
        isBuiltIn = isBuiltIn, importedAt = importedAt,
    )

    private fun Song.toEntity() = SongEntity(
        id = id, title = title, artist = artist, difficulty = difficulty,
        bpm = bpm, timeSignature = timeSignature, level = level,
        notesJson = json.encodeToString(notes),
        isBuiltIn = isBuiltIn, importedAt = importedAt,
    )
}
