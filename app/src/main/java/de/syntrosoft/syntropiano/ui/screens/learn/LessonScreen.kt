package de.syntrosoft.syntropiano.ui.screens.learn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.components.NoteQuiz
import de.syntrosoft.syntropiano.ui.theme.Blue400
import de.syntrosoft.syntropiano.ui.theme.DarkSurface
import de.syntrosoft.syntropiano.ui.theme.Green400
import de.syntrosoft.syntropiano.ui.theme.Orange400
import de.syntrosoft.syntropiano.ui.theme.Red400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    onBack: () -> Unit,
    onPlaySong: (songId: Long, mode: String) -> Unit,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Level ${state.level}") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when (state.currentStep) {
                LessonStep.THEORY -> TheoryContent(state, onNext = { viewModel.advanceToQuiz() })
                LessonStep.QUIZ -> QuizContent(state, viewModel)
                LessonStep.SONG_PRACTICE -> SongPracticeContent(state, onPlaySong, onNext = { viewModel.advanceToTest() })
                LessonStep.TEST -> TestContent(state, onPlaySong)
                LessonStep.COMPLETED -> CompletedContent(state, onBack)
            }
        }
    }
}

@Composable
private fun TheoryContent(state: LessonState, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(state.theoryTitle, style = MaterialTheme.typography.headlineMedium, color = Blue400)
        Spacer(modifier = Modifier.height(16.dp))
        Text(state.theoryContent, style = MaterialTheme.typography.bodyLarge, color = Color.White, lineHeight = 24.sp)
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Weiter zum Quiz")
        }
    }
}

@Composable
private fun QuizContent(state: LessonState, viewModel: LessonViewModel) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Quiz: ${state.currentQuizIndex + 1}/${state.quizQuestions.size}",
            style = MaterialTheme.typography.titleLarge,
            color = Orange400,
        )
        Text(
            "${state.quizCorrect} richtig",
            style = MaterialTheme.typography.bodySmall,
            color = Green400,
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (state.currentQuizIndex < state.quizQuestions.size) {
            NoteQuiz(
                question = state.quizQuestions[state.currentQuizIndex],
                onAnswer = { _, correct -> viewModel.onQuizAnswer(correct) },
            )
        }
    }
}

@Composable
private fun SongPracticeContent(
    state: LessonState,
    onPlaySong: (Long, String) -> Unit,
    onNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Übungslieder", style = MaterialTheme.typography.headlineMedium, color = Green400)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Übe diese Lieder bevor du den Level-Test machst:", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        state.levelSongs.forEach { song ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                onClick = { onPlaySong(song.id, "PRACTICE") },
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(song.title, color = Color.White, modifier = Modifier.weight(1f))
                    Text("Üben →", color = Blue400, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Level-Test starten")
        }
    }
}

@Composable
private fun TestContent(state: LessonState, onPlaySong: (Long, String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Level-Test", style = MaterialTheme.typography.headlineLarge, color = Red400)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Spiele ein Lied mit mindestens 70% Genauigkeit", color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        val testSong = state.levelSongs.firstOrNull()
        if (testSong != null) {
            Button(
                onClick = { onPlaySong(testSong.id, "PERFORMANCE") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red400),
            ) {
                Text("${testSong.title} spielen")
            }
        } else {
            Text("Keine Lieder für dieses Level verfügbar", color = Color.Gray)
        }
    }
}

@Composable
private fun CompletedContent(state: LessonState, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (state.testPassed) {
            Text("🎉", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Level bestanden!", style = MaterialTheme.typography.headlineLarge, color = Green400)
            Text("+200 XP", style = MaterialTheme.typography.titleLarge, color = Orange400)
        } else {
            Text("Knapp daneben!", style = MaterialTheme.typography.headlineLarge, color = Red400)
            Text("Du brauchst mindestens 70% Genauigkeit.", color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.testPassed) "Weiter" else "Nochmal versuchen")
        }
    }
}
