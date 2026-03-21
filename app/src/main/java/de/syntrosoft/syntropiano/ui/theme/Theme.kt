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
