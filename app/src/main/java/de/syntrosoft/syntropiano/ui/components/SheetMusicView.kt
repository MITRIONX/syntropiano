package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.NoteResult
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.ui.theme.Blue400
import de.syntrosoft.syntropiano.ui.theme.Green400
import de.syntrosoft.syntropiano.ui.theme.Red400

@Composable
fun SheetMusicView(
    notes: List<Note>,
    noteResults: List<NoteResult>,
    currentNoteIndex: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F0E8))
    ) {
        val staffTop = size.height * 0.2f
        val staffBottom = size.height * 0.8f
        val lineSpacing = (staffBottom - staffTop) / 4

        // Draw 5 staff lines
        for (i in 0..4) {
            val y = staffTop + i * lineSpacing
            drawLine(Color(0xFF333333), Offset(0f, y), Offset(size.width, y), 1f)
        }

        if (notes.isEmpty()) return@Canvas

        // Draw notes
        val noteSpacing = size.width / (notes.size + 2).coerceAtLeast(1)
        val noteRadius = lineSpacing * 0.45f

        for ((i, note) in notes.withIndex()) {
            val x = (i + 1) * noteSpacing
            val pitch = note.toPitch() ?: continue
            val y = pitchToY(pitch, staffTop, lineSpacing)

            val color = when {
                i < noteResults.size && noteResults[i].status == NoteResult.Status.CORRECT -> Green400
                i < noteResults.size && noteResults[i].status == NoteResult.Status.WRONG -> Red400
                i == currentNoteIndex -> Blue400
                i > currentNoteIndex -> Color(0xFF999999)
                else -> Color(0xFF333333)
            }

            // Note head
            drawCircle(color, noteRadius, Offset(x, y))

            // Stem
            drawLine(color, Offset(x + noteRadius, y), Offset(x + noteRadius, y - lineSpacing * 2), 2f)
        }

        // Cursor line
        if (currentNoteIndex in notes.indices) {
            val cursorX = (currentNoteIndex + 1) * noteSpacing
            drawLine(
                Blue400.copy(alpha = 0.5f),
                Offset(cursorX, staffTop - 10),
                Offset(cursorX, staffBottom + 10),
                2f,
            )
        }
    }
}

// Map pitch to Y position on staff (treble clef, C4 = middle C = first ledger line below)
private fun pitchToY(pitch: Pitch, staffTop: Float, lineSpacing: Float): Float {
    // E4 = bottom line, F4 = first space, G4 = second line, etc.
    val noteOrder = mapOf(
        "C" to 0, "D" to 1, "E" to 2, "F" to 3, "G" to 4, "A" to 5, "B" to 6,
    )
    val noteName = pitch.displayName.replace("#", "").dropLast(1)
    val octave = pitch.displayName.last().digitToInt()
    val notePos = noteOrder[noteName] ?: 0

    // E4 is on the bottom staff line (staffTop + 4 * lineSpacing)
    // Each step up = half a lineSpacing
    val e4Pos = staffTop + 4 * lineSpacing
    val stepsAboveE4 = (octave - 4) * 7 + (notePos - 2)
    return e4Pos - stepsAboveE4 * (lineSpacing / 2)
}
