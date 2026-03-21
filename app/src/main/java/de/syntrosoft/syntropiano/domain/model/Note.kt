package de.syntrosoft.syntropiano.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val pitch: String,       // e.g. "C4", "F#3"
    val duration: Float,     // in beats
    val beat: Float,         // start beat position
    val hand: String = "R",  // "L" or "R"
) {
    fun toPitch(): Pitch? = Pitch.fromName(pitch)
    fun toHand(): Hand = if (hand == "L") Hand.LEFT else Hand.RIGHT
}
