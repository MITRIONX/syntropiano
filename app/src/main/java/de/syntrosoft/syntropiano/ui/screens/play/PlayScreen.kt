package de.syntrosoft.syntropiano.ui.screens.play

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.components.KeyboardView
import de.syntrosoft.syntropiano.ui.components.SheetMusicView
import de.syntrosoft.syntropiano.ui.components.StarRating
import de.syntrosoft.syntropiano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    onBack: () -> Unit,
    viewModel: PlayViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val showResult by viewModel.showResult.collectAsStateWithLifecycle()
    val isListening by viewModel.audioCaptureService.isListening.collectAsStateWithLifecycle()
    val detectedPitch by viewModel.audioCaptureService.detectedPitch.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.song?.title ?: "Laden...") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopListening()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val currentSession = session
        if (currentSession == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (showResult) {
            ResultScreen(
                session = currentSession,
                onRetry = { viewModel.retry() },
                onBack = onBack,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // Song info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${currentSession.song.bpm} BPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                Text(
                    "${currentSession.correctNotes}/${currentSession.totalNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Orange400,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sheet music
            SheetMusicView(
                notes = currentSession.song.notes,
                noteResults = currentSession.noteResults,
                currentNoteIndex = currentSession.currentNoteIndex,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Keyboard visualization
            val currentNote = currentSession.song.notes.getOrNull(currentSession.currentNoteIndex)
            val highlightPitch = currentNote?.toPitch()
            val lastResult = currentSession.noteResults.lastOrNull()

            KeyboardView(
                highlightPitch = highlightPitch,
                detectedPitch = detectedPitch,
                isCorrect = lastResult?.let { it.status == de.syntrosoft.syntropiano.domain.model.NoteResult.Status.CORRECT },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Microphone status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isListening) Color(0xFF1A2E1A) else DarkSurface,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (isListening) "\uD83C\uDFA4 Mikrofon aktiv" else "\uD83C\uDFA4 Mikrofon aus",
                        color = if (isListening) Green400 else Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (detectedPitch != null) {
                        Text(
                            "Erkannt: ${detectedPitch?.displayName}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start/Stop button
            Button(
                onClick = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else if (viewModel.audioCaptureService.hasPermission(context)) {
                        viewModel.startListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Red400 else Green400,
                ),
            ) {
                Text(
                    if (isListening) "Stopp" else "Start",
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(
    session: de.syntrosoft.syntropiano.domain.model.PlaySession,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Geschafft!", style = MaterialTheme.typography.headlineLarge, color = Color.White)

        Spacer(modifier = Modifier.height(24.dp))

        StarRating(stars = session.stars)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "${(session.accuracy * 100).toInt()}% Genauigkeit",
            style = MaterialTheme.typography.titleLarge,
            color = Orange400,
        )

        Text(
            "${session.correctNotes} von ${session.totalNotes} Noten richtig",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Nochmal")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Zurück")
        }
    }
}
