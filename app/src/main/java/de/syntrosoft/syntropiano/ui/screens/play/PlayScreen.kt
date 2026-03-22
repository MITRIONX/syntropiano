package de.syntrosoft.syntropiano.ui.screens.play

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val isDemoPlaying by viewModel.isDemoPlaying.collectAsStateWithLifecycle()
    val isPlayAlongActive by viewModel.isPlayAlongActive.collectAsStateWithLifecycle()
    val playbackNoteIndex by viewModel.playbackNoteIndex.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val playbackVolume by viewModel.playbackVolume.collectAsStateWithLifecycle()
    val enabledHands by viewModel.enabledHands.collectAsStateWithLifecycle()
    val tempoFactor by viewModel.tempoFactor.collectAsStateWithLifecycle()
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
                        viewModel.stopDemo()
                        viewModel.stopPlayAlong()
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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
                playbackProgress = if (playbackProgress >= 0f) playbackProgress else null,
                enabledHands = enabledHands,
                onSeek = if (isDemoPlaying || isPlayAlongActive) { index ->
                    viewModel.seekToNote(index)
                } else null,
                timeSignature = currentSession.song.timeSignature,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Keyboard visualization
            var showHint by remember { mutableStateOf(false) }
            val currentNote = currentSession.song.notes.getOrNull(currentSession.currentNoteIndex)
            val highlightPitch = if (showHint) currentNote?.toPitch() else null
            val lastResult = currentSession.noteResults.lastOrNull()

            // Beim Mitspielen alle Noten auf Keyboard zeigen (zum Lernen), bei Demo nur gewählte Hände
            val playbackHands = if (isPlayAlongActive) {
                setOf("L", "R")
            } else {
                enabledHands
            }

            // Alle gleichzeitig klingenden Noten finden (nur gespielte Hände)
            val playbackIdx = playbackNoteIndex
            val playbackPitches = if (playbackIdx >= 0) {
                val currentBeat = currentSession.song.notes.getOrNull(playbackIdx)?.beat
                if (currentBeat != null) {
                    currentSession.song.notes
                        .filter { it.beat <= currentBeat && currentBeat < it.beat + it.duration && it.hand in playbackHands }
                        .mapNotNull { note -> note.toPitch()?.let { it to note.hand } }
                        .toMap()
                } else emptyMap()
            } else emptyMap()

            // Oktavbereich dynamisch aus Song-Noten berechnen
            val songOctaves = currentSession.song.notes.mapNotNull { note ->
                note.pitch.lastOrNull()?.digitToIntOrNull()
            }
            val minOctave = (songOctaves.minOrNull() ?: 3)
            val maxOctave = (songOctaves.maxOrNull() ?: 4)
            val keyboardStartOctave = minOctave
            val keyboardOctaves = (maxOctave - minOctave + 1).coerceAtLeast(2)

            KeyboardView(
                highlightPitch = highlightPitch,
                detectedPitch = detectedPitch,
                isCorrect = lastResult?.let { it.status == de.syntrosoft.syntropiano.domain.model.NoteResult.Status.CORRECT },
                playbackPitches = playbackPitches,
                playbackNoteIndex = playbackNoteIndex,
                startOctave = keyboardStartOctave,
                octaves = keyboardOctaves,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Volume slider + Tempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "\uD83D\uDD0A",
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = playbackVolume,
                    onValueChange = { viewModel.setPlaybackVolume(it) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Orange400,
                        activeTrackColor = Orange400,
                    ),
                )
            }

            // Tempo-Auswahl
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Tempo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                listOf(0.25f to "25%", 0.5f to "50%", 0.75f to "75%", 1.0f to "100%").forEach { (factor, label) ->
                    FilterChip(
                        selected = tempoFactor == factor,
                        onClick = { viewModel.setTempoFactor(factor) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Orange400,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hand-Auswahl
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hand:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                FilterChip(
                    selected = enabledHands == setOf("L"),
                    onClick = { viewModel.toggleHand("L") },
                    label = { Text("Links", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFAB47BC),
                        selectedLabelColor = Color.White,
                    ),
                )
                FilterChip(
                    selected = enabledHands == setOf("L", "R"),
                    onClick = {
                        if (enabledHands != setOf("L", "R")) viewModel.toggleHand("L")
                    },
                    label = { Text("Beide", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Orange400,
                        selectedLabelColor = Color.White,
                    ),
                )
                FilterChip(
                    selected = enabledHands == setOf("R"),
                    onClick = { viewModel.toggleHand("R") },
                    label = { Text("Rechts", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Orange400,
                        selectedLabelColor = Color.White,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                FilterChip(
                    selected = showHint,
                    onClick = { showHint = !showHint },
                    label = { Text("Hilfe", style = MaterialTheme.typography.bodySmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Blue400,
                        selectedLabelColor = Color.White,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Demo / Mitspielen buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (isDemoPlaying) viewModel.stopDemo() else viewModel.playDemo()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isPlayAlongActive && !isListening,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDemoPlaying) Red400 else Orange400,
                    ),
                ) {
                    Text(if (isDemoPlaying) "Demo stopp" else "Demo")
                }

                OutlinedButton(
                    onClick = {
                        if (isPlayAlongActive) {
                            viewModel.stopPlayAlong()
                        } else if (viewModel.audioCaptureService.hasPermission(context)) {
                            viewModel.startPlayAlong()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isPlayAlongActive || (!isDemoPlaying && !isListening),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isPlayAlongActive) Red400 else Blue400,
                    ),
                ) {
                    Text(if (isPlayAlongActive) "Mitspielen stopp" else "Mitspielen")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                enabled = !isDemoPlaying && !isPlayAlongActive,
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
