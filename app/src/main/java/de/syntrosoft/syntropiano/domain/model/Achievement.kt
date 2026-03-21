package de.syntrosoft.syntropiano.domain.model

data class Achievement(
    val type: AchievementType,
    val unlockedAt: Long? = null,
    val progress: Float = 0f,  // 0.0 - 1.0
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}
