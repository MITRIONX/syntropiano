package de.syntrosoft.syntropiano.ui.screens.play

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.components.StarRating
import de.syntrosoft.syntropiano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    onBack: () -> Unit,
    onSongSelected: (songId: Long) -> Unit,
    viewModel: SongListViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lieder") },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(songs, key = { it.song.id }) { songWithStars ->
                SongCard(songWithStars, onClick = { onSongSelected(songWithStars.song.id) })
            }
        }
    }
}

@Composable
private fun SongCard(songWithStars: SongWithStars, onClick: () -> Unit) {
    val song = songWithStars.song
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                if (song.artist.isNotBlank()) {
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(
                    "Schwierigkeit: ${"★".repeat(song.difficulty)}${"☆".repeat(5 - song.difficulty)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Orange400,
                )
            }
            if (songWithStars.bestStars > 0) {
                StarRating(stars = songWithStars.bestStars)
            }
        }
    }
}
