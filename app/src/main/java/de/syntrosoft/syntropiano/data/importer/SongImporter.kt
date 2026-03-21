package de.syntrosoft.syntropiano.data.importer

import android.content.Context
import android.net.Uri
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.model.Song
import javax.inject.Inject

class SongImporter @Inject constructor(
    private val songParser: SongParser,
    private val songRepository: SongRepository,
) {
    suspend fun import(context: Context, uri: Uri): Result<Song> = runCatching {
        val jsonString = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText()
            ?: throw IllegalArgumentException("Could not read file")

        val song = songParser.parse(jsonString).getOrThrow()
        val id = songRepository.importSong(song)
        song.copy(id = id)
    }
}
