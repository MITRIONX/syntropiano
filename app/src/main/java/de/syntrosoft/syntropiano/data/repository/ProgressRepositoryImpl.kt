package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.data.db.dao.ProgressDao
import de.syntrosoft.syntropiano.data.db.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao,
) : ProgressRepository {

    override suspend fun saveProgress(songId: Long?, lessonId: Long?, stars: Int, accuracy: Float, xpEarned: Int) {
        progressDao.insert(
            ProgressEntity(
                songId = songId, lessonId = lessonId,
                completedAt = System.currentTimeMillis(),
                stars = stars, accuracy = accuracy, xpEarned = xpEarned,
            )
        )
    }

    override suspend fun getBestStarsForSong(songId: Long): Int =
        progressDao.getBestStarsForSong(songId) ?: 0

    override suspend fun countThreeStarSongs(): Int =
        progressDao.countThreeStarSongs()

    override suspend fun countCompletedSongs(): Int =
        progressDao.countCompletedSongs()

    override suspend fun averageAccuracy(): Float =
        progressDao.averageAccuracy() ?: 0f

    override suspend fun isLessonCompleted(lessonId: Long): Boolean =
        progressDao.getForLesson(lessonId) != null

    override fun getAllProgress(): Flow<List<ProgressEntity>> =
        progressDao.getAll()
}
