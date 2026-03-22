package de.syntrosoft.syntropiano.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.ui.theme.Blue400
import de.syntrosoft.syntropiano.ui.theme.Green400
import de.syntrosoft.syntropiano.ui.theme.Orange400
import de.syntrosoft.syntropiano.ui.theme.Red400
import kotlinx.coroutines.delay

private val Purple400 = Color(0xFFAB47BC)

@Composable
fun KeyboardView(
    highlightPitch: Pitch? = null,
    detectedPitch: Pitch? = null,
    isCorrect: Boolean? = null,
    playbackPitches: Map<Pitch, String> = emptyMap(), // Pitch -> "L"/"R"
    playbackNoteIndex: Int = -1,
    startOctave: Int = 3,
    octaves: Int = 2,
    modifier: Modifier = Modifier,
) {
    // Track which keys are newly struck vs sustained from previous beat
    var prevPitchSet by remember { mutableStateOf(emptySet<Pitch>()) }
    var newlyStruck by remember { mutableStateOf(emptySet<Pitch>()) }

    var strikeFlash by remember { mutableFloatStateOf(1f) }
    val animatedFlash by animateFloatAsState(
        targetValue = strikeFlash,
        animationSpec = tween(180),
        label = "strike",
    )
    LaunchedEffect(playbackNoteIndex) {
        if (playbackNoteIndex >= 0) {
            val currentKeys = playbackPitches.keys
            newlyStruck = if (currentKeys == prevPitchSet && currentKeys.isNotEmpty()) {
                currentKeys // same pitches re-struck → flash all
            } else {
                currentKeys - prevPitchSet // only flash new keys
            }
            prevPitchSet = currentKeys
            strikeFlash = 0.2f
            delay(60)
            strikeFlash = 1f
        } else {
            prevPitchSet = emptySet()
            newlyStruck = emptySet()
        }
    }

    val whiteNotes = listOf("C", "D", "E", "F", "G", "A", "B")
    val blackNotePositions = mapOf("C" to 0, "D" to 1, "F" to 3, "G" to 4, "A" to 5)

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val totalWhiteKeys = whiteNotes.size * octaves
        val whiteKeyWidth = size.width / totalWhiteKeys
        val whiteKeyHeight = size.height
        val blackKeyWidth = whiteKeyWidth * 0.6f
        val blackKeyHeight = whiteKeyHeight * 0.6f

        // Draw white keys
        for (octave in 0 until octaves) {
            for ((i, noteName) in whiteNotes.withIndex()) {
                val keyIndex = octave * whiteNotes.size + i
                val x = keyIndex * whiteKeyWidth
                val pitchName = "$noteName${startOctave + octave}"
                val pitch = Pitch.fromName(pitchName)

                val isPlayback = pitch != null && pitch in playbackPitches
                val keyColor = when {
                    pitch == detectedPitch && isCorrect == true -> Green400
                    pitch == detectedPitch && isCorrect == false -> Red400
                    isPlayback -> {
                        val base = if (playbackPitches[pitch] == "L") Purple400 else Orange400
                        if (pitch in newlyStruck) lerp(Color.White, base, animatedFlash) else base
                    }
                    pitch == highlightPitch -> Blue400
                    else -> Color.White
                }

                drawRoundRect(
                    color = keyColor,
                    topLeft = Offset(x + 1, 0f),
                    size = Size(whiteKeyWidth - 2, whiteKeyHeight),
                    cornerRadius = CornerRadius(0f, 0f),
                )
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.3f),
                    topLeft = Offset(x + 1, 0f),
                    size = Size(whiteKeyWidth - 2, whiteKeyHeight),
                    cornerRadius = CornerRadius(0f, 0f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1f),
                )

                // Oktav-Label auf C-Tasten
                if (noteName == "C") {
                    val label = "C${startOctave + octave}"
                    val textResult = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(
                            x + (whiteKeyWidth - textResult.size.width) / 2,
                            whiteKeyHeight - textResult.size.height - 4f,
                        ),
                    )
                }
            }
        }

        // Draw black keys
        for (octave in 0 until octaves) {
            for ((noteName, pos) in blackNotePositions) {
                val keyIndex = octave * whiteNotes.size + pos
                val x = (keyIndex + 1) * whiteKeyWidth - blackKeyWidth / 2
                val sharpName = "$noteName#${startOctave + octave}"
                val pitch = Pitch.fromName(sharpName)

                val isPlaybackBlack = pitch != null && pitch in playbackPitches
                val keyColor = when {
                    pitch == detectedPitch && isCorrect == true -> Green400
                    pitch == detectedPitch && isCorrect == false -> Red400
                    isPlaybackBlack -> {
                        val base = if (playbackPitches[pitch] == "L") Purple400 else Orange400
                        if (pitch in newlyStruck) lerp(Color.White, base, animatedFlash) else base
                    }
                    pitch == highlightPitch -> Blue400
                    else -> Color(0xFF1A1A1A)
                }

                drawRoundRect(
                    color = keyColor,
                    topLeft = Offset(x, 0f),
                    size = Size(blackKeyWidth, blackKeyHeight),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
    }
}
