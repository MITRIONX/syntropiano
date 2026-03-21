# SyntroPiano — Design Spec

Android-App zum Keyboard/Klavier lernen für Kinder (10 Jahre). Begleitet das Üben am echten Keyboard mit Notenlernen, strukturierten Kursen, freiem Spielen und Mikrofon-basierter Tonerkennung.

## Zielgruppe & Kontext

- **Nutzer:** 10-jähriger Junge, Anfänger am Keyboard
- **Gerät:** Android Smartphone & Tablet
- **Echtes Keyboard:** Ja — die App ist Lern-Begleiter, kein Ersatz-Instrument
- **Konnektivität:** Offline-first, kein Backend nötig

## Architektur-Ansatz: Offline-First + Import

Die App läuft komplett offline. Lieder sind als JSON-Dateien gebündelt und können per Datei-Import erweitert werden. Kein Backend erforderlich, aber später optional nachrüstbar (Cloud-Sync).

Vorteile:
- Keine Server-Kosten, kein Internet nötig
- Neue Lieder per Import ohne App-Update
- Datenschutz: alles bleibt auf dem Gerät

## App-Bereiche

### 1. Lernen (strukturierter Kurs)

Geführter Lernpfad in 5 Leveln, die aufeinander aufbauen:

**Level 1 — Noten Entdecker:** Notenschlüssel, Notenlinien, die ersten 7 Noten (C-H). Interaktive Quiz ("Welche Note ist das?"). Einfache Tonfolgen erkennen und nachspielen.

**Level 2 — Erste Melodien:** Taktarten, Notenwerte (ganz, halb, viertel). Einfache Melodien mit der rechten Hand. Erste Kinderlieder (Alle meine Entchen, Hänschen klein).

**Level 3 — Rhythmus & Vorzeichen:** Halbe Noten, Pausen, Punktierungen. Vorzeichen (♯ ♭). Schwierigere Stücke, Tempo-Training.

**Level 4 — Beide Hände:** Linke Hand einführen (Bassschlüssel). Einfache Begleitmuster. Koordination beider Hände. Bekannte Lieder zweihändig.

**Level 5+ — Fortgeschritten:** Akkorde, Dynamik, Pedalnutzung. Anspruchsvollere Stücke. Freies Spielen mit wachsender Bibliothek.

Jedes Level enthält 4 Phasen:
- **Theorie** — Interaktive Erklärungen mit Animationen
- **Übungen** — Quiz + Noten auf dem Keyboard spielen
- **Praxis** — Echte Lieder spielen mit Mikrofon-Feedback
- **Test** — Level-Abschluss um weiterzukommen

### 2. Spielen (freie Bibliothek)

Liederbibliothek durchstöbern und frei üben. Mix aus Kinderliedern, Volksliedern, Pop-Songs (vereinfacht), klassischen Stücken und Filmmusik. Bibliothek erweiterbar per Datei-Import.

### 3. Profil (Gamification & Fortschritt)

Fortschritt, Achievements, Streaks, Statistiken — siehe Gamification-Sektion.

## Spielmodus & Audio-Erkennung

### Spielansicht

Der Hauptbildschirm beim Üben zeigt:
- **Notenblatt** mit scrollendem Cursor — gespielte Noten werden grün (richtig) oder rot (falsch) markiert, aktuelle Note blau hervorgehoben
- **Keyboard-Visualisierung** — zeigt welche Taste als nächstes zu drücken ist
- **Live-Feedback** — Mikrofon-Status, erkannte Note, Richtig/Falsch-Anzeige, Fortschritts-Zähler

### Audio-Erkennung (Pitch Detection)

1. **Mikrofon-Input:** Android AudioRecord API im Low-Latency Modus
2. **Pitch Detection:** YIN-Algorithmus via TarsosDSP-Bibliothek erkennt Frequenz → Note
3. **Note Matching:** Erkannte Note wird mit erwarteter Note verglichen, mit Toleranz für Timing und leichte Tonabweichung
4. **Echtzeit-Feedback:** Visuelle Rückmeldung (grün/rot) und Hinweis welche Note erwartet wurde

### 3 Spielmodi

- **Übemodus** — Wartet auf jede Note. Kein Zeitdruck. Perfekt zum Lernen neuer Stücke.
- **Rhythmus-Modus** — Noten scrollen im eingestellten Tempo. Timing zählt. BPM einstellbar.
- **Auftritt** — Ganzes Stück durchspielen. Bewertung am Ende mit 1-3 Sternen.

## Gamification

### XP-System

| Aktion | XP |
|---|---|
| Lektion abschließen | +50 |
| Lied mit 3 Sternen | +100 |
| Tägliches Üben (Streak) | +20 |
| Level-Test bestehen | +200 |
| Perfekte Genauigkeit (100%) | +50 Bonus |
| 7-Tage Streak erreicht | +150 Bonus |

### Ränge (Level-basiert)

- Lv 1-5: Anfänger
- Lv 6-10: Klavier-Lehrling
- Lv 11-20: Melodie-Meister
- Lv 21-35: Virtuose
- Lv 36+: Piano-Legende

### Achievements

- **Erste Note** — Erste Note richtig gespielt
- **Feuerwerk** — 7-Tage Streak
- **Sternesammler** — 10 Lieder mit 3 Sternen
- **Beidhändig** — Erstes Lied mit beiden Händen
- **Perfektionist** — 100% Genauigkeit bei einem Lied
- **Meister** — Alle Level abgeschlossen

### Profil-Statistiken

- Tage-Streak (aktuell & längster)
- Anzahl gelernter Lieder
- Durchschnittliche Genauigkeit
- Gesamte Übungszeit
- Wochen-Aktivität (Balkendiagramm)

## Technische Architektur

### Tech-Stack

- **Sprache:** Kotlin (100%, kein Java)
- **UI:** Jetpack Compose mit Material 3
- **Lokale DB:** Room (SQLite)
- **Audio:** AudioRecord API + TarsosDSP (YIN Pitch Detection)
- **DI:** Hilt
- **Build:** Gradle KTS, Version Catalogs, min SDK 26 (Android 8.0)
- **Navigation:** Compose Navigation

### Clean Architecture (3 Layer)

**UI Layer (Jetpack Compose):**
- Screens: LearnScreen, PlayScreen, ProfileScreen, SongListScreen, LessonScreen
- Components: SheetMusicView, KeyboardView, NoteQuiz
- ViewModels als State Holders

**Domain Layer:**
- PitchDetector — Audio-Signal → erkannte Note
- NoteMatcher — Erkannte Note ↔ erwartete Note vergleichen
- ScoreCalculator — Punkte und Sterne berechnen
- ProgressTracker — Lernfortschritt verwalten
- SongParser — JSON → Song-Objekt
- AchievementEngine — Achievement-Bedingungen prüfen

**Data Layer:**
- Room DB (Fortschritt, Achievements, Profil)
- JSON Song Files (assets/ für gebündelte, importiert für neue)
- AudioRecord API (Mikrofon-Zugriff)
- DataStore (App-Einstellungen)
- SongImporter (Datei-Import Handler)

### Datenmodell

**Song:** id, title, artist, difficulty (1-5), bpm, timeSignature, level, notes (List), isBuiltIn, importedAt

**Note:** pitch (z.B. "C4"), duration (in Beats), startBeat, hand (LEFT/RIGHT)

**Lesson:** id, level (1-5), order, title, type (THEORY/EXERCISE/SONG/TEST), content (JSON)

**Progress:** lessonId, completedAt, stars (0-3), accuracy (0.0-1.0), xpEarned

**PlayerProfile:** totalXp, currentLevel, currentStreak, longestStreak, lastPlayedAt

**Achievement:** id, type (enum), unlockedAt, progress (0.0-1.0)

### Song-Dateiformat (JSON)

```json
{
  "title": "Alle meine Entchen",
  "artist": "Volkslied",
  "difficulty": 1,
  "bpm": 100,
  "timeSignature": "4/4",
  "notes": [
    { "pitch": "C4", "duration": 1.0, "beat": 0, "hand": "R" },
    { "pitch": "D4", "duration": 1.0, "beat": 1, "hand": "R" },
    { "pitch": "E4", "duration": 1.0, "beat": 2, "hand": "R" },
    { "pitch": "F4", "duration": 1.0, "beat": 3, "hand": "R" }
  ]
}
```

Lieder werden als JSON in `assets/songs/` gebündelt oder per Android File Picker importiert.

### Projektstruktur

```
app/src/main/
├── ui/
│   ├── screens/        (Learn, Play, Profile, SongList, Lesson)
│   ├── components/     (SheetMusic, Keyboard, NoteQuiz)
│   ├── theme/          (Colors, Typography, Shapes)
│   └── navigation/     (NavGraph, Routes)
├── domain/
│   ├── model/          (Song, Note, Lesson, Progress, Achievement)
│   ├── audio/          (PitchDetector, NoteMatcher)
│   └── engine/         (ScoreCalculator, AchievementEngine)
├── data/
│   ├── db/             (AppDatabase, DAOs)
│   ├── repository/     (SongRepo, ProgressRepo)
│   └── import/         (SongImporter)
└── di/                 (Hilt Modules)
```

## Nicht im Scope (bewusst ausgelassen)

- Touch-Keyboard auf dem Bildschirm (echtes Keyboard vorhanden)
- MIDI-Unterstützung (evtl. spätere Erweiterung)
- Backend/Cloud-Sync (evtl. spätere Erweiterung)
- Multi-User/Accounts
- In-App-Käufe
- iOS-Version
