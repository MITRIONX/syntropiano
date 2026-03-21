package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.data.db.dao.AchievementDao
import de.syntrosoft.syntropiano.data.db.dao.PlayerProfileDao
import de.syntrosoft.syntropiano.data.db.entity.AchievementEntity
import de.syntrosoft.syntropiano.data.db.entity.PlayerProfileEntity
import de.syntrosoft.syntropiano.domain.engine.XpCalculator
import de.syntrosoft.syntropiano.domain.model.Achievement
import de.syntrosoft.syntropiano.domain.model.AchievementType
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val profileDao: PlayerProfileDao,
    private val achievementDao: AchievementDao,
    private val xpCalculator: XpCalculator,
) : PlayerRepository {

    override fun getProfile(): Flow<PlayerProfile> =
        profileDao.get().map { it?.toDomain() ?: PlayerProfile() }

    override suspend fun addXp(amount: Int) {
        val current = profileDao.getOnce() ?: PlayerProfileEntity()
        val newTotalXp = current.totalXp + amount
        val newLevel = xpCalculator.levelForTotalXp(newTotalXp)
        profileDao.upsert(current.copy(totalXp = newTotalXp, currentLevel = newLevel))
    }

    override suspend fun updateStreak() {
        val current = profileDao.getOnce() ?: PlayerProfileEntity()
        val today = LocalDate.now(ZoneId.systemDefault())
        val lastPlayed = current.lastPlayedAt?.let {
            java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        val newStreak = when {
            lastPlayed == today -> current.currentStreak // already counted today
            lastPlayed == today.minusDays(1) -> current.currentStreak + 1
            else -> 1 // streak broken or first time
        }

        profileDao.upsert(
            current.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(current.longestStreak, newStreak),
                lastPlayedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun addPlayTime(minutes: Int) {
        val current = profileDao.getOnce() ?: PlayerProfileEntity()
        profileDao.upsert(current.copy(totalPlayTimeMinutes = current.totalPlayTimeMinutes + minutes))
    }

    override fun getAchievements(): Flow<List<Achievement>> =
        achievementDao.getAll().map { entities ->
            AchievementType.entries.map { type ->
                val entity = entities.find { it.type == type.name }
                Achievement(
                    type = type,
                    unlockedAt = entity?.unlockedAt,
                    progress = entity?.progress ?: 0f,
                )
            }
        }

    override suspend fun unlockAchievement(type: AchievementType) {
        val existing = achievementDao.getByType(type.name)
        if (existing?.unlockedAt != null) return // already unlocked
        achievementDao.upsert(
            AchievementEntity(type = type.name, unlockedAt = System.currentTimeMillis(), progress = 1f)
        )
    }

    override suspend fun updateAchievementProgress(type: AchievementType, progress: Float) {
        val existing = achievementDao.getByType(type.name)
        if (existing?.unlockedAt != null) return // already unlocked
        achievementDao.upsert(
            AchievementEntity(type = type.name, progress = progress.coerceIn(0f, 1f))
        )
    }

    private fun PlayerProfileEntity.toDomain() = PlayerProfile(
        totalXp = totalXp, currentLevel = currentLevel,
        currentStreak = currentStreak, longestStreak = longestStreak,
        lastPlayedAt = lastPlayedAt, totalPlayTimeMinutes = totalPlayTimeMinutes,
    )
}
