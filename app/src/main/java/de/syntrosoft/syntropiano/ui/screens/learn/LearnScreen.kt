package de.syntrosoft.syntropiano.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.theme.*

private val levelColors = listOf(Blue400, Green400, Orange400, Red400, Purple300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val levels by viewModel.levels.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lernen") },
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
            itemsIndexed(levels) { index, level ->
                LevelCard(
                    level = level,
                    color = levelColors[index % levelColors.size],
                    onClick = { if (level.isUnlocked) onLevelSelected(level.level) },
                )
            }
        }
    }
}

@Composable
private fun LevelCard(level: LevelInfo, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = level.isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (level.isUnlocked) color else Color.Gray),
                contentAlignment = Alignment.Center,
            ) {
                if (level.isUnlocked) {
                    Text("${level.level}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(level.title, style = MaterialTheme.typography.titleLarge, color = if (level.isUnlocked) Color.White else Color.Gray)
                Text(level.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            if (level.isCompleted) {
                Text("\u2705", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
