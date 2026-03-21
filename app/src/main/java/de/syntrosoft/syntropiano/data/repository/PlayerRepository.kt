package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.domain.model.Achievement
import de.syntrosoft.syntropiano.domain.model.AchievementType
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getProfile(): Flow<PlayerProfile>
    suspend fun addXp(amount: Int)
    suspend fun updateStreak()
    suspend fun addPlayTime(minutes: Int)
    fun getAchievements(): Flow<List<Achievement>>
    suspend fun unlockAchievement(type: AchievementType)
    suspend fun updateAchievementProgress(type: AchievementType, progress: Float)
}
