package de.syntrosoft.syntropiano.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.components.AchievementCard
import de.syntrosoft.syntropiano.ui.components.StreakBadge
import de.syntrosoft.syntropiano.ui.components.XpBar
import de.syntrosoft.syntropiano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val (xpInLevel, xpNeeded) = viewModel.xpCalculator.xpProgressInLevel(profile.totalXp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Avatar & Level
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Blue400, Purple300))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\uD83C\uDFB9", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(profile.rank.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        StreakBadge(profile.currentStreak)
                    }
                }
            }

            // XP Bar
            item {
                XpBar(currentXp = xpInLevel, requiredXp = xpNeeded, level = profile.currentLevel)
            }

            // Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard("${profile.currentStreak}", "Streak", Red400, Modifier.weight(1f))
                    StatCard("${stats.completedSongs}", "Lieder", Green400, Modifier.weight(1f))
                    StatCard("${(stats.averageAccuracy * 100).toInt()}%", "Genauigkeit", Orange400, Modifier.weight(1f))
                    StatCard("${profile.totalPlayTimeMinutes / 60}h", "Übungszeit", Purple300, Modifier.weight(1f))
                }
            }

            // Achievements
            item {
                Text("Achievements", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(((achievements.size + 1) / 2 * 120).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(achievements) { achievement ->
                        AchievementCard(achievement)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
