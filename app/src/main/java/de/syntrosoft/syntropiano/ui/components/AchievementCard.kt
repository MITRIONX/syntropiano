package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syntrosoft.syntropiano.domain.model.Achievement

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val alpha = if (achievement.isUnlocked) 1f else 0.4f
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = achievementEmoji(achievement.type.name),
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = achievement.type.title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = alpha),
        )
        Text(
            text = achievement.type.description,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = alpha),
        )
    }
}

private fun achievementEmoji(type: String): String = when (type) {
    "FIRST_NOTE" -> "\uD83C\uDFB5"
    "STREAK_7" -> "\uD83D\uDD25"
    "STAR_COLLECTOR" -> "\u2B50"
    "BOTH_HANDS" -> "\uD83C\uDFB9"
    "PERFECTIONIST" -> "\uD83D\uDCAF"
    "MASTER" -> "\uD83C\uDFC6"
    else -> "\uD83C\uDFB5"
}
