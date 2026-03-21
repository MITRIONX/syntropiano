package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.ui.theme.Blue400
import de.syntrosoft.syntropiano.ui.theme.Green400
import de.syntrosoft.syntropiano.ui.theme.Red400

@Composable
fun KeyboardView(
    highlightPitch: Pitch? = null,
    detectedPitch: Pitch? = null,
    isCorrect: Boolean? = null,
    startOctave: Int = 3,
    octaves: Int = 2,
    modifier: Modifier = Modifier,
) {
    val whiteNotes = listOf("C", "D", "E", "F", "G", "A", "B")
    val blackNotePositions = mapOf("C" to 0, "D" to 1, "F" to 3, "G" to 4, "A" to 5) // after which white key

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

                val keyColor = when {
                    pitch == highlightPitch -> Blue400
                    pitch == detectedPitch && isCorrect == true -> Green400
                    pitch == detectedPitch && isCorrect == false -> Red400
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
            }
        }

        // Draw black keys
        for (octave in 0 until octaves) {
            for ((noteName, pos) in blackNotePositions) {
                val keyIndex = octave * whiteNotes.size + pos
                val x = (keyIndex + 1) * whiteKeyWidth - blackKeyWidth / 2
                val sharpName = "$noteName#${startOctave + octave}"
                val pitch = Pitch.fromName(sharpName)

                val keyColor = when {
                    pitch == highlightPitch -> Blue400
                    pitch == detectedPitch && isCorrect == true -> Green400
                    pitch == detectedPitch && isCorrect == false -> Red400
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
