package de.syntrosoft.syntropiano.data.db.dao

import androidx.room.*
import de.syntrosoft.syntropiano.data.db.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Insert
    suspend fun insert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE songId = :songId ORDER BY completedAt DESC LIMIT 1")
    suspend fun getBestForSong(songId: Long): ProgressEntity?

    @Query("SELECT MAX(stars) FROM progress WHERE songId = :songId")
    suspend fun getBestStarsForSong(songId: Long): Int?

    @Query("SELECT COUNT(DISTINCT songId) FROM progress WHERE stars >= 3")
    suspend fun countThreeStarSongs(): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM progress WHERE stars >= 1")
    suspend fun countCompletedSongs(): Int

    @Query("SELECT AVG(accuracy) FROM progress")
    suspend fun averageAccuracy(): Float?

    @Query("SELECT * FROM progress ORDER BY completedAt DESC")
    fun getAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE lessonId = :lessonId ORDER BY completedAt DESC LIMIT 1")
    suspend fun getForLesson(lessonId: Long): ProgressEntity?
}
