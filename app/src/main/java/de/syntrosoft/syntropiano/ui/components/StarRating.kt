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
