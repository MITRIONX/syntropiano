package de.syntrosoft.syntropiano.domain.engine

import org.junit.Assert.*
import org.junit.Test

class ScoreCalculatorTest {
    private val calc = ScoreCalculator()

    @Test
    fun `95 percent accuracy gives 3 stars`() {
        assertEquals(3, calc.calculateStars(0.95f))
    }

    @Test
    fun `80 percent accuracy gives 2 stars`() {
        assertEquals(2, calc.calculateStars(0.80f))
    }

    @Test
    fun `60 percent accuracy gives 1 star`() {
        assertEquals(1, calc.calculateStars(0.60f))
    }

    @Test
    fun `below 60 percent gives 0 stars`() {
        assertEquals(0, calc.calculateStars(0.59f))
    }

    @Test
    fun `100 percent gives 3 stars plus perfection bonus`() {
        val xp = calc.calculateXp(accuracy = 1.0f, stars = 3, isLevelTest = false)
        assertEquals(100 + 50, xp) // 3-star song + perfection bonus
    }

    @Test
    fun `level test gives 200 XP`() {
        val xp = calc.calculateXp(accuracy = 0.85f, stars = 2, isLevelTest = true)
        assertEquals(200, xp)
    }

    @Test
    fun `3 star song gives 100 XP`() {
        val xp = calc.calculateXp(accuracy = 0.96f, stars = 3, isLevelTest = false)
        assertEquals(100, xp)
    }

    @Test
    fun `lesson completion gives 50 XP`() {
        assertEquals(50, calc.lessonXp())
    }

    @Test
    fun `daily streak gives 20 XP`() {
        assertEquals(20, calc.streakXp())
    }

    @Test
    fun `7 day streak bonus gives 150 XP`() {
        assertEquals(150, calc.streakBonusXp(7))
    }

    @Test
    fun `non 7 day streak gives no bonus`() {
        assertEquals(0, calc.streakBonusXp(6))
        assertEquals(0, calc.streakBonusXp(8))
    }
}
