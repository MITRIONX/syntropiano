package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syntrosoft.syntropiano.ui.theme.Red400

@Composable
fun StreakBadge(streakDays: Int, modifier: Modifier = Modifier) {
    if (streakDays <= 0) return
    Row(
        modifier = modifier
            .background(Red400.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("\uD83D\uDD25", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "$streakDays Tage",
            style = MaterialTheme.typography.labelSmall,
            color = Red400,
        )
    }
}
