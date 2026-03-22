package de.syntrosoft.syntropiano.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.NoteResult
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.ui.theme.Blue400
import de.syntrosoft.syntropiano.ui.theme.Green400
import de.syntrosoft.syntropiano.ui.theme.Orange400
import de.syntrosoft.syntropiano.ui.theme.Red400
import kotlin.math.roundToInt

private val Purple400 = Color(0xFFAB47BC)
private const val VISIBLE_SLOTS = 9

/** Notes grouped by beat position (chord = multiple notes at same beat) */
private data class NoteSlot(
    val beat: Float,
    val maxDuration: Float,
    val entries: List<Pair<Int, Note>>, // (originalNoteIndex, note)
)

private data class SlotLayout(
    val slots: List<NoteSlot>,
    val noteToSlot: Map<Int, Int>,
)

private fun computeSlotLayout(notes: List<Note>): SlotLayout {
    if (notes.isEmpty()) return SlotLayout(emptyList(), emptyMap())

    val grouped = notes.withIndex()
        .groupBy { it.value.beat }
        .entries
        .sortedBy { it.key }

    val slots = grouped.map { (beat, indexedNotes) ->
        NoteSlot(
            beat = beat,
            maxDuration = indexedNotes.maxOf { it.value.duration },
            entries = indexedNotes.map { it.index to it.value },
        )
    }

    val noteToSlot = mutableMapOf<Int, Int>()
    slots.forEachIndexed { slotIdx, slot ->
        slot.entries.forEach { (noteIdx, _) ->
            noteToSlot[noteIdx] = slotIdx
        }
    }

    return SlotLayout(slots, noteToSlot)
}

@Composable
fun SheetMusicView(
    notes: List<Note>,
    noteResults: List<NoteResult>,
    currentNoteIndex: Int,
    modifier: Modifier = Modifier,
    playbackProgress: Float? = null,
    enabledHands: Set<String> = setOf("L", "R"),
    onSeek: ((Int) -> Unit)? = null,
    timeSignature: String = "4/4",
) {
    val layout = remember(notes) { computeSlotLayout(notes) }
    val slots = layout.slots
    val noteToSlot = layout.noteToSlot

    // Convert note-index-based progress to slot-index-based progress
    fun noteToSlotProgress(noteProgress: Float): Float {
        if (slots.isEmpty()) return 0f
        val noteIdx = noteProgress.toInt().coerceIn(0, notes.lastIndex.coerceAtLeast(0))
        val frac = noteProgress - noteIdx
        val slotIdx = noteToSlot[noteIdx] ?: 0
        if (noteIdx >= notes.lastIndex) return slotIdx.toFloat()
        val nextSlotIdx = noteToSlot[noteIdx + 1] ?: slotIdx
        return if (nextSlotIdx == slotIdx) slotIdx.toFloat()
        else slotIdx + frac * (nextSlotIdx - slotIdx)
    }

    // Page-based scrolling: notes stay still until cursor reaches end of page
    val slotsPerPage = (VISIBLE_SLOTS - 2).coerceAtLeast(1)
    val activeSlotIdx = when {
        playbackProgress != null && playbackProgress >= 0f -> noteToSlotProgress(playbackProgress).toInt()
        currentNoteIndex >= 0 -> noteToSlot[currentNoteIndex] ?: 0
        else -> 0
    }
    val currentPage = activeSlotIdx / slotsPerPage
    val pageStartSlot = (currentPage * slotsPerPage).toFloat()

    val pitchRange = remember(notes) {
        if (notes.isEmpty()) return@remember 0 to 0
        val steps = notes.mapNotNull { note ->
            note.toPitch()?.let { pitchToSteps(it) }
        }
        if (steps.isEmpty()) 0 to 0
        else steps.min() to steps.max()
    }

    // Animate page turn (notes stay still within a page)
    val targetScrollSlot by animateFloatAsState(
        targetValue = pageStartSlot,
        animationSpec = tween(durationMillis = 300),
        label = "sheetScroll",
    )

    val textMeasurer = rememberTextMeasurer()
    val slotSpacingState = remember { mutableFloatStateOf(0f) }
    val scrollOffsetState = remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F0E8))
            .then(
                if (onSeek != null) {
                    Modifier
                        .pointerInput(onSeek, slots.size) {
                            detectTapGestures { offset ->
                                val ss = slotSpacingState.floatValue
                                val so = scrollOffsetState.floatValue
                                if (ss > 0f && slots.isNotEmpty()) {
                                    val slotIdx = ((offset.x + so) / ss - 1f)
                                        .roundToInt()
                                        .coerceIn(0, slots.lastIndex)
                                    val noteIdx = slots[slotIdx].entries.firstOrNull()?.first ?: 0
                                    onSeek(noteIdx)
                                }
                            }
                        }
                        .pointerInput(onSeek, slots.size) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val ss = slotSpacingState.floatValue
                                val so = scrollOffsetState.floatValue
                                if (ss > 0f && slots.isNotEmpty()) {
                                    val slotIdx = ((change.position.x + so) / ss - 1f)
                                        .roundToInt()
                                        .coerceIn(0, slots.lastIndex)
                                    val noteIdx = slots[slotIdx].entries.firstOrNull()?.first ?: 0
                                    onSeek(noteIdx)
                                }
                            }
                        }
                } else Modifier
            )
    ) {
        val staffTop = size.height * 0.12f
        val staffBottom = size.height * 0.72f
        val lineSpacing = (staffBottom - staffTop) / 4
        val slotSpacing = size.width / (VISIBLE_SLOTS + 1)
        val noteRadius = lineSpacing * 0.35f
        val scrollOffset = targetScrollSlot * slotSpacing

        slotSpacingState.floatValue = slotSpacing
        scrollOffsetState.floatValue = scrollOffset

        // 5 staff lines
        for (i in 0..4) {
            val y = staffTop + i * lineSpacing
            drawLine(Color(0xFFAAAAAA), Offset(0f, y), Offset(size.width, y), 1f)
        }

        if (slots.isEmpty()) return@Canvas

        val (minSteps, maxSteps) = pitchRange
        val range = (maxSteps - minSteps).coerceAtLeast(4)
        val beatsPerBar = timeSignature.split("/").firstOrNull()?.toIntOrNull() ?: 4

        clipRect(0f, 0f, size.width, size.height) {

            // ── Bar lines ──
            var lastBar = -1
            for ((slotIdx, slot) in slots.withIndex()) {
                val bar = (slot.beat / beatsPerBar).toInt()
                if (bar != lastBar && lastBar >= 0) {
                    val barX = (slotIdx + 0.5f) * slotSpacing - scrollOffset
                    if (barX in 0f..size.width) {
                        drawLine(
                            Color(0xFF999999),
                            Offset(barX, staffTop),
                            Offset(barX, staffBottom),
                            1.5f,
                        )
                    }
                }
                lastBar = bar
            }

            // ── Rest symbols between slots ──
            val restColor = Color(0xFF666666)
            for (i in 0 until slots.lastIndex) {
                val cur = slots[i]
                val next = slots[i + 1]
                val gap = next.beat - (cur.beat + cur.maxDuration)
                if (gap > 0.1f) {
                    val restX = (i + 1.5f) * slotSpacing - scrollOffset
                    if (restX in -slotSpacing..size.width + slotSpacing) {
                        val midY = (staffTop + staffBottom) / 2f
                        val restW = lineSpacing * 0.6f
                        val restH = lineSpacing * 0.35f
                        when {
                            gap >= 4f -> {
                                // Whole rest: rectangle hanging below 2nd line
                                val ry = staffTop + lineSpacing
                                drawRect(restColor, Offset(restX - restW / 2, ry), Size(restW, restH))
                            }
                            gap >= 2f -> {
                                // Half rest: rectangle sitting on 3rd line
                                val ry = staffTop + 2 * lineSpacing - restH
                                drawRect(restColor, Offset(restX - restW / 2, ry), Size(restW, restH))
                            }
                            gap >= 1f -> {
                                // Quarter rest: zigzag
                                val amp = lineSpacing * 0.15f
                                val h = lineSpacing * 0.5f
                                drawLine(restColor, Offset(restX + amp, midY - h), Offset(restX - amp, midY - h * 0.3f), 2f)
                                drawLine(restColor, Offset(restX - amp, midY - h * 0.3f), Offset(restX + amp, midY + h * 0.3f), 2f)
                                drawLine(restColor, Offset(restX + amp, midY + h * 0.3f), Offset(restX - amp, midY + h), 2f)
                            }
                            else -> {
                                // Eighth rest: dot + diagonal
                                drawCircle(restColor, lineSpacing * 0.08f, Offset(restX, midY - lineSpacing * 0.15f))
                                drawLine(restColor, Offset(restX, midY - 0.05f * lineSpacing), Offset(restX - lineSpacing * 0.2f, midY + lineSpacing * 0.35f), 1.5f)
                            }
                        }
                    }
                }
            }

            // ── Notes per slot (chords stacked) ──
            for ((slotIdx, slot) in slots.withIndex()) {
                val x = (slotIdx + 1) * slotSpacing - scrollOffset
                if (x < -slotSpacing * 2 || x > size.width + slotSpacing * 2) continue

                for (entryIdx in slot.entries.indices) {
                    val (noteIdx, note) = slot.entries[entryIdx]
                    val pitch = note.toPitch() ?: continue
                    val steps = pitchToSteps(pitch)
                    val isLeftHand = note.hand == "L"
                    val isHandEnabled = note.hand in enabledHands

                    val normalized = (steps - minSteps).toFloat() / range
                    val y = staffBottom - normalized * (staffBottom - staffTop)

                    val defaultColor = when {
                        !isHandEnabled -> Color(0xFFDDDDDD)
                        isLeftHand -> Purple400.copy(alpha = 0.5f)
                        else -> Color(0xFF666666)
                    }
                    val color = when {
                        !isHandEnabled -> Color(0xFFDDDDDD)
                        noteIdx < noteResults.size && noteResults[noteIdx].status == NoteResult.Status.CORRECT -> Green400
                        noteIdx < noteResults.size && noteResults[noteIdx].status == NoteResult.Status.WRONG -> Red400
                        noteIdx == currentNoteIndex -> Blue400
                        noteIdx > currentNoteIndex -> defaultColor
                        else -> Color(0xFF333333)
                    }

                    val duration = note.duration

                    // ── Note head + stem based on duration ──
                    if (duration >= 4f) {
                        // Whole note: open oval, no stem
                        drawCircle(color, noteRadius, Offset(x, y), style = Stroke(2f))
                    } else {
                        // Note head
                        if (duration >= 2f) {
                            // Half note: open circle
                            drawCircle(color, noteRadius, Offset(x, y), style = Stroke(2f))
                        } else {
                            // Quarter / eighth / sixteenth: filled
                            drawCircle(color, noteRadius, Offset(x, y))
                        }

                        // Stem
                        drawLine(color, Offset(x + noteRadius, y), Offset(x + noteRadius, y - lineSpacing * 1.5f), 2f)

                        // Flags for eighth (<=0.5) and sixteenth (<=0.25)
                        if (duration <= 0.5f + 0.01f) {
                            val stemTopY = y - lineSpacing * 1.5f
                            drawLine(color, Offset(x + noteRadius, stemTopY), Offset(x + noteRadius + lineSpacing * 0.5f, stemTopY + lineSpacing * 0.5f), 2f)
                        }
                        if (duration <= 0.25f + 0.01f) {
                            val stemTopY = y - lineSpacing * 1.5f + lineSpacing * 0.3f
                            drawLine(color, Offset(x + noteRadius, stemTopY), Offset(x + noteRadius + lineSpacing * 0.5f, stemTopY + lineSpacing * 0.5f), 2f)
                        }
                    }

                    // Dot for dotted notes (1.5x a standard value)
                    val isDotted = listOf(0.375f, 0.75f, 1.5f, 3.0f).any { d ->
                        duration in (d - 0.05f)..(d + 0.05f)
                    }
                    if (isDotted) {
                        drawCircle(color, noteRadius * 0.25f, Offset(x + noteRadius * 2f, y))
                    }

                    // Note name below staff (stacked for chords)
                    val noteName = pitch.displayName.dropLast(1)
                    val textColor = if (isLeftHand) Purple400 else color
                    val textResult = textMeasurer.measure(
                        noteName,
                        style = TextStyle(fontSize = 9.sp, color = textColor),
                    )
                    val textY = staffBottom + lineSpacing * 0.6f + entryIdx * textResult.size.height * 1.1f
                    drawText(
                        textResult,
                        topLeft = Offset(x - textResult.size.width / 2f, textY),
                    )
                }
            }

            // ── Playback cursor (orange) – smooth slot-based position ──
            if (playbackProgress != null && playbackProgress >= 0f) {
                val slotProg = noteToSlotProgress(playbackProgress)
                val playbackX = (slotProg + 1f) * slotSpacing - scrollOffset
                if (playbackX in -slotSpacing..size.width + slotSpacing) {
                    drawLine(
                        Orange400.copy(alpha = 0.7f),
                        Offset(playbackX, staffTop - 10),
                        Offset(playbackX, staffBottom + 10),
                        3f,
                    )
                }
            }

            // ── User cursor (blue) ──
            if (currentNoteIndex in notes.indices) {
                val slotIdx = noteToSlot[currentNoteIndex] ?: 0
                val cursorX = (slotIdx + 1) * slotSpacing - scrollOffset
                if (cursorX in -slotSpacing..size.width + slotSpacing) {
                    drawLine(
                        Blue400.copy(alpha = 0.6f),
                        Offset(cursorX, staffTop - 10),
                        Offset(cursorX, staffBottom + 10),
                        2f,
                    )
                }
            }
        }
    }
}

private fun pitchToSteps(pitch: Pitch): Int {
    val noteOrder = mapOf(
        "C" to 0, "C#" to 1, "D" to 2, "D#" to 3, "E" to 4, "F" to 5,
        "F#" to 6, "G" to 7, "G#" to 8, "A" to 9, "A#" to 10, "B" to 11,
    )
    val name = pitch.displayName
    val octave = name.last().digitToInt()
    val noteName = name.dropLast(1)
    val semitone = noteOrder[noteName] ?: 0
    return octave * 12 + semitone
}
