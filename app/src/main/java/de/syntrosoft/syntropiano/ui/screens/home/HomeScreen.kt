package de.syntrosoft.syntropiano.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToLearn: () -> Unit,
    onNavigateToSongList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "SyntroPiano",
            style = MaterialTheme.typography.headlineLarge,
            color = Blue400,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${profile.rank.title} · Level ${profile.currentLevel}",
            style = MaterialTheme.typography.bodyMedium,
            color = Orange400,
        )

        if (profile.currentStreak > 0) {
            Text(
                text = "\uD83D\uDD25 ${profile.currentStreak} Tage Streak",
                style = MaterialTheme.typography.bodySmall,
                color = Red400,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HomeCard(
            title = "Lernen",
            subtitle = "Strukturierter Kurs",
            gradientColors = listOf(Blue400, Color(0xFF1565C0)),
            onClick = onNavigateToLearn,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeCard(
            title = "Spielen",
            subtitle = "Freies Üben",
            gradientColors = listOf(Green400, Color(0xFF2E7D32)),
            onClick = onNavigateToSongList,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeCard(
            title = "Profil",
            subtitle = "Fortschritt & Achievements",
            gradientColors = listOf(Orange400, Color(0xFFE65100)),
            onClick = onNavigateToProfile,
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
