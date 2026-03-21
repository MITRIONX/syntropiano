package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.ui.theme.DarkSurface
import de.syntrosoft.syntropiano.ui.theme.Green400
import de.syntrosoft.syntropiano.ui.theme.Red400

data class QuizQuestion(
    val correctPitch: Pitch,
    val options: List<Pitch>,
)

@Composable
fun NoteQuiz(
    question: QuizQuestion,
    onAnswer: (Pitch, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedAnswer by remember(question) { mutableStateOf<Pitch?>(null) }
    val isAnswered = selectedAnswer != null

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Welche Note ist das?",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show the note on a mini staff
        SheetMusicView(
            notes = listOf(
                Note(
                    pitch = question.correctPitch.displayName,
                    duration = 1f,
                    beat = 0f,
                )
            ),
            noteResults = emptyList(),
            currentNoteIndex = 0,
            modifier = Modifier.height(100.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Answer options (2x2 grid)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in question.options.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (option in row) {
                        val bgColor = when {
                            !isAnswered -> DarkSurface
                            option == question.correctPitch -> Green400.copy(alpha = 0.3f)
                            option == selectedAnswer -> Red400.copy(alpha = 0.3f)
                            else -> DarkSurface
                        }
                        val borderColor = when {
                            !isAnswered -> Color.Gray
                            option == question.correctPitch -> Green400
                            option == selectedAnswer -> Red400
                            else -> Color.Gray
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable(enabled = !isAnswered) {
                                    selectedAnswer = option
                                    onAnswer(option, option == question.correctPitch)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(borderColor)
                            ),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    option.displayName,
                                    fontSize = 20.sp,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
