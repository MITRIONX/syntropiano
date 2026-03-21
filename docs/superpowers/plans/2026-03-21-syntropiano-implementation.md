# SyntroPiano Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline-first Android app that teaches kids (10y) keyboard/piano through structured courses, free play, microphone-based pitch detection, and gamification.

**Architecture:** Clean Architecture with 3 layers (UI → Domain → Data). Jetpack Compose UI, Room for persistence, TarsosDSP for audio pitch detection. All content bundled as JSON assets, expandable via file import.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Hilt, TarsosDSP, Compose Navigation, Gradle KTS with Version Catalogs, min SDK 26.

**Spec:** `docs/superpowers/specs/2026-03-21-syntropiano-design.md`

---

## File Structure

```
app/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── assets/
│   │   │   └── songs/
│   │   │       ├── alle-meine-entchen.json
│   │   │       ├── haenschen-klein.json
│   │   │       └── freude-schoener-goetterfunken.json
│   │   └── java/de/syntrosoft/syntropiano/
│   │       ├── SyntroPianoApp.kt                    # Hilt Application class
│   │       ├── MainActivity.kt                       # Single Activity, Compose host
│   │       ├── di/
│   │       │   ├── AppModule.kt                      # Room, DataStore, AudioRecord bindings
│   │       │   └── RepositoryModule.kt               # Repository interface → impl bindings
│   │       ├── domain/
│   │       │   ├── model/
│   │       │   │   ├── Pitch.kt                      # Pitch enum (C0..B8) with frequency mapping
│   │       │   │   ├── Hand.kt                       # LEFT, RIGHT enum
│   │       │   │   ├── Note.kt                       # pitch, duration, startBeat, hand
│   │       │   │   ├── Song.kt                       # id, title, artist, difficulty, bpm, notes, etc.
│   │       │   │   ├── Lesson.kt                     # id, level, order, title, type, content
│   │       │   │   ├── LessonType.kt                 # THEORY, EXERCISE, SONG, TEST enum
│   │       │   │   ├── PlayMode.kt                   # PRACTICE, RHYTHM, PERFORMANCE enum
│   │       │   │   ├── NoteResult.kt                 # CORRECT, WRONG, MISSED enum + detected pitch
│   │       │   │   ├── PlaySession.kt                # Current play state: noteResults, accuracy, stars
│   │       │   │   ├── PlayerProfile.kt              # totalXp, currentLevel, streaks, rank
│   │       │   │   ├── Achievement.kt                # type enum, unlockedAt, progress
│   │       │   │   └── AchievementType.kt            # FIRST_NOTE, STREAK_7, STAR_COLLECTOR, etc.
│   │       │   ├── audio/
│   │       │   │   ├── PitchDetector.kt              # Interface: audio buffer → detected Pitch?
│   │       │   │   ├── YinPitchDetector.kt           # TarsosDSP YIN implementation
│   │       │   │   └── NoteMatcher.kt                # Compare detected pitch with expected note
│   │       │   └── engine/
│   │       │       ├── ScoreCalculator.kt            # Accuracy → stars, XP calculation
│   │       │       ├── XpCalculator.kt               # XP curve: level × 300, rank thresholds
│   │       │       └── AchievementEngine.kt          # Check achievement conditions
│   │       ├── data/
│   │       │   ├── db/
│   │       │   │   ├── AppDatabase.kt                # Room database definition
│   │       │   │   ├── entity/
│   │       │   │   │   ├── SongEntity.kt             # Room entity for songs
│   │       │   │   │   ├── ProgressEntity.kt         # Room entity for lesson/song progress
│   │       │   │   │   ├── PlayerProfileEntity.kt    # Room entity for player profile
│   │       │   │   │   └── AchievementEntity.kt      # Room entity for achievements
│   │       │   │   ├── dao/
│   │       │   │   │   ├── SongDao.kt                # CRUD for songs
│   │       │   │   │   ├── ProgressDao.kt            # CRUD for progress records
│   │       │   │   │   ├── PlayerProfileDao.kt       # CRUD for player profile
│   │       │   │   │   └── AchievementDao.kt         # CRUD for achievements
│   │       │   │   └── converter/
│   │       │   │       └── Converters.kt             # TypeConverters for Room (JSON notes, enums, dates)
│   │       │   ├── repository/
│   │       │   │   ├── SongRepository.kt             # Interface
│   │       │   │   ├── SongRepositoryImpl.kt         # Room + asset songs
│   │       │   │   ├── ProgressRepository.kt         # Interface
│   │       │   │   ├── ProgressRepositoryImpl.kt     # Room-backed progress
│   │       │   │   ├── PlayerRepository.kt           # Interface
│   │       │   │   └── PlayerRepositoryImpl.kt       # Room-backed profile + achievements
│   │       │   └── import/
│   │       │       ├── SongParser.kt                 # JSON string → Song, with validation
│   │       │       └── SongImporter.kt               # File picker → parse → save to Room
│   │       └── ui/
│   │           ├── theme/
│   │           │   ├── Theme.kt                      # SyntroPianoTheme, dark/light
│   │           │   ├── Color.kt                      # Color palette
│   │           │   └── Type.kt                       # Typography
│   │           ├── navigation/
│   │           │   ├── Screen.kt                     # Sealed class for routes
│   │           │   └── NavGraph.kt                   # NavHost with all screens
│   │           ├── components/
│   │           │   ├── KeyboardView.kt               # Piano keyboard visualization
│   │           │   ├── SheetMusicView.kt             # Staff + notes + scrolling cursor
│   │           │   ├── NoteQuiz.kt                   # "Which note is this?" quiz component
│   │           │   ├── StarRating.kt                 # 1-3 star display
│   │           │   ├── XpBar.kt                      # XP progress bar with level
│   │           │   ├── StreakBadge.kt                 # Fire streak counter
│   │           │   └── AchievementCard.kt            # Achievement badge display
│   │           ├── screens/
│   │           │   ├── home/
│   │           │   │   ├── HomeScreen.kt             # Main screen with Learn/Play/Profile tabs
│   │           │   │   └── HomeViewModel.kt
│   │           │   ├── learn/
│   │           │   │   ├── LearnScreen.kt            # Level overview with progress
│   │           │   │   ├── LearnViewModel.kt
│   │           │   │   ├── LessonScreen.kt           # Individual lesson (theory/exercise/song/test)
│   │           │   │   └── LessonViewModel.kt
│   │           │   ├── play/
│   │           │   │   ├── SongListScreen.kt         # Song library browser
│   │           │   │   ├── SongListViewModel.kt
│   │           │   │   ├── PlayScreen.kt             # Main play screen with sheet music + feedback
│   │           │   │   └── PlayViewModel.kt          # Orchestrates audio, matching, scoring
│   │           │   └── profile/
│   │           │       ├── ProfileScreen.kt          # Stats, achievements, streak, XP
│   │           │       └── ProfileViewModel.kt
│   │           └── audio/
│   │               └── AudioCaptureService.kt        # Manages AudioRecord lifecycle, feeds PitchDetector
│   └── test/java/de/syntrosoft/syntropiano/
│       ├── domain/
│       │   ├── model/
│       │   │   └── PitchTest.kt
│       │   ├── audio/
│       │   │   ├── YinPitchDetectorTest.kt
│       │   │   └── NoteMatcherTest.kt
│       │   └── engine/
│       │       ├── ScoreCalculatorTest.kt
│       │       ├── XpCalculatorTest.kt
│       │       └── AchievementEngineTest.kt
│       └── data/
│           ├── import/
│           │   └── SongParserTest.kt
│           └── repository/
│               └── SongRepositoryTest.kt
├── gradle/
│   └── libs.versions.toml                           # Version catalog
├── build.gradle.kts                                  # Project-level
└── settings.gradle.kts
```

---

## Task 1: Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (project-level)
- Create: `app/build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/SyntroPianoApp.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/MainActivity.kt`
- Create: `gradle.properties`

- [ ] **Step 1: Create project-level Gradle files**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // for TarsosDSP
    }
}

rootProject.name = "SyntroPiano"
include(":app")
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 2: Create version catalog**

`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
compose-bom = "2025.01.01"
hilt = "2.54"
hilt-navigation-compose = "1.2.0"
room = "2.6.1"
navigation-compose = "2.8.6"
lifecycle = "2.8.7"
coroutines = "1.9.0"
serialization = "1.7.3"
tarsosdsp = "2.4"
junit = "4.13.2"
mockk = "1.13.14"
turbine = "1.2.0"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }

hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }

tarsosdsp-android = { group = "com.github.niccoloZeppworkinprogress", name = "TarsosDSP-Android", version = "master-SNAPSHOT" }

junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

**Note on TarsosDSP:** The original TarsosDSP Android library is not published to Maven Central. Use the JitPack fork or copy the YIN algorithm source directly. If the JitPack dependency fails during build, fall back to implementing YIN manually (see Task 5 for the algorithm). The core YIN algorithm is ~100 lines of Kotlin.

- [ ] **Step 3: Create app-level build.gradle.kts**

`app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "de.syntrosoft.syntropiano"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.syntrosoft.syntropiano"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test)
}
```

- [ ] **Step 4: Create AndroidManifest.xml**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:name=".SyntroPianoApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="SyntroPiano"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 5: Create Application and MainActivity**

`SyntroPianoApp.kt`:
```kotlin
package de.syntrosoft.syntropiano

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SyntroPianoApp : Application()
```

`MainActivity.kt`:
```kotlin
package de.syntrosoft.syntropiano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import de.syntrosoft.syntropiano.ui.theme.SyntroPianoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyntroPianoTheme {
                // NavGraph will be added in Task 9
            }
        }
    }
}
```

- [ ] **Step 6: Create minimal theme**

`app/src/main/java/de/syntrosoft/syntropiano/ui/theme/Color.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.theme

import androidx.compose.ui.graphics.Color

val Blue400 = Color(0xFF4FC3F7)
val Green400 = Color(0xFF81C784)
val Orange400 = Color(0xFFFFB74D)
val Red400 = Color(0xFFE57373)
val Purple300 = Color(0xFFCE93D8)

val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF1A1A2E)
val DarkCard = Color(0xFF1A1A2E)
```

`app/src/main/java/de/syntrosoft/syntropiano/ui/theme/Type.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)
```

`app/src/main/java/de/syntrosoft/syntropiano/ui/theme/Theme.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    secondary = Green400,
    tertiary = Orange400,
    background = DarkBackground,
    surface = DarkSurface,
    error = Red400,
)

@Composable
fun SyntroPianoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
```

- [ ] **Step 7: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: scaffold Android project with Compose, Hilt, Room, TarsosDSP"
```

---

## Task 2: Domain Models

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/Pitch.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/Hand.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/Note.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/Song.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/Lesson.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/LessonType.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/PlayMode.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/NoteResult.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/PlaySession.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/PlayerProfile.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/Achievement.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/model/AchievementType.kt`
- Test: `app/src/test/java/de/syntrosoft/syntropiano/domain/model/PitchTest.kt`

- [ ] **Step 1: Write Pitch test**

`PitchTest.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

import org.junit.Assert.*
import org.junit.Test

class PitchTest {
    @Test
    fun `C4 is middle C at 261_63 Hz`() {
        val pitch = Pitch.C4
        assertEquals(261.63f, pitch.frequency, 0.01f)
    }

    @Test
    fun `A4 is concert pitch at 440 Hz`() {
        assertEquals(440.0f, Pitch.A4.frequency, 0.01f)
    }

    @Test
    fun `fromFrequency finds nearest pitch`() {
        val pitch = Pitch.fromFrequency(442.0f)
        assertEquals(Pitch.A4, pitch)
    }

    @Test
    fun `fromFrequency returns null for out of range`() {
        assertNull(Pitch.fromFrequency(10.0f))
    }

    @Test
    fun `fromName parses C4`() {
        assertEquals(Pitch.C4, Pitch.fromName("C4"))
    }

    @Test
    fun `fromName parses sharp notes`() {
        assertEquals(Pitch.Cs4, Pitch.fromName("C#4"))
    }

    @Test
    fun `displayName formats correctly`() {
        assertEquals("C4", Pitch.C4.displayName)
        assertEquals("C#4", Pitch.Cs4.displayName)
    }

    @Test
    fun `pitches are ordered by frequency`() {
        assertTrue(Pitch.C4.frequency < Pitch.D4.frequency)
        assertTrue(Pitch.D4.frequency < Pitch.E4.frequency)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*.PitchTest" -v`
Expected: FAIL — Pitch class not found

- [ ] **Step 3: Implement Pitch enum**

`Pitch.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

enum class Pitch(val frequency: Float, val displayName: String) {
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*.PitchTest" -v`
Expected: All tests PASS

- [ ] **Step 5: Create remaining model classes**

`Hand.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

enum class Hand { LEFT, RIGHT }
```

`Note.kt`:
```kotlin
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
```

`Song.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String = "",
    val difficulty: Int = 1,      // 1-5
    val bpm: Int = 100,
    val timeSignature: String = "4/4",
    val level: Int = 1,
    val notes: List<Note>,
    val isBuiltIn: Boolean = true,
    val importedAt: Long? = null,
)
```

`LessonType.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

enum class LessonType { THEORY, EXERCISE, SONG, TEST }
```

`Lesson.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

data class Lesson(
    val id: Long = 0,
    val level: Int,
    val order: Int,
    val title: String,
    val type: LessonType,
    val content: String = "",  // JSON content
    val songId: Long? = null,  // linked song for SONG/TEST types
)
```

`PlayMode.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

enum class PlayMode { PRACTICE, RHYTHM, PERFORMANCE }
```

`NoteResult.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

data class NoteResult(
    val expected: Note,
    val detected: Pitch?,
    val status: Status,
) {
    enum class Status { CORRECT, WRONG, MISSED, PENDING }
}
```

`PlaySession.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

data class PlaySession(
    val song: Song,
    val mode: PlayMode,
    val noteResults: List<NoteResult> = emptyList(),
    val currentNoteIndex: Int = 0,
    val isFinished: Boolean = false,
) {
    val totalNotes: Int get() = song.notes.size
    val correctNotes: Int get() = noteResults.count { it.status == NoteResult.Status.CORRECT }
    val accuracy: Float get() = if (noteResults.isEmpty()) 0f else correctNotes.toFloat() / noteResults.size
    val stars: Int get() = when {
        accuracy >= 0.95f -> 3
        accuracy >= 0.80f -> 2
        accuracy >= 0.60f -> 1
        else -> 0
    }
}
```

`AchievementType.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

enum class AchievementType(val title: String, val description: String) {
    FIRST_NOTE("Erste Note", "Erste Note richtig gespielt"),
    STREAK_7("Feuerwerk", "7-Tage Streak"),
    STAR_COLLECTOR("Sternesammler", "10 Lieder mit 3 Sternen"),
    BOTH_HANDS("Beidhändig", "Erstes Lied mit beiden Händen"),
    PERFECTIONIST("Perfektionist", "100% Genauigkeit bei einem Lied"),
    MASTER("Meister", "Alle Level abgeschlossen"),
}
```

`Achievement.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

data class Achievement(
    val type: AchievementType,
    val unlockedAt: Long? = null,
    val progress: Float = 0f,  // 0.0 - 1.0
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}
```

`PlayerProfile.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.model

enum class Rank(val title: String, val minLevel: Int) {
    BEGINNER("Anfänger", 1),
    APPRENTICE("Klavier-Lehrling", 6),
    MELODY_MASTER("Melodie-Meister", 11),
    VIRTUOSO("Virtuose", 21),
    LEGEND("Piano-Legende", 36),
}

data class PlayerProfile(
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPlayedAt: Long? = null,
    val totalPlayTimeMinutes: Int = 0,
) {
    val rank: Rank get() = Rank.entries.last { currentLevel >= it.minLevel }
    val xpForNextLevel: Int get() = (currentLevel + 1) * 300
    val xpInCurrentLevel: Int get() {
        val xpForCurrent = (1..currentLevel).sumOf { it * 300 }
        return totalXp - xpForCurrent + (currentLevel * 300)
    }
}
```

- [ ] **Step 6: Run all tests**

Run: `./gradlew test -v`
Expected: All PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/domain/model/
git add app/src/test/java/de/syntrosoft/syntropiano/domain/model/
git commit -m "feat: add domain models (Pitch, Note, Song, Lesson, Achievement, PlayerProfile)"
```

---

## Task 3: Song Parser & Validation

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/import/SongParser.kt`
- Test: `app/src/test/java/de/syntrosoft/syntropiano/data/import/SongParserTest.kt`

- [ ] **Step 1: Write SongParser tests**

`SongParserTest.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.import

import de.syntrosoft.syntropiano.domain.model.Song
import org.junit.Assert.*
import org.junit.Test

class SongParserTest {
    private val parser = SongParser()

    private val validJson = """
        {
            "title": "Test Song",
            "artist": "Test Artist",
            "difficulty": 2,
            "bpm": 120,
            "timeSignature": "4/4",
            "notes": [
                {"pitch": "C4", "duration": 1.0, "beat": 0, "hand": "R"},
                {"pitch": "D4", "duration": 1.0, "beat": 1, "hand": "R"}
            ]
        }
    """.trimIndent()

    @Test
    fun `parses valid song JSON`() {
        val result = parser.parse(validJson)
        assertTrue(result.isSuccess)
        val song = result.getOrThrow()
        assertEquals("Test Song", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals(2, song.difficulty)
        assertEquals(120, song.bpm)
        assertEquals(2, song.notes.size)
        assertEquals("C4", song.notes[0].pitch)
    }

    @Test
    fun `fails on missing title`() {
        val json = """{"notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("title") == true)
    }

    @Test
    fun `fails on empty notes`() {
        val json = """{"title": "Empty", "notes": []}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on missing notes`() {
        val json = """{"title": "No Notes"}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on invalid pitch`() {
        val json = """{"title": "Bad", "notes": [{"pitch": "X9", "duration": 1.0, "beat": 0}]}"""
        val result = parser.parse(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("pitch") == true)
    }

    @Test
    fun `defaults artist to empty string`() {
        val json = """{"title": "Minimal", "notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val song = parser.parse(json).getOrThrow()
        assertEquals("", song.artist)
    }

    @Test
    fun `defaults difficulty to 1`() {
        val json = """{"title": "Minimal", "notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val song = parser.parse(json).getOrThrow()
        assertEquals(1, song.difficulty)
    }

    @Test
    fun `defaults hand to R`() {
        val json = """{"title": "Test", "notes": [{"pitch": "C4", "duration": 1.0, "beat": 0}]}"""
        val song = parser.parse(json).getOrThrow()
        assertEquals("R", song.notes[0].hand)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*.SongParserTest" -v`
Expected: FAIL

- [ ] **Step 3: Implement SongParser**

`SongParser.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.import

import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SongJson(
    val title: String? = null,
    val artist: String = "",
    val difficulty: Int = 1,
    val bpm: Int = 100,
    val timeSignature: String = "4/4",
    val level: Int = 1,
    val notes: List<Note>? = null,
)

class SongParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonString: String): Result<Song> = runCatching {
        val raw = json.decodeFromString<SongJson>(jsonString)

        requireNotNull(raw.title) { "Missing required field: title" }
        requireNotNull(raw.notes) { "Missing required field: notes" }
        require(raw.notes.isNotEmpty()) { "Song must have at least one note" }

        raw.notes.forEach { note ->
            requireNotNull(Pitch.fromName(note.pitch)) {
                "Invalid pitch: '${note.pitch}'. Must be in range A0-C8 (e.g. C4, F#3)"
            }
        }

        Song(
            title = raw.title,
            artist = raw.artist,
            difficulty = raw.difficulty.coerceIn(1, 5),
            bpm = raw.bpm,
            timeSignature = raw.timeSignature,
            level = raw.level,
            notes = raw.notes,
            isBuiltIn = false,
            importedAt = System.currentTimeMillis(),
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "*.SongParserTest" -v`
Expected: All PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/data/import/
git add app/src/test/java/de/syntrosoft/syntropiano/data/import/
git commit -m "feat: add SongParser with JSON validation"
```

---

## Task 4: Score & XP Engine

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/engine/ScoreCalculator.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/engine/XpCalculator.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/engine/AchievementEngine.kt`
- Test: `app/src/test/java/de/syntrosoft/syntropiano/domain/engine/ScoreCalculatorTest.kt`
- Test: `app/src/test/java/de/syntrosoft/syntropiano/domain/engine/XpCalculatorTest.kt`
- Test: `app/src/test/java/de/syntrosoft/syntropiano/domain/engine/AchievementEngineTest.kt`

- [ ] **Step 1: Write ScoreCalculator tests**

`ScoreCalculatorTest.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.engine

import org.junit.Assert.*
import org.junit.Test

class ScoreCalculatorTest {
    private val calc = ScoreCalculator()

    @Test
    fun `95 percent accuracy gives 3 stars`() {
        assertEquals(3, calc.calculateStars(0.95f))
    }

    @Test
    fun `80 percent accuracy gives 2 stars`() {
        assertEquals(2, calc.calculateStars(0.80f))
    }

    @Test
    fun `60 percent accuracy gives 1 star`() {
        assertEquals(1, calc.calculateStars(0.60f))
    }

    @Test
    fun `below 60 percent gives 0 stars`() {
        assertEquals(0, calc.calculateStars(0.59f))
    }

    @Test
    fun `100 percent gives 3 stars plus perfection bonus`() {
        val xp = calc.calculateXp(accuracy = 1.0f, stars = 3, isLevelTest = false)
        assertEquals(100 + 50, xp) // 3-star song + perfection bonus
    }

    @Test
    fun `level test gives 200 XP`() {
        val xp = calc.calculateXp(accuracy = 0.85f, stars = 2, isLevelTest = true)
        assertEquals(200, xp)
    }

    @Test
    fun `3 star song gives 100 XP`() {
        val xp = calc.calculateXp(accuracy = 0.96f, stars = 3, isLevelTest = false)
        assertEquals(100, xp)
    }

    @Test
    fun `lesson completion gives 50 XP`() {
        assertEquals(50, calc.lessonXp())
    }

    @Test
    fun `daily streak gives 20 XP`() {
        assertEquals(20, calc.streakXp())
    }

    @Test
    fun `7 day streak bonus gives 150 XP`() {
        assertEquals(150, calc.streakBonusXp(7))
    }

    @Test
    fun `non 7 day streak gives no bonus`() {
        assertEquals(0, calc.streakBonusXp(6))
        assertEquals(0, calc.streakBonusXp(8))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*.ScoreCalculatorTest" -v`
Expected: FAIL

- [ ] **Step 3: Implement ScoreCalculator**

`ScoreCalculator.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.engine

class ScoreCalculator {

    fun calculateStars(accuracy: Float): Int = when {
        accuracy >= 0.95f -> 3
        accuracy >= 0.80f -> 2
        accuracy >= 0.60f -> 1
        else -> 0
    }

    fun calculateXp(accuracy: Float, stars: Int, isLevelTest: Boolean): Int {
        var xp = when {
            isLevelTest -> 200
            stars >= 3 -> 100
            else -> 50
        }
        if (accuracy >= 1.0f) xp += 50 // perfection bonus
        return xp
    }

    fun lessonXp(): Int = 50

    fun streakXp(): Int = 20

    fun streakBonusXp(streakDays: Int): Int =
        if (streakDays == 7) 150 else 0
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "*.ScoreCalculatorTest" -v`
Expected: All PASS

- [ ] **Step 5: Write XpCalculator tests**

`XpCalculatorTest.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.engine

import org.junit.Assert.*
import org.junit.Test

class XpCalculatorTest {
    private val calc = XpCalculator()

    @Test
    fun `level 1 requires 300 XP`() {
        assertEquals(300, calc.xpForLevel(1))
    }

    @Test
    fun `level 10 requires 3000 XP`() {
        assertEquals(3000, calc.xpForLevel(10))
    }

    @Test
    fun `0 XP is level 1`() {
        assertEquals(1, calc.levelForTotalXp(0))
    }

    @Test
    fun `300 XP is level 2`() {
        assertEquals(2, calc.levelForTotalXp(300))
    }

    @Test
    fun `299 XP is still level 1`() {
        assertEquals(1, calc.levelForTotalXp(299))
    }

    @Test
    fun `900 XP is level 3`() {
        // Level 1 = 300, Level 2 = 600 → total 900 to reach level 3
        assertEquals(3, calc.levelForTotalXp(900))
    }
}
```

- [ ] **Step 6: Implement XpCalculator**

`XpCalculator.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.engine

class XpCalculator {

    fun xpForLevel(level: Int): Int = level * 300

    fun levelForTotalXp(totalXp: Int): Int {
        var accumulated = 0
        var level = 1
        while (true) {
            val needed = xpForLevel(level)
            if (accumulated + needed > totalXp) return level
            accumulated += needed
            level++
        }
    }

    fun xpProgressInLevel(totalXp: Int): Pair<Int, Int> {
        var accumulated = 0
        var level = 1
        while (true) {
            val needed = xpForLevel(level)
            if (accumulated + needed > totalXp) {
                return (totalXp - accumulated) to needed
            }
            accumulated += needed
            level++
        }
    }
}
```

- [ ] **Step 7: Run XpCalculator tests**

Run: `./gradlew test --tests "*.XpCalculatorTest" -v`
Expected: All PASS

- [ ] **Step 8: Write AchievementEngine tests**

`AchievementEngineTest.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.engine

import de.syntrosoft.syntropiano.domain.model.AchievementType
import org.junit.Assert.*
import org.junit.Test

class AchievementEngineTest {
    private val engine = AchievementEngine()

    @Test
    fun `FIRST_NOTE unlocks when first correct note is played`() {
        val result = engine.check(
            type = AchievementType.FIRST_NOTE,
            totalCorrectNotes = 1,
        )
        assertTrue(result)
    }

    @Test
    fun `FIRST_NOTE does not unlock with 0 correct notes`() {
        assertFalse(engine.check(AchievementType.FIRST_NOTE, totalCorrectNotes = 0))
    }

    @Test
    fun `STREAK_7 unlocks at 7 day streak`() {
        assertTrue(engine.check(AchievementType.STREAK_7, currentStreak = 7))
    }

    @Test
    fun `STREAK_7 does not unlock at 6 days`() {
        assertFalse(engine.check(AchievementType.STREAK_7, currentStreak = 6))
    }

    @Test
    fun `STAR_COLLECTOR unlocks at 10 three-star songs`() {
        assertTrue(engine.check(AchievementType.STAR_COLLECTOR, threeStarSongs = 10))
    }

    @Test
    fun `PERFECTIONIST unlocks with perfect accuracy`() {
        assertTrue(engine.check(AchievementType.PERFECTIONIST, hasPerfectSong = true))
    }
}
```

- [ ] **Step 9: Implement AchievementEngine**

`AchievementEngine.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.engine

import de.syntrosoft.syntropiano.domain.model.AchievementType

class AchievementEngine {

    fun check(
        type: AchievementType,
        totalCorrectNotes: Int = 0,
        currentStreak: Int = 0,
        threeStarSongs: Int = 0,
        hasBothHandsSong: Boolean = false,
        hasPerfectSong: Boolean = false,
        allLevelsCompleted: Boolean = false,
    ): Boolean = when (type) {
        AchievementType.FIRST_NOTE -> totalCorrectNotes >= 1
        AchievementType.STREAK_7 -> currentStreak >= 7
        AchievementType.STAR_COLLECTOR -> threeStarSongs >= 10
        AchievementType.BOTH_HANDS -> hasBothHandsSong
        AchievementType.PERFECTIONIST -> hasPerfectSong
        AchievementType.MASTER -> allLevelsCompleted
    }
}
```

- [ ] **Step 10: Run all engine tests**

Run: `./gradlew test --tests "*.engine.*" -v`
Expected: All PASS

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/domain/engine/
git add app/src/test/java/de/syntrosoft/syntropiano/domain/engine/
git commit -m "feat: add ScoreCalculator, XpCalculator, and AchievementEngine"
```

---

## Task 5: Pitch Detection (Audio)

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/audio/PitchDetector.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/audio/YinPitchDetector.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/domain/audio/NoteMatcher.kt`
- Test: `app/src/test/java/de/syntrosoft/syntropiano/domain/audio/YinPitchDetectorTest.kt`
- Test: `app/src/test/java/de/syntrosoft/syntropiano/domain/audio/NoteMatcherTest.kt`

- [ ] **Step 1: Write NoteMatcher tests**

`NoteMatcherTest.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.NoteResult
import de.syntrosoft.syntropiano.domain.model.Pitch
import org.junit.Assert.*
import org.junit.Test

class NoteMatcherTest {
    private val matcher = NoteMatcher()

    @Test
    fun `exact pitch match is CORRECT`() {
        val expected = Note(pitch = "C4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, Pitch.C4)
        assertEquals(NoteResult.Status.CORRECT, result.status)
    }

    @Test
    fun `wrong pitch is WRONG`() {
        val expected = Note(pitch = "C4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, Pitch.D4)
        assertEquals(NoteResult.Status.WRONG, result.status)
        assertEquals(Pitch.D4, result.detected)
    }

    @Test
    fun `null pitch is MISSED`() {
        val expected = Note(pitch = "C4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, null)
        assertEquals(NoteResult.Status.MISSED, result.status)
    }

    @Test
    fun `enharmonic equivalent is CORRECT`() {
        // C#4 and Db4 are the same pitch
        val expected = Note(pitch = "C#4", duration = 1f, beat = 0f)
        val result = matcher.match(expected, Pitch.Cs4)
        assertEquals(NoteResult.Status.CORRECT, result.status)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "*.NoteMatcherTest" -v`
Expected: FAIL

- [ ] **Step 3: Implement NoteMatcher**

`NoteMatcher.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.NoteResult
import de.syntrosoft.syntropiano.domain.model.Pitch

class NoteMatcher {

    fun match(expected: Note, detected: Pitch?): NoteResult {
        if (detected == null) {
            return NoteResult(expected = expected, detected = null, status = NoteResult.Status.MISSED)
        }

        val expectedPitch = expected.toPitch()
        val isCorrect = expectedPitch == detected

        return NoteResult(
            expected = expected,
            detected = detected,
            status = if (isCorrect) NoteResult.Status.CORRECT else NoteResult.Status.WRONG,
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "*.NoteMatcherTest" -v`
Expected: All PASS

- [ ] **Step 5: Create PitchDetector interface and YIN implementation**

`PitchDetector.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Pitch

interface PitchDetector {
    /**
     * Detect the fundamental frequency from an audio buffer.
     * @param audioBuffer PCM 16-bit samples
     * @param sampleRate sample rate in Hz (typically 44100)
     * @return detected Pitch or null if no clear pitch found
     */
    fun detect(audioBuffer: ShortArray, sampleRate: Int): Pitch?
}
```

`YinPitchDetector.kt` — standalone YIN implementation (no TarsosDSP dependency needed for core algorithm):
```kotlin
package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Pitch
import kotlin.math.abs

/**
 * YIN pitch detection algorithm.
 * Reference: De Cheveigné & Kawahara (2002) "YIN, a fundamental frequency estimator"
 */
class YinPitchDetector(
    private val threshold: Float = 0.15f,
) : PitchDetector {

    override fun detect(audioBuffer: ShortArray, sampleRate: Int): Pitch? {
        val floatBuffer = FloatArray(audioBuffer.size) { audioBuffer[it].toFloat() / Short.MAX_VALUE }
        val halfSize = floatBuffer.size / 2

        // Step 1 & 2: Difference function
        val diff = FloatArray(halfSize)
        for (tau in 1 until halfSize) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = floatBuffer[i] - floatBuffer[i + tau]
                sum += delta * delta
            }
            diff[tau] = sum
        }

        // Step 3: Cumulative mean normalized difference
        val cmndf = FloatArray(halfSize)
        cmndf[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfSize) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum == 0f) 1f else diff[tau] * tau / runningSum
        }

        // Step 4: Absolute threshold
        var tau = 2
        while (tau < halfSize) {
            if (cmndf[tau] < threshold) {
                while (tau + 1 < halfSize && cmndf[tau + 1] < cmndf[tau]) {
                    tau++
                }
                break
            }
            tau++
        }

        if (tau >= halfSize) return null

        // Step 5: Parabolic interpolation
        val betterTau = if (tau > 0 && tau < halfSize - 1) {
            val s0 = cmndf[tau - 1]
            val s1 = cmndf[tau]
            val s2 = cmndf[tau + 1]
            val adjustment = (s2 - s0) / (2 * (2 * s1 - s2 - s0))
            if (abs(adjustment) < 1) tau + adjustment else tau.toFloat()
        } else {
            tau.toFloat()
        }

        val frequency = sampleRate / betterTau
        return Pitch.fromFrequency(frequency)
    }
}
```

- [ ] **Step 6: Write YinPitchDetector test with synthetic sine wave**

`YinPitchDetectorTest.kt`:
```kotlin
package de.syntrosoft.syntropiano.domain.audio

import de.syntrosoft.syntropiano.domain.model.Pitch
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class YinPitchDetectorTest {
    private val detector = YinPitchDetector()
    private val sampleRate = 44100

    private fun generateSineWave(frequency: Float, samples: Int = 4096): ShortArray {
        return ShortArray(samples) { i ->
            val sample = sin(2.0 * PI * frequency * i / sampleRate)
            (sample * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
    }

    @Test
    fun `detects A4 at 440 Hz`() {
        val buffer = generateSineWave(440f)
        val result = detector.detect(buffer, sampleRate)
        assertEquals(Pitch.A4, result)
    }

    @Test
    fun `detects C4 middle C`() {
        val buffer = generateSineWave(261.63f)
        val result = detector.detect(buffer, sampleRate)
        assertEquals(Pitch.C4, result)
    }

    @Test
    fun `detects E4`() {
        val buffer = generateSineWave(329.63f)
        val result = detector.detect(buffer, sampleRate)
        assertEquals(Pitch.E4, result)
    }

    @Test
    fun `returns null for silence`() {
        val silence = ShortArray(4096) { 0 }
        val result = detector.detect(silence, sampleRate)
        assertNull(result)
    }
}
```

- [ ] **Step 7: Run all audio tests**

Run: `./gradlew test --tests "*.audio.*" -v`
Expected: All PASS

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/domain/audio/
git add app/src/test/java/de/syntrosoft/syntropiano/domain/audio/
git commit -m "feat: add YIN pitch detection and NoteMatcher"
```

---

## Task 6: Room Database & DAOs

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/entity/SongEntity.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/entity/ProgressEntity.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/entity/PlayerProfileEntity.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/entity/AchievementEntity.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/dao/SongDao.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/dao/ProgressDao.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/dao/PlayerProfileDao.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/dao/AchievementDao.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/converter/Converters.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/db/AppDatabase.kt`

- [ ] **Step 1: Create Room entities**

`SongEntity.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "",
    val difficulty: Int = 1,
    val bpm: Int = 100,
    val timeSignature: String = "4/4",
    val level: Int = 1,
    val notesJson: String,    // Serialized List<Note> as JSON
    val isBuiltIn: Boolean = true,
    val importedAt: Long? = null,
)
```

`ProgressEntity.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long? = null,
    val lessonId: Long? = null,
    val completedAt: Long,
    val stars: Int = 0,
    val accuracy: Float = 0f,
    val xpEarned: Int = 0,
)
```

`PlayerProfileEntity.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1, // singleton
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPlayedAt: Long? = null,
    val totalPlayTimeMinutes: Int = 0,
)
```

`AchievementEntity.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val type: String, // AchievementType.name
    val unlockedAt: Long? = null,
    val progress: Float = 0f,
)
```

- [ ] **Step 2: Create DAOs**

`SongDao.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.dao

import androidx.room.*
import de.syntrosoft.syntropiano.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY difficulty, title")
    fun getAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE level = :level ORDER BY difficulty")
    fun getByLevel(level: Int): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int
}
```

`ProgressDao.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.dao

import androidx.room.*
import de.syntrosoft.syntropiano.data.db.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Insert
    suspend fun insert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE songId = :songId ORDER BY completedAt DESC LIMIT 1")
    suspend fun getBestForSong(songId: Long): ProgressEntity?

    @Query("SELECT MAX(stars) FROM progress WHERE songId = :songId")
    suspend fun getBestStarsForSong(songId: Long): Int?

    @Query("SELECT COUNT(DISTINCT songId) FROM progress WHERE stars >= 3")
    suspend fun countThreeStarSongs(): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM progress WHERE stars >= 1")
    suspend fun countCompletedSongs(): Int

    @Query("SELECT AVG(accuracy) FROM progress")
    suspend fun averageAccuracy(): Float?

    @Query("SELECT * FROM progress ORDER BY completedAt DESC")
    fun getAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE lessonId = :lessonId ORDER BY completedAt DESC LIMIT 1")
    suspend fun getForLesson(lessonId: Long): ProgressEntity?
}
```

`PlayerProfileDao.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.dao

import androidx.room.*
import de.syntrosoft.syntropiano.data.db.entity.PlayerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun get(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: PlayerProfileEntity)
}
```

`AchievementDao.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.dao

import androidx.room.*
import de.syntrosoft.syntropiano.data.db.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE type = :type")
    suspend fun getByType(type: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(achievement: AchievementEntity)
}
```

- [ ] **Step 3: Create Converters and AppDatabase**

`Converters.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db.converter

import androidx.room.TypeConverter
import de.syntrosoft.syntropiano.domain.model.Note
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun notesToJson(notes: List<Note>): String = json.encodeToString(notes)

    @TypeConverter
    fun jsonToNotes(jsonString: String): List<Note> = json.decodeFromString(jsonString)
}
```

`AppDatabase.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.syntrosoft.syntropiano.data.db.converter.Converters
import de.syntrosoft.syntropiano.data.db.dao.*
import de.syntrosoft.syntropiano.data.db.entity.*

@Database(
    entities = [
        SongEntity::class,
        ProgressEntity::class,
        PlayerProfileEntity::class,
        AchievementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun progressDao(): ProgressDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun achievementDao(): AchievementDao
}
```

- [ ] **Step 4: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/data/db/
git commit -m "feat: add Room database with entities and DAOs"
```

---

## Task 7: Repositories & Hilt DI

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/repository/SongRepository.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/repository/SongRepositoryImpl.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/repository/ProgressRepository.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/repository/ProgressRepositoryImpl.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/repository/PlayerRepository.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/repository/PlayerRepositoryImpl.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/data/import/SongImporter.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/di/AppModule.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/di/RepositoryModule.kt`

- [ ] **Step 1: Create repository interfaces**

`SongRepository.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getSongsByLevel(level: Int): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    suspend fun importSong(song: Song): Long
    suspend fun seedBuiltInSongs()
}
```

`ProgressRepository.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.data.db.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    suspend fun saveProgress(songId: Long?, lessonId: Long?, stars: Int, accuracy: Float, xpEarned: Int)
    suspend fun getBestStarsForSong(songId: Long): Int
    suspend fun countThreeStarSongs(): Int
    suspend fun countCompletedSongs(): Int
    suspend fun averageAccuracy(): Float
    suspend fun isLessonCompleted(lessonId: Long): Boolean
    fun getAllProgress(): Flow<List<ProgressEntity>>
}
```

`PlayerRepository.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.domain.model.Achievement
import de.syntrosoft.syntropiano.domain.model.AchievementType
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getProfile(): Flow<PlayerProfile>
    suspend fun addXp(amount: Int)
    suspend fun updateStreak()
    suspend fun addPlayTime(minutes: Int)
    fun getAchievements(): Flow<List<Achievement>>
    suspend fun unlockAchievement(type: AchievementType)
    suspend fun updateAchievementProgress(type: AchievementType, progress: Float)
}
```

- [ ] **Step 2: Implement SongRepositoryImpl**

`SongRepositoryImpl.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.repository

import android.content.Context
import de.syntrosoft.syntropiano.data.db.dao.SongDao
import de.syntrosoft.syntropiano.data.db.entity.SongEntity
import de.syntrosoft.syntropiano.data.import.SongParser
import de.syntrosoft.syntropiano.domain.model.Note
import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val songParser: SongParser,
    private val context: Context,
) : SongRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllSongs(): Flow<List<Song>> =
        songDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getSongsByLevel(level: Int): Flow<List<Song>> =
        songDao.getByLevel(level).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSongById(id: Long): Song? =
        songDao.getById(id)?.toDomain()

    override suspend fun importSong(song: Song): Long {
        val entity = song.toEntity()
        return songDao.insert(entity)
    }

    override suspend fun seedBuiltInSongs() {
        if (songDao.count() > 0) return
        val assetFiles = context.assets.list("songs") ?: return
        val songs = assetFiles.mapNotNull { filename ->
            val jsonString = context.assets.open("songs/$filename").bufferedReader().readText()
            songParser.parse(jsonString).getOrNull()?.copy(isBuiltIn = true)
        }
        songDao.insertAll(songs.map { it.toEntity() })
    }

    private fun SongEntity.toDomain() = Song(
        id = id, title = title, artist = artist, difficulty = difficulty,
        bpm = bpm, timeSignature = timeSignature, level = level,
        notes = json.decodeFromString(notesJson),
        isBuiltIn = isBuiltIn, importedAt = importedAt,
    )

    private fun Song.toEntity() = SongEntity(
        id = id, title = title, artist = artist, difficulty = difficulty,
        bpm = bpm, timeSignature = timeSignature, level = level,
        notesJson = json.encodeToString(notes),
        isBuiltIn = isBuiltIn, importedAt = importedAt,
    )
}
```

- [ ] **Step 3: Implement ProgressRepositoryImpl**

`ProgressRepositoryImpl.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.data.db.dao.ProgressDao
import de.syntrosoft.syntropiano.data.db.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao,
) : ProgressRepository {

    override suspend fun saveProgress(songId: Long?, lessonId: Long?, stars: Int, accuracy: Float, xpEarned: Int) {
        progressDao.insert(
            ProgressEntity(
                songId = songId, lessonId = lessonId,
                completedAt = System.currentTimeMillis(),
                stars = stars, accuracy = accuracy, xpEarned = xpEarned,
            )
        )
    }

    override suspend fun getBestStarsForSong(songId: Long): Int =
        progressDao.getBestStarsForSong(songId) ?: 0

    override suspend fun countThreeStarSongs(): Int =
        progressDao.countThreeStarSongs()

    override suspend fun countCompletedSongs(): Int =
        progressDao.countCompletedSongs()

    override suspend fun averageAccuracy(): Float =
        progressDao.averageAccuracy() ?: 0f

    override suspend fun isLessonCompleted(lessonId: Long): Boolean =
        progressDao.getForLesson(lessonId) != null

    override fun getAllProgress(): Flow<List<ProgressEntity>> =
        progressDao.getAll()
}
```

- [ ] **Step 4: Implement PlayerRepositoryImpl**

`PlayerRepositoryImpl.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.repository

import de.syntrosoft.syntropiano.data.db.dao.AchievementDao
import de.syntrosoft.syntropiano.data.db.dao.PlayerProfileDao
import de.syntrosoft.syntropiano.data.db.entity.AchievementEntity
import de.syntrosoft.syntropiano.data.db.entity.PlayerProfileEntity
import de.syntrosoft.syntropiano.domain.engine.XpCalculator
import de.syntrosoft.syntropiano.domain.model.Achievement
import de.syntrosoft.syntropiano.domain.model.AchievementType
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class PlayerRepositoryImpl @Inject constructor(
    private val profileDao: PlayerProfileDao,
    private val achievementDao: AchievementDao,
    private val xpCalculator: XpCalculator,
) : PlayerRepository {

    override fun getProfile(): Flow<PlayerProfile> =
        profileDao.get().map { it?.toDomain() ?: PlayerProfile() }

    override suspend fun addXp(amount: Int) {
        val current = profileDao.getOnce() ?: PlayerProfileEntity()
        val newTotalXp = current.totalXp + amount
        val newLevel = xpCalculator.levelForTotalXp(newTotalXp)
        profileDao.upsert(current.copy(totalXp = newTotalXp, currentLevel = newLevel))
    }

    override suspend fun updateStreak() {
        val current = profileDao.getOnce() ?: PlayerProfileEntity()
        val today = LocalDate.now(ZoneId.systemDefault())
        val lastPlayed = current.lastPlayedAt?.let {
            java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        val newStreak = when {
            lastPlayed == today -> current.currentStreak // already counted today
            lastPlayed == today.minusDays(1) -> current.currentStreak + 1
            else -> 1 // streak broken or first time
        }

        profileDao.upsert(
            current.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(current.longestStreak, newStreak),
                lastPlayedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun addPlayTime(minutes: Int) {
        val current = profileDao.getOnce() ?: PlayerProfileEntity()
        profileDao.upsert(current.copy(totalPlayTimeMinutes = current.totalPlayTimeMinutes + minutes))
    }

    override fun getAchievements(): Flow<List<Achievement>> =
        achievementDao.getAll().map { entities ->
            AchievementType.entries.map { type ->
                val entity = entities.find { it.type == type.name }
                Achievement(
                    type = type,
                    unlockedAt = entity?.unlockedAt,
                    progress = entity?.progress ?: 0f,
                )
            }
        }

    override suspend fun unlockAchievement(type: AchievementType) {
        val existing = achievementDao.getByType(type.name)
        if (existing?.unlockedAt != null) return // already unlocked
        achievementDao.upsert(
            AchievementEntity(type = type.name, unlockedAt = System.currentTimeMillis(), progress = 1f)
        )
    }

    override suspend fun updateAchievementProgress(type: AchievementType, progress: Float) {
        val existing = achievementDao.getByType(type.name)
        if (existing?.unlockedAt != null) return // already unlocked
        achievementDao.upsert(
            AchievementEntity(type = type.name, progress = progress.coerceIn(0f, 1f))
        )
    }

    private fun PlayerProfileEntity.toDomain() = PlayerProfile(
        totalXp = totalXp, currentLevel = currentLevel,
        currentStreak = currentStreak, longestStreak = longestStreak,
        lastPlayedAt = lastPlayedAt, totalPlayTimeMinutes = totalPlayTimeMinutes,
    )
}
```

- [ ] **Step 5: Create SongImporter**

`SongImporter.kt`:
```kotlin
package de.syntrosoft.syntropiano.data.import

import android.content.Context
import android.net.Uri
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.model.Song
import javax.inject.Inject

class SongImporter @Inject constructor(
    private val songParser: SongParser,
    private val songRepository: SongRepository,
) {
    suspend fun import(context: Context, uri: Uri): Result<Song> = runCatching {
        val jsonString = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText()
            ?: throw IllegalArgumentException("Could not read file")

        val song = songParser.parse(jsonString).getOrThrow()
        val id = songRepository.importSong(song)
        song.copy(id = id)
    }
}
```

- [ ] **Step 6: Create Hilt modules**

`AppModule.kt`:
```kotlin
package de.syntrosoft.syntropiano.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.syntrosoft.syntropiano.data.db.AppDatabase
import de.syntrosoft.syntropiano.data.db.dao.*
import de.syntrosoft.syntropiano.data.import.SongParser
import de.syntrosoft.syntropiano.domain.audio.NoteMatcher
import de.syntrosoft.syntropiano.domain.audio.PitchDetector
import de.syntrosoft.syntropiano.domain.audio.YinPitchDetector
import de.syntrosoft.syntropiano.domain.engine.AchievementEngine
import de.syntrosoft.syntropiano.domain.engine.ScoreCalculator
import de.syntrosoft.syntropiano.domain.engine.XpCalculator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "syntropiano.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSongDao(db: AppDatabase): SongDao = db.songDao()
    @Provides fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()
    @Provides fun providePlayerProfileDao(db: AppDatabase): PlayerProfileDao = db.playerProfileDao()
    @Provides fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()

    @Provides @Singleton fun provideSongParser(): SongParser = SongParser()
    @Provides @Singleton fun provideScoreCalculator(): ScoreCalculator = ScoreCalculator()
    @Provides @Singleton fun provideXpCalculator(): XpCalculator = XpCalculator()
    @Provides @Singleton fun provideAchievementEngine(): AchievementEngine = AchievementEngine()
    @Provides @Singleton fun provideNoteMatcher(): NoteMatcher = NoteMatcher()
    @Provides @Singleton fun providePitchDetector(): PitchDetector = YinPitchDetector()

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context
}
```

`RepositoryModule.kt`:
```kotlin
package de.syntrosoft.syntropiano.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.syntrosoft.syntropiano.data.repository.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository

    @Binds @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository
}
```

- [ ] **Step 7: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/data/
git add app/src/main/java/de/syntrosoft/syntropiano/di/
git commit -m "feat: add repositories, SongImporter, and Hilt DI modules"
```

---

## Task 8: Audio Capture Service

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/audio/AudioCaptureService.kt`

- [ ] **Step 1: Implement AudioCaptureService**

`AudioCaptureService.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import de.syntrosoft.syntropiano.domain.audio.PitchDetector
import de.syntrosoft.syntropiano.domain.model.Pitch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCaptureService @Inject constructor(
    private val pitchDetector: PitchDetector,
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 4096
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    private val _detectedPitch = MutableStateFlow<Pitch?>(null)
    val detectedPitch: StateFlow<Pitch?> = _detectedPitch

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun start(scope: CoroutineScope) {
        if (_isListening.value) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, BUFFER_SIZE * 2),
        )

        audioRecord?.startRecording()
        _isListening.value = true

        captureJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(BUFFER_SIZE)
            while (isActive && _isListening.value) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: break
                if (read > 0) {
                    val pitch = pitchDetector.detect(buffer, SAMPLE_RATE)
                    _detectedPitch.value = pitch
                }
            }
        }
    }

    fun stop() {
        _isListening.value = false
        captureJob?.cancel()
        captureJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _detectedPitch.value = null
    }
}
```

- [ ] **Step 2: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/audio/
git commit -m "feat: add AudioCaptureService with real-time pitch detection"
```

---

## Task 9: Navigation & Home Screen

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/Screen.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/home/HomeScreen.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/home/HomeViewModel.kt`
- Modify: `app/src/main/java/de/syntrosoft/syntropiano/MainActivity.kt`

- [ ] **Step 1: Create navigation routes**

`Screen.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Learn : Screen("learn")
    data object Lesson : Screen("lesson/{lessonLevel}/{lessonOrder}") {
        fun createRoute(level: Int, order: Int) = "lesson/$level/$order"
    }
    data object SongList : Screen("songs")
    data object Play : Screen("play/{songId}/{mode}") {
        fun createRoute(songId: Long, mode: String) = "play/$songId/$mode"
    }
    data object Profile : Screen("profile")
}
```

- [ ] **Step 2: Create HomeScreen**

`HomeViewModel.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val songRepository: SongRepository,
) : ViewModel() {

    val profile = playerRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfile())

    init {
        viewModelScope.launch { songRepository.seedBuiltInSongs() }
    }
}
```

`HomeScreen.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToLearn: () -> Unit,
    onNavigateToSongList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "SyntroPiano",
            style = MaterialTheme.typography.headlineLarge,
            color = Blue400,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${profile.rank.title} · Level ${profile.currentLevel}",
            style = MaterialTheme.typography.bodyMedium,
            color = Orange400,
        )

        if (profile.currentStreak > 0) {
            Text(
                text = "\uD83D\uDD25 ${profile.currentStreak} Tage Streak",
                style = MaterialTheme.typography.bodySmall,
                color = Red400,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HomeCard(
            title = "Lernen",
            subtitle = "Strukturierter Kurs",
            gradientColors = listOf(Blue400, Color(0xFF1565C0)),
            onClick = onNavigateToLearn,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeCard(
            title = "Spielen",
            subtitle = "Freies Üben",
            gradientColors = listOf(Green400, Color(0xFF2E7D32)),
            onClick = onNavigateToSongList,
        )

        Spacer(modifier = Modifier.height(16.dp))

        HomeCard(
            title = "Profil",
            subtitle = "Fortschritt & Achievements",
            gradientColors = listOf(Orange400, Color(0xFFE65100)),
            onClick = onNavigateToProfile,
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradientColors))
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column {
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = subtitle, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
```

- [ ] **Step 3: Create NavGraph**

`NavGraph.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.syntrosoft.syntropiano.ui.screens.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLearn = { navController.navigate(Screen.Learn.route) },
                onNavigateToSongList = { navController.navigate(Screen.SongList.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
            )
        }

        composable(Screen.Learn.route) {
            // LearnScreen — implemented in Task 11
        }

        composable(Screen.SongList.route) {
            // SongListScreen — implemented in Task 12
        }

        composable(
            Screen.Play.route,
            arguments = listOf(
                navArgument("songId") { type = NavType.LongType },
                navArgument("mode") { type = NavType.StringType },
            ),
        ) {
            // PlayScreen — implemented in Task 13
        }

        composable(Screen.Profile.route) {
            // ProfileScreen — implemented in Task 14
        }
    }
}
```

- [ ] **Step 4: Update MainActivity**

`MainActivity.kt`:
```kotlin
package de.syntrosoft.syntropiano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.syntrosoft.syntropiano.ui.navigation.NavGraph
import de.syntrosoft.syntropiano.ui.theme.SyntroPianoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyntroPianoTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
```

- [ ] **Step 5: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/
git add app/src/main/java/de/syntrosoft/syntropiano/ui/screens/home/
git add app/src/main/java/de/syntrosoft/syntropiano/MainActivity.kt
git commit -m "feat: add navigation and HomeScreen with Learn/Play/Profile cards"
```

---

## Task 10: UI Components (Keyboard, SheetMusic, Gamification)

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/components/KeyboardView.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/components/SheetMusicView.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/components/StarRating.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/components/XpBar.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/components/StreakBadge.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/components/AchievementCard.kt`

- [ ] **Step 1: Create KeyboardView**

`KeyboardView.kt`:
```kotlin
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
```

- [ ] **Step 2: Create SheetMusicView**

`SheetMusicView.kt`:
```kotlin
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
```

- [ ] **Step 3: Create gamification components**

`StarRating.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun StarRating(stars: Int, maxStars: Int = 3, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        repeat(maxStars) { i ->
            Text(
                text = if (i < stars) "\u2B50" else "\u2606",
                fontSize = 24.sp,
            )
        }
    }
}
```

`XpBar.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.syntrosoft.syntropiano.ui.theme.Orange400

@Composable
fun XpBar(
    currentXp: Int,
    requiredXp: Int,
    level: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Level $level", style = MaterialTheme.typography.labelSmall, color = Orange400)
            Text("$currentXp / $requiredXp XP", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF333333)),
        ) {
            val fraction = (currentXp.toFloat() / requiredXp).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(Orange400, Color(0xFFFF9800)))),
            )
        }
    }
}
```

`StreakBadge.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syntrosoft.syntropiano.ui.theme.Red400

@Composable
fun StreakBadge(streakDays: Int, modifier: Modifier = Modifier) {
    if (streakDays <= 0) return
    Row(
        modifier = modifier
            .background(Red400.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("\uD83D\uDD25", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "$streakDays Tage",
            style = MaterialTheme.typography.labelSmall,
            color = Red400,
        )
    }
}
```

`AchievementCard.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syntrosoft.syntropiano.domain.model.Achievement

@Composable
fun AchievementCard(achievement: Achievement, modifier: Modifier = Modifier) {
    val alpha = if (achievement.isUnlocked) 1f else 0.4f
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = achievementEmoji(achievement.type.name),
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = achievement.type.title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = alpha),
        )
        Text(
            text = achievement.type.description,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = alpha),
        )
    }
}

private fun achievementEmoji(type: String): String = when (type) {
    "FIRST_NOTE" -> "\uD83C\uDFB5"
    "STREAK_7" -> "\uD83D\uDD25"
    "STAR_COLLECTOR" -> "\u2B50"
    "BOTH_HANDS" -> "\uD83C\uDFB9"
    "PERFECTIONIST" -> "\uD83D\uDCAF"
    "MASTER" -> "\uD83C\uDFC6"
    else -> "\uD83C\uDFB5"
}
```

- [ ] **Step 4: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/components/
git commit -m "feat: add UI components (KeyboardView, SheetMusic, StarRating, XpBar, StreakBadge, AchievementCard)"
```

---

## Task 11: Learn Screen (Course Overview)

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/learn/LearnScreen.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/learn/LearnViewModel.kt`
- Modify: `app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create LearnViewModel**

`LearnViewModel.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelInfo(
    val level: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _levels = MutableStateFlow<List<LevelInfo>>(emptyList())
    val levels: StateFlow<List<LevelInfo>> = _levels

    private val levelDefinitions = listOf(
        Triple(1, "Noten Entdecker", "Notenschlüssel, Notenlinien, C-H"),
        Triple(2, "Erste Melodien", "Taktarten, Notenwerte, Kinderlieder"),
        Triple(3, "Rhythmus & Vorzeichen", "Pausen, Punktierungen, ♯ ♭"),
        Triple(4, "Beide Hände", "Bassschlüssel, Koordination"),
        Triple(5, "Fortgeschritten", "Akkorde, Dynamik, Pedalnutzung"),
    )

    init {
        loadLevels()
    }

    fun loadLevels() {
        viewModelScope.launch {
            val levelInfos = levelDefinitions.map { (level, title, desc) ->
                // Level-Test lesson IDs follow pattern: level * 100 + 99 (e.g. 199, 299)
                val testLessonId = level.toLong() * 100 + 99
                val isCompleted = progressRepository.isLessonCompleted(testLessonId)
                LevelInfo(
                    level = level,
                    title = title,
                    description = desc,
                    isUnlocked = level == 1 || progressRepository.isLessonCompleted((level - 1).toLong() * 100 + 99),
                    isCompleted = isCompleted,
                )
            }
            _levels.value = levelInfos
        }
    }
}
```

- [ ] **Step 2: Create LearnScreen**

`LearnScreen.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.theme.*

private val levelColors = listOf(Blue400, Green400, Orange400, Red400, Purple300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val levels by viewModel.levels.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lernen") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            itemsIndexed(levels) { index, level ->
                LevelCard(
                    level = level,
                    color = levelColors[index % levelColors.size],
                    onClick = { if (level.isUnlocked) onLevelSelected(level.level) },
                )
            }
        }
    }
}

@Composable
private fun LevelCard(level: LevelInfo, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = level.isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (level.isUnlocked) color else Color.Gray),
                contentAlignment = Alignment.Center,
            ) {
                if (level.isUnlocked) {
                    Text("${level.level}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(level.title, style = MaterialTheme.typography.titleLarge, color = if (level.isUnlocked) Color.White else Color.Gray)
                Text(level.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            if (level.isCompleted) {
                Text("\u2705", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
```

- [ ] **Step 3: Wire LearnScreen into NavGraph**

In `NavGraph.kt`, replace the `Screen.Learn` composable placeholder:
```kotlin
composable(Screen.Learn.route) {
    LearnScreen(
        onBack = { navController.popBackStack() },
        onLevelSelected = { level ->
            // Navigate to first lesson of this level — will be wired in Task 12
        },
    )
}
```

- [ ] **Step 4: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/screens/learn/
git add app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt
git commit -m "feat: add LearnScreen with level overview and navigation"
```

---

## Task 12: Song List Screen

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/play/SongListScreen.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/play/SongListViewModel.kt`
- Modify: `app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create SongListViewModel**

`SongListViewModel.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.model.Song
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongWithStars(val song: Song, val bestStars: Int)

@HiltViewModel
class SongListViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _songs = MutableStateFlow<List<SongWithStars>>(emptyList())
    val songs: StateFlow<List<SongWithStars>> = _songs

    init {
        viewModelScope.launch {
            songRepository.getAllSongs().collect { songList ->
                _songs.value = songList.map { song ->
                    SongWithStars(song, progressRepository.getBestStarsForSong(song.id))
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create SongListScreen**

`SongListScreen.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.play

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.components.StarRating
import de.syntrosoft.syntropiano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    onBack: () -> Unit,
    onSongSelected: (songId: Long) -> Unit,
    viewModel: SongListViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lieder") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(songs, key = { it.song.id }) { songWithStars ->
                SongCard(songWithStars, onClick = { onSongSelected(songWithStars.song.id) })
            }
        }
    }
}

@Composable
private fun SongCard(songWithStars: SongWithStars, onClick: () -> Unit) {
    val song = songWithStars.song
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                if (song.artist.isNotBlank()) {
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(
                    "Schwierigkeit: ${"★".repeat(song.difficulty)}${"☆".repeat(5 - song.difficulty)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Orange400,
                )
            }
            if (songWithStars.bestStars > 0) {
                StarRating(stars = songWithStars.bestStars)
            }
        }
    }
}
```

- [ ] **Step 3: Wire SongListScreen into NavGraph**

In `NavGraph.kt`, replace the `Screen.SongList` composable placeholder:
```kotlin
composable(Screen.SongList.route) {
    SongListScreen(
        onBack = { navController.popBackStack() },
        onSongSelected = { songId ->
            navController.navigate(Screen.Play.createRoute(songId, "PRACTICE"))
        },
    )
}
```

- [ ] **Step 4: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/screens/play/SongList*
git add app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt
git commit -m "feat: add SongListScreen with difficulty and star ratings"
```

---

## Task 13: Play Screen (Core Gameplay)

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/play/PlayScreen.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/play/PlayViewModel.kt`
- Modify: `app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create PlayViewModel**

`PlayViewModel.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.audio.NoteMatcher
import de.syntrosoft.syntropiano.domain.engine.AchievementEngine
import de.syntrosoft.syntropiano.domain.engine.ScoreCalculator
import de.syntrosoft.syntropiano.domain.model.*
import de.syntrosoft.syntropiano.ui.audio.AudioCaptureService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    private val progressRepository: ProgressRepository,
    private val playerRepository: PlayerRepository,
    private val noteMatcher: NoteMatcher,
    private val scoreCalculator: ScoreCalculator,
    private val achievementEngine: AchievementEngine,
    val audioCaptureService: AudioCaptureService,
) : ViewModel() {

    private val songId: Long = savedStateHandle["songId"] ?: 0L
    private val modeString: String = savedStateHandle["mode"] ?: "PRACTICE"

    private val _session = MutableStateFlow<PlaySession?>(null)
    val session: StateFlow<PlaySession?> = _session

    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult

    init {
        viewModelScope.launch {
            val song = songRepository.getSongById(songId) ?: return@launch
            val mode = PlayMode.valueOf(modeString)
            _session.value = PlaySession(song = song, mode = mode)
        }

        // Listen to pitch detection
        viewModelScope.launch {
            audioCaptureService.detectedPitch.collect { pitch ->
                if (pitch != null) onPitchDetected(pitch)
            }
        }
    }

    fun startListening() {
        audioCaptureService.start(viewModelScope)
    }

    fun stopListening() {
        audioCaptureService.stop()
    }

    private fun onPitchDetected(pitch: Pitch) {
        val current = _session.value ?: return
        if (current.isFinished) return
        if (current.currentNoteIndex >= current.song.notes.size) return

        val expectedNote = current.song.notes[current.currentNoteIndex]
        val result = noteMatcher.match(expectedNote, pitch)

        // In practice mode, show wrong note feedback but don't advance
        if (current.mode == PlayMode.PRACTICE && result.status != NoteResult.Status.CORRECT) {
            // Show the wrong note (red feedback) but stay on current note
            _session.value = current.copy(
                noteResults = current.noteResults + result,
            )
            return
        }

        val updatedResults = current.noteResults + result
        val nextIndex = current.currentNoteIndex + 1
        val isFinished = nextIndex >= current.song.notes.size

        _session.value = current.copy(
            noteResults = updatedResults,
            currentNoteIndex = nextIndex,
            isFinished = isFinished,
        )

        if (isFinished) {
            onSessionFinished(current.copy(noteResults = updatedResults, isFinished = true))
        }
    }

    private fun onSessionFinished(session: PlaySession) {
        viewModelScope.launch {
            stopListening()
            _showResult.value = true

            val stars = scoreCalculator.calculateStars(session.accuracy)
            val xp = scoreCalculator.calculateXp(session.accuracy, stars, isLevelTest = false)

            progressRepository.saveProgress(
                songId = session.song.id,
                lessonId = null,
                stars = stars,
                accuracy = session.accuracy,
                xpEarned = xp,
            )

            playerRepository.addXp(xp)
            playerRepository.updateStreak()

            // Check achievements
            if (achievementEngine.check(AchievementType.FIRST_NOTE, totalCorrectNotes = session.correctNotes)) {
                playerRepository.unlockAchievement(AchievementType.FIRST_NOTE)
            }
            if (session.accuracy >= 1.0f && achievementEngine.check(AchievementType.PERFECTIONIST, hasPerfectSong = true)) {
                playerRepository.unlockAchievement(AchievementType.PERFECTIONIST)
            }
        }
    }

    fun retry() {
        val current = _session.value ?: return
        _session.value = PlaySession(song = current.song, mode = current.mode)
        _showResult.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
```

- [ ] **Step 2: Create PlayScreen**

`PlayScreen.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.play

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.components.KeyboardView
import de.syntrosoft.syntropiano.ui.components.SheetMusicView
import de.syntrosoft.syntropiano.ui.components.StarRating
import de.syntrosoft.syntropiano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    onBack: () -> Unit,
    viewModel: PlayViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val showResult by viewModel.showResult.collectAsStateWithLifecycle()
    val isListening by viewModel.audioCaptureService.isListening.collectAsStateWithLifecycle()
    val detectedPitch by viewModel.audioCaptureService.detectedPitch.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.song?.title ?: "Laden...") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopListening()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val currentSession = session
        if (currentSession == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (showResult) {
            ResultScreen(
                session = currentSession,
                onRetry = { viewModel.retry() },
                onBack = onBack,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // Song info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${currentSession.song.bpm} BPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                Text(
                    "${currentSession.correctNotes}/${currentSession.totalNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Orange400,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sheet music
            SheetMusicView(
                notes = currentSession.song.notes,
                noteResults = currentSession.noteResults,
                currentNoteIndex = currentSession.currentNoteIndex,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Keyboard visualization
            val currentNote = currentSession.song.notes.getOrNull(currentSession.currentNoteIndex)
            val highlightPitch = currentNote?.toPitch()
            val lastResult = currentSession.noteResults.lastOrNull()

            KeyboardView(
                highlightPitch = highlightPitch,
                detectedPitch = detectedPitch,
                isCorrect = lastResult?.let { it.status == de.syntrosoft.syntropiano.domain.model.NoteResult.Status.CORRECT },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Microphone status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isListening) Color(0xFF1A2E1A) else DarkSurface,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (isListening) "\uD83C\uDFA4 Mikrofon aktiv" else "\uD83C\uDFA4 Mikrofon aus",
                        color = if (isListening) Green400 else Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (detectedPitch != null) {
                        Text(
                            "Erkannt: ${detectedPitch?.displayName}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start/Stop button
            Button(
                onClick = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else if (viewModel.audioCaptureService.hasPermission(context)) {
                        viewModel.startListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Red400 else Green400,
                ),
            ) {
                Text(
                    if (isListening) "Stopp" else "Start",
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun ResultScreen(
    session: de.syntrosoft.syntropiano.domain.model.PlaySession,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Geschafft!", style = MaterialTheme.typography.headlineLarge, color = Color.White)

        Spacer(modifier = Modifier.height(24.dp))

        StarRating(stars = session.stars)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "${(session.accuracy * 100).toInt()}% Genauigkeit",
            style = MaterialTheme.typography.titleLarge,
            color = Orange400,
        )

        Text(
            "${session.correctNotes} von ${session.totalNotes} Noten richtig",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Nochmal")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Zurück")
        }
    }
}
```

- [ ] **Step 3: Wire PlayScreen into NavGraph**

In `NavGraph.kt`, replace the `Screen.Play` composable placeholder:
```kotlin
composable(
    Screen.Play.route,
    arguments = listOf(
        navArgument("songId") { type = NavType.LongType },
        navArgument("mode") { type = NavType.StringType },
    ),
) {
    PlayScreen(onBack = { navController.popBackStack() })
}
```

- [ ] **Step 4: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/screens/play/Play*
git add app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt
git commit -m "feat: add PlayScreen with real-time pitch detection and scoring"
```

---

## Task 14: Profile Screen

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/profile/ProfileScreen.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/profile/ProfileViewModel.kt`
- Modify: `app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create ProfileViewModel**

`ProfileViewModel.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.domain.engine.XpCalculator
import de.syntrosoft.syntropiano.domain.model.Achievement
import de.syntrosoft.syntropiano.domain.model.PlayerProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val completedSongs: Int = 0,
    val averageAccuracy: Float = 0f,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val progressRepository: ProgressRepository,
    val xpCalculator: XpCalculator,
) : ViewModel() {

    val profile = playerRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerProfile())

    val achievements = playerRepository.getAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats

    init {
        viewModelScope.launch {
            _stats.value = ProfileStats(
                completedSongs = progressRepository.countCompletedSongs(),
                averageAccuracy = progressRepository.averageAccuracy(),
            )
        }
    }
}
```

- [ ] **Step 2: Create ProfileScreen**

`ProfileScreen.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.syntrosoft.syntropiano.ui.components.AchievementCard
import de.syntrosoft.syntropiano.ui.components.StreakBadge
import de.syntrosoft.syntropiano.ui.components.XpBar
import de.syntrosoft.syntropiano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val (xpInLevel, xpNeeded) = viewModel.xpCalculator.xpProgressInLevel(profile.totalXp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            // Avatar & Level
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Blue400, Purple300))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\uD83C\uDFB9", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(profile.rank.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        StreakBadge(profile.currentStreak)
                    }
                }
            }

            // XP Bar
            item {
                XpBar(currentXp = xpInLevel, requiredXp = xpNeeded, level = profile.currentLevel)
            }

            // Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard("${profile.currentStreak}", "Streak", Red400, Modifier.weight(1f))
                    StatCard("${stats.completedSongs}", "Lieder", Green400, Modifier.weight(1f))
                    StatCard("${(stats.averageAccuracy * 100).toInt()}%", "Genauigkeit", Orange400, Modifier.weight(1f))
                    StatCard("${profile.totalPlayTimeMinutes / 60}h", "Übungszeit", Purple300, Modifier.weight(1f))
                }
            }

            // Achievements
            item {
                Text("Achievements", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(((achievements.size + 1) / 2 * 120).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(achievements) { achievement ->
                        AchievementCard(achievement)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}
```

- [ ] **Step 3: Wire ProfileScreen into NavGraph**

In `NavGraph.kt`, replace the `Screen.Profile` composable placeholder:
```kotlin
composable(Screen.Profile.route) {
    ProfileScreen(onBack = { navController.popBackStack() })
}
```

- [ ] **Step 4: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/screens/profile/
git add app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt
git commit -m "feat: add ProfileScreen with stats, XP bar, streak, and achievements"
```

---

## Task 15: Lesson Screen, NoteQuiz & Kurs-Inhalte

**Files:**
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/components/NoteQuiz.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/learn/LessonScreen.kt`
- Create: `app/src/main/java/de/syntrosoft/syntropiano/ui/screens/learn/LessonViewModel.kt`
- Modify: `app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create NoteQuiz component**

`NoteQuiz.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.components

import androidx.compose.foundation.background
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
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.ui.theme.*

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
                de.syntrosoft.syntropiano.domain.model.Note(
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
```

- [ ] **Step 2: Create LessonViewModel**

`LessonViewModel.kt`:
```kotlin
package de.syntrosoft.syntropiano.ui.screens.learn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.syntrosoft.syntropiano.data.repository.PlayerRepository
import de.syntrosoft.syntropiano.data.repository.ProgressRepository
import de.syntrosoft.syntropiano.data.repository.SongRepository
import de.syntrosoft.syntropiano.domain.engine.ScoreCalculator
import de.syntrosoft.syntropiano.domain.model.Pitch
import de.syntrosoft.syntropiano.domain.model.Song
import de.syntrosoft.syntropiano.ui.components.QuizQuestion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LessonStep { THEORY, QUIZ, SONG_PRACTICE, TEST, COMPLETED }

data class LessonState(
    val level: Int = 1,
    val currentStep: LessonStep = LessonStep.THEORY,
    val theoryTitle: String = "",
    val theoryContent: String = "",
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val currentQuizIndex: Int = 0,
    val quizCorrect: Int = 0,
    val quizTotal: Int = 0,
    val levelSongs: List<Song> = emptyList(),
    val testPassed: Boolean = false,
)

@HiltViewModel
class LessonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    private val progressRepository: ProgressRepository,
    private val playerRepository: PlayerRepository,
    private val scoreCalculator: ScoreCalculator,
) : ViewModel() {

    private val level: Int = savedStateHandle["lessonLevel"] ?: 1

    private val _state = MutableStateFlow(LessonState(level = level))
    val state: StateFlow<LessonState> = _state

    init {
        loadLessonContent()
    }

    private fun loadLessonContent() {
        viewModelScope.launch {
            // Generate theory content based on level
            val (title, content) = getTheoryForLevel(level)
            val quizzes = generateQuizQuestions(level)

            songRepository.getSongsByLevel(level).collect { songs ->
                _state.value = _state.value.copy(
                    theoryTitle = title,
                    theoryContent = content,
                    quizQuestions = quizzes,
                    levelSongs = songs,
                )
            }
        }
    }

    fun advanceToQuiz() {
        _state.value = _state.value.copy(currentStep = LessonStep.QUIZ)
    }

    fun onQuizAnswer(correct: Boolean) {
        val current = _state.value
        val newCorrect = if (correct) current.quizCorrect + 1 else current.quizCorrect
        val newTotal = current.quizTotal + 1
        val nextIndex = current.currentQuizIndex + 1

        _state.value = current.copy(
            quizCorrect = newCorrect,
            quizTotal = newTotal,
            currentQuizIndex = nextIndex,
        )

        if (nextIndex >= current.quizQuestions.size) {
            _state.value = _state.value.copy(currentStep = LessonStep.SONG_PRACTICE)
        }
    }

    fun advanceToTest() {
        _state.value = _state.value.copy(currentStep = LessonStep.TEST)
    }

    fun onTestCompleted(accuracy: Float) {
        val passed = accuracy >= 0.70f
        viewModelScope.launch {
            if (passed) {
                val testLessonId = level.toLong() * 100 + 99
                val xp = 200 // level test XP
                progressRepository.saveProgress(
                    songId = null,
                    lessonId = testLessonId,
                    stars = scoreCalculator.calculateStars(accuracy),
                    accuracy = accuracy,
                    xpEarned = xp,
                )
                playerRepository.addXp(xp)
            }
            _state.value = _state.value.copy(
                currentStep = LessonStep.COMPLETED,
                testPassed = passed,
            )
        }
    }

    private fun getTheoryForLevel(level: Int): Pair<String, String> = when (level) {
        1 -> "Noten Entdecker" to """
            Willkommen in der Welt der Musik!

            Das Notensystem besteht aus 5 Linien. Darauf werden die Noten platziert.

            Die 7 Grundnoten heißen: C - D - E - F - G - A - H

            Auf dem Keyboard findest du sie als weiße Tasten.
            Die Note C ist immer links neben den 2 schwarzen Tasten.

            Probiere es aus: Finde das C auf deinem Keyboard!
        """.trimIndent()
        2 -> "Erste Melodien" to """
            Super, du kennst jetzt die Noten!

            Noten haben verschiedene Längen:
            • Ganze Note = 4 Schläge ○
            • Halbe Note = 2 Schläge 𝅗𝅥
            • Viertelnote = 1 Schlag ♩

            Die meisten Lieder sind im 4/4-Takt: 4 Schläge pro Takt.

            Jetzt spielen wir deine ersten Melodien!
        """.trimIndent()
        3 -> "Rhythmus & Vorzeichen" to """
            Zeit für Vorzeichen!

            ♯ (Kreuz) = einen Halbton höher → schwarze Taste rechts
            ♭ (Be) = einen Halbton tiefer → schwarze Taste links

            Neue Notenwerte:
            • Achtel = halber Schlag ♪
            • Punktierte Note = 1,5× so lang
            • Pause = Stille (gleiche Längen wie Noten)
        """.trimIndent()
        4 -> "Beide Hände" to """
            Jetzt wird es spannend: Beide Hände!

            Die linke Hand spielt im Bassschlüssel (unteres System).
            Die rechte Hand spielt im Violinschlüssel (oberes System).

            Fange langsam an: Erst links alleine üben,
            dann rechts, dann zusammen.
        """.trimIndent()
        else -> "Fortgeschritten" to """
            Du bist jetzt ein erfahrener Spieler!

            Akkorde: Mehrere Noten gleichzeitig spielen.
            Dynamik: laut (f) und leise (p) spielen.

            Übe die Stücke in verschiedenen Tempi!
        """.trimIndent()
    }

    private fun generateQuizQuestions(level: Int): List<QuizQuestion> {
        val notesForLevel = when (level) {
            1 -> listOf(Pitch.C4, Pitch.D4, Pitch.E4, Pitch.F4, Pitch.G4, Pitch.A4, Pitch.B4)
            2 -> listOf(Pitch.C4, Pitch.D4, Pitch.E4, Pitch.F4, Pitch.G4, Pitch.A4, Pitch.B4, Pitch.C5)
            else -> Pitch.entries.filter { it.frequency >= Pitch.C3.frequency && it.frequency <= Pitch.C6.frequency }
        }

        return (1..5).map {
            val correct = notesForLevel.random()
            val wrongOptions = notesForLevel.filter { it != correct }.shuffled().take(3)
            QuizQuestion(
                correctPitch = correct,
                options = (wrongOptions + correct).shuffled(),
            )
        }
    }
}
```

- [ ] **Step 3: Create LessonScreen**

`LessonScreen.kt`:
```kotlin
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
import de.syntrosoft.syntropiano.ui.theme.*

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
```

- [ ] **Step 4: Wire LessonScreen into NavGraph**

In `NavGraph.kt`, add the Lesson composable and update Learn navigation:
```kotlin
composable(Screen.Learn.route) {
    LearnScreen(
        onBack = { navController.popBackStack() },
        onLevelSelected = { level ->
            navController.navigate(Screen.Lesson.createRoute(level, 1))
        },
    )
}

composable(
    Screen.Lesson.route,
    arguments = listOf(
        navArgument("lessonLevel") { type = NavType.IntType },
        navArgument("lessonOrder") { type = NavType.IntType },
    ),
) {
    LessonScreen(
        onBack = { navController.popBackStack() },
        onPlaySong = { songId, mode ->
            navController.navigate(Screen.Play.createRoute(songId, mode))
        },
    )
}
```

- [ ] **Step 5: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/de/syntrosoft/syntropiano/ui/components/NoteQuiz.kt
git add app/src/main/java/de/syntrosoft/syntropiano/ui/screens/learn/Lesson*
git add app/src/main/java/de/syntrosoft/syntropiano/ui/navigation/NavGraph.kt
git commit -m "feat: add LessonScreen with theory, quiz, song practice, and level test"
```

---

## Task 16: Built-in Song Content

**Files:**
- Create: `app/src/main/assets/songs/alle-meine-entchen.json`
- Create: `app/src/main/assets/songs/haenschen-klein.json`
- Create: `app/src/main/assets/songs/freude-schoener-goetterfunken.json`

- [ ] **Step 1: Create song JSON files**

`alle-meine-entchen.json`:
```json
{
  "title": "Alle meine Entchen",
  "artist": "Volkslied",
  "difficulty": 1,
  "bpm": 100,
  "timeSignature": "4/4",
  "level": 1,
  "notes": [
    {"pitch": "C4", "duration": 1.0, "beat": 0, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 1, "hand": "R"},
    {"pitch": "E4", "duration": 1.0, "beat": 2, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 3, "hand": "R"},
    {"pitch": "G4", "duration": 2.0, "beat": 4, "hand": "R"},
    {"pitch": "G4", "duration": 2.0, "beat": 6, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 8, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 9, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 10, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 11, "hand": "R"},
    {"pitch": "G4", "duration": 4.0, "beat": 12, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 16, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 17, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 18, "hand": "R"},
    {"pitch": "A4", "duration": 1.0, "beat": 19, "hand": "R"},
    {"pitch": "G4", "duration": 4.0, "beat": 20, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 24, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 25, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 26, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 27, "hand": "R"},
    {"pitch": "E4", "duration": 2.0, "beat": 28, "hand": "R"},
    {"pitch": "E4", "duration": 2.0, "beat": 30, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 32, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 33, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 34, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 35, "hand": "R"},
    {"pitch": "C4", "duration": 4.0, "beat": 36, "hand": "R"}
  ]
}
```

`haenschen-klein.json`:
```json
{
  "title": "Hänschen klein",
  "artist": "Volkslied",
  "difficulty": 1,
  "bpm": 110,
  "timeSignature": "4/4",
  "level": 1,
  "notes": [
    {"pitch": "G4", "duration": 1.0, "beat": 0, "hand": "R"},
    {"pitch": "E4", "duration": 1.0, "beat": 1, "hand": "R"},
    {"pitch": "E4", "duration": 2.0, "beat": 2, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 4, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 5, "hand": "R"},
    {"pitch": "D4", "duration": 2.0, "beat": 6, "hand": "R"},
    {"pitch": "C4", "duration": 1.0, "beat": 8, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 9, "hand": "R"},
    {"pitch": "E4", "duration": 1.0, "beat": 10, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 11, "hand": "R"},
    {"pitch": "G4", "duration": 1.0, "beat": 12, "hand": "R"},
    {"pitch": "G4", "duration": 1.0, "beat": 13, "hand": "R"},
    {"pitch": "G4", "duration": 2.0, "beat": 14, "hand": "R"}
  ]
}
```

`freude-schoener-goetterfunken.json`:
```json
{
  "title": "Freude schöner Götterfunken",
  "artist": "L. v. Beethoven",
  "difficulty": 2,
  "bpm": 90,
  "timeSignature": "4/4",
  "level": 2,
  "notes": [
    {"pitch": "E4", "duration": 1.0, "beat": 0, "hand": "R"},
    {"pitch": "E4", "duration": 1.0, "beat": 1, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 2, "hand": "R"},
    {"pitch": "G4", "duration": 1.0, "beat": 3, "hand": "R"},
    {"pitch": "G4", "duration": 1.0, "beat": 4, "hand": "R"},
    {"pitch": "F4", "duration": 1.0, "beat": 5, "hand": "R"},
    {"pitch": "E4", "duration": 1.0, "beat": 6, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 7, "hand": "R"},
    {"pitch": "C4", "duration": 1.0, "beat": 8, "hand": "R"},
    {"pitch": "C4", "duration": 1.0, "beat": 9, "hand": "R"},
    {"pitch": "D4", "duration": 1.0, "beat": 10, "hand": "R"},
    {"pitch": "E4", "duration": 1.0, "beat": 11, "hand": "R"},
    {"pitch": "E4", "duration": 1.5, "beat": 12, "hand": "R"},
    {"pitch": "D4", "duration": 0.5, "beat": 13.5, "hand": "R"},
    {"pitch": "D4", "duration": 2.0, "beat": 14, "hand": "R"}
  ]
}
```

- [ ] **Step 2: Verify project builds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/songs/
git commit -m "feat: add built-in songs (Alle meine Entchen, Hänschen klein, Freude schöner Götterfunken)"
```

---

## Task 17: Final Integration & Run

- [ ] **Step 1: Add .gitignore**

Create `.gitignore` in project root:
```
*.iml
.gradle/
local.properties
.idea/
build/
app/build/
.superpowers/
captures/
*.apk
*.aab
```

- [ ] **Step 2: Run all unit tests**

Run: `./gradlew test -v`
Expected: All tests PASS

- [ ] **Step 3: Build release APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, APK at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 4: Final commit**

```bash
git add .gitignore
git commit -m "chore: add .gitignore and finalize project setup"
```
