package de.syntrosoft.syntropiano.domain.engine

class ScoreCalculator {

    fun calculateStars(accuracy: Float): Int = when {
        accuracy >= 0.95f -> 3
        accuracy >= 0.80f -> 2
        accuracy >= 0.60f -> 1
        else -> 0
    }

    fun calculateXp(accuracy: Float, stars: Int, isLevelTest: Boolean): Int {
        var xp = when {
            isLevelTest -> 200
            stars >= 3 -> 100
            else -> 50
        }
        if (accuracy >= 1.0f) xp += 50 // perfection bonus
        return xp
    }

    fun lessonXp(): Int = 50

    fun streakXp(): Int = 20

    fun streakBonusXp(streakDays: Int): Int =
        if (streakDays == 7) 150 else 0
}
