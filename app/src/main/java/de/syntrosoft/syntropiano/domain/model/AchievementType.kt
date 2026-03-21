package de.syntrosoft.syntropiano.domain.model

enum class AchievementType(val title: String, val description: String) {
    FIRST_NOTE("Erste Note", "Erste Note richtig gespielt"),
    STREAK_7("Feuerwerk", "7-Tage Streak"),
    STAR_COLLECTOR("Sternesammler", "10 Lieder mit 3 Sternen"),
    BOTH_HANDS("Beidhändig", "Erstes Lied mit beiden Händen"),
    PERFECTIONIST("Perfektionist", "100% Genauigkeit bei einem Lied"),
    MASTER("Meister", "Alle Level abgeschlossen"),
}
