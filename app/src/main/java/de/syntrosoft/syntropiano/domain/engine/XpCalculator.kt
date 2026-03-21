package de.syntrosoft.syntropiano.domain.engine

class XpCalculator {

    fun xpForLevel(level: Int): Int = level * 300

    fun levelForTotalXp(totalXp: Int): Int {
        var accumulated = 0
        var level = 1
        while (true) {
            val needed = xpForLevel(level)
            if (accumulated + needed > totalXp) return level
            accumulated += needed
            level++
        }
    }

    fun xpProgressInLevel(totalXp: Int): Pair<Int, Int> {
        var accumulated = 0
        var level = 1
        while (true) {
            val needed = xpForLevel(level)
            if (accumulated + needed > totalXp) {
                return (totalXp - accumulated) to needed
            }
            accumulated += needed
            level++
        }
    }
}
