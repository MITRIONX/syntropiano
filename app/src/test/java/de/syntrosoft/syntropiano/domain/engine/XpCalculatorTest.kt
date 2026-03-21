package de.syntrosoft.syntropiano.domain.engine

import org.junit.Assert.*
import org.junit.Test

class XpCalculatorTest {
    private val calc = XpCalculator()

    @Test
    fun `level 1 requires 300 XP`() {
        assertEquals(300, calc.xpForLevel(1))
    }

    @Test
    fun `level 10 requires 3000 XP`() {
        assertEquals(3000, calc.xpForLevel(10))
    }

    @Test
    fun `0 XP is level 1`() {
        assertEquals(1, calc.levelForTotalXp(0))
    }

    @Test
    fun `300 XP is level 2`() {
        assertEquals(2, calc.levelForTotalXp(300))
    }

    @Test
    fun `299 XP is still level 1`() {
        assertEquals(1, calc.levelForTotalXp(299))
    }

    @Test
    fun `900 XP is level 3`() {
        // Level 1 = 300, Level 2 = 600 → total 900 to reach level 3
        assertEquals(3, calc.levelForTotalXp(900))
    }
}
