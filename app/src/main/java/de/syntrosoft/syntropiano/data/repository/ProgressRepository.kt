package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.data.db.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    suspend fun saveProgress(songId: Long?, lessonId: Long?, stars: Int, accuracy: Float, xpEarned: Int)
    suspend fun getBestStarsForSong(songId: Long): Int
    suspend fun countThreeStarSongs(): Int
    suspend fun countCompletedSongs(): Int
    suspend fun averageAccuracy(): Float
    suspend fun isLessonCompleted(lessonId: Long): Boolean
    fun getAllProgress(): Flow<List<ProgressEntity>>
}
