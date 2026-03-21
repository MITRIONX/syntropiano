package de.syntrosoft.syntropiano.domain.model

enum class Rank(val title: String, val minLevel: Int) {
    BEGINNER("Anfänger", 1),
    APPRENTICE("Klavier-Lehrling", 6),
    MELODY_MASTER("Melodie-Meister", 11),
    VIRTUOSO("Virtuose", 21),
    LEGEND("Piano-Legende", 36),
}

data class PlayerProfile(
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPlayedAt: Long? = null,
    val totalPlayTimeMinutes: Int = 0,
) {
    val rank: Rank get() = Rank.entries.last { currentLevel >= it.minLevel }
    val xpForNextLevel: Int get() = (currentLevel + 1) * 300
    val xpInCurrentLevel: Int get() {
        val xpForCurrent = (1..currentLevel).sumOf { it * 300 }
        return totalXp - xpForCurrent + (currentLevel * 300)
    }
}
