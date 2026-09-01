package com.dirac.mactrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A composite icon for a meal or recipe: the icons of its foods bunched together in a small rounded
// tile (up to four, in a 2x2 grid), MacroFactor-style. Falls back to a single glyph when there are no
// ingredient icons (or exactly one).
@Composable
fun ClusterIcon(
    emojis: List<String>,
    modifier: Modifier = Modifier,
    fallback: String = "🍽️",
    tileSize: Dp = 40.dp
) {
    val shown = emojis.take(4)
    val big = (tileSize.value * 0.5f).sp
    val cell = (tileSize.value * 0.34f).sp
    Box(
        modifier = modifier
            .size(tileSize)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when (shown.size) {
            0 -> Text(fallback, fontSize = big)
            1 -> Text(shown[0], fontSize = big)
            else -> Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Text(shown[0], fontSize = cell)
                    if (shown.size >= 2) Text(shown[1], fontSize = cell)
                }
                if (shown.size >= 3) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text(shown[2], fontSize = cell)
                        if (shown.size >= 4) Text(shown[3], fontSize = cell)
                    }
                }
            }
        }
    }
}
