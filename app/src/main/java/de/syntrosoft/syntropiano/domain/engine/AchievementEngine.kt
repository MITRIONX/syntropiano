package de.syntrosoft.syntropiano.domain.engine

import de.syntrosoft.syntropiano.domain.model.AchievementType

class AchievementEngine {

    fun check(
        type: AchievementType,
        totalCorrectNotes: Int = 0,
        currentStreak: Int = 0,
        threeStarSongs: Int = 0,
        hasBothHandsSong: Boolean = false,
        hasPerfectSong: Boolean = false,
        allLevelsCompleted: Boolean = false,
    ): Boolean = when (type) {
        AchievementType.FIRST_NOTE -> totalCorrectNotes >= 1
        AchievementType.STREAK_7 -> currentStreak >= 7
        AchievementType.STAR_COLLECTOR -> threeStarSongs >= 10
        AchievementType.BOTH_HANDS -> hasBothHandsSong
        AchievementType.PERFECTIONIST -> hasPerfectSong
        AchievementType.MASTER -> allLevelsCompleted
    }
}
