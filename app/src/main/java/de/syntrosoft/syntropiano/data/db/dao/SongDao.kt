package de.syntrosoft.syntropiano.data.db.dao

import androidx.room.*
import de.syntrosoft.syntropiano.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY difficulty, title")
    fun getAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE level = :level ORDER BY difficulty")
    fun getByLevel(level: Int): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Query("DELETE FROM songs WHERE isBuiltIn = 1")
    suspend fun deleteBuiltIn()
}
