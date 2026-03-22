package de.syntrosoft.syntropiano.domain.model

enum class Pitch(val frequency: Float, val displayName: String) {
    // Octave 1
    C1(32.70f, "C1"), Cs1(34.65f, "C#1"), D1(36.71f, "D1"), Ds1(38.89f, "D#1"),
    E1(41.20f, "E1"), F1(43.65f, "F1"), Fs1(46.25f, "F#1"), G1(49.00f, "G1"),
    Gs1(51.91f, "G#1"), A1(55.00f, "A1"), As1(58.27f, "A#1"), B1(61.74f, "B1"),
    // Octave 2
    C2(65.41f, "C2"), Cs2(69.30f, "C#2"), D2(73.42f, "D2"), Ds2(77.78f, "D#2"),
    E2(82.41f, "E2"), F2(87.31f, "F2"), Fs2(92.50f, "F#2"), G2(98.00f, "G2"),
    Gs2(103.83f, "G#2"), A2(110.00f, "A2"), As2(116.54f, "A#2"), B2(123.47f, "B2"),
    // Octave 3
    C3(130.81f, "C3"), Cs3(138.59f, "C#3"), D3(146.83f, "D3"), Ds3(155.56f, "D#3"),
    E3(164.81f, "E3"), F3(174.61f, "F3"), Fs3(185.00f, "F#3"), G3(196.00f, "G3"),
    Gs3(207.65f, "G#3"), A3(220.00f, "A3"), As3(233.08f, "A#3"), B3(246.94f, "B3"),
    // Octave 4 (middle)
    C4(261.63f, "C4"), Cs4(277.18f, "C#4"), D4(293.66f, "D4"), Ds4(311.13f, "D#4"),
    E4(329.63f, "E4"), F4(349.23f, "F4"), Fs4(369.99f, "F#4"), G4(392.00f, "G4"),
    Gs4(415.30f, "G#4"), A4(440.00f, "A4"), As4(466.16f, "A#4"), B4(493.88f, "B4"),
    // Octave 5
    C5(523.25f, "C5"), Cs5(554.37f, "C#5"), D5(587.33f, "D5"), Ds5(622.25f, "D#5"),
    E5(659.25f, "E5"), F5(698.46f, "F5"), Fs5(739.99f, "F#5"), G5(783.99f, "G5"),
    Gs5(830.61f, "G#5"), A5(880.00f, "A5"), As5(932.33f, "A#5"), B5(987.77f, "B5"),
    // Octave 6
    C6(1046.50f, "C6"), Cs6(1108.73f, "C#6"), D6(1174.66f, "D6"), Ds6(1244.51f, "D#6"),
    E6(1318.51f, "E6"), F6(1396.91f, "F6"), Fs6(1479.98f, "F#6"), G6(1567.98f, "G6"),
    Gs6(1661.22f, "G#6"), A6(1760.00f, "A6"), As6(1864.66f, "A#6"), B6(1975.53f, "B6");

    companion object {
        private const val TOLERANCE_CENTS = 50f // half semitone

        fun fromFrequency(freq: Float): Pitch? {
            if (freq < entries.first().frequency * 0.9f || freq > entries.last().frequency * 1.1f) {
                return null
            }
            return entries.minByOrNull { kotlin.math.abs(it.frequency - freq) }
        }

        fun fromName(name: String): Pitch? {
            val normalized = name.replace("#", "s")
            return entries.find { it.name == normalized }
        }
    }
}
