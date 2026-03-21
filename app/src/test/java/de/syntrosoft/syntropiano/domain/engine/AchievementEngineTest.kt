package de.syntrosoft.syntropiano.domain.engine

import de.syntrosoft.syntropiano.domain.model.AchievementType
import org.junit.Assert.*
import org.junit.Test

class AchievementEngineTest {
    private val engine = AchievementEngine()

    @Test
    fun `FIRST_NOTE unlocks when first correct note is played`() {
        val result = engine.check(
            type = AchievementType.FIRST_NOTE,
            totalCorrectNotes = 1,
        )
        assertTrue(result)
    }

    @Test
    fun `FIRST_NOTE does not unlock with 0 correct notes`() {
        assertFalse(engine.check(AchievementType.FIRST_NOTE, totalCorrectNotes = 0))
    }

    @Test
    fun `STREAK_7 unlocks at 7 day streak`() {
        assertTrue(engine.check(AchievementType.STREAK_7, currentStreak = 7))
    }

    @Test
    fun `STREAK_7 does not unlock at 6 days`() {
        assertFalse(engine.check(AchievementType.STREAK_7, currentStreak = 6))
    }

    @Test
    fun `STAR_COLLECTOR unlocks at 10 three-star songs`() {
        assertTrue(engine.check(AchievementType.STAR_COLLECTOR, threeStarSongs = 10))
    }

    @Test
    fun `PERFECTIONIST unlocks with perfect accuracy`() {
        assertTrue(engine.check(AchievementType.PERFECTIONIST, hasPerfectSong = true))
    }
}
