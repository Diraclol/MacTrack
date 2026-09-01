package com.dirac.mactrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// Emoji sets shared by the avatar picker (profile) and the custom-food icon picker.
val AVATAR_EMOJIS = listOf(
    "🧑", "👩", "👨", "🧔", "👵", "👴",
    "🧑‍🦰", "🧑‍🦱", "🧑‍🦳", "🧑‍🦲", "👱", "🧑‍🎤",
    "💪", "🏃", "🏋️", "🚴", "🧘", "🤸",
    "🥗", "🍎", "🥦", "🥑", "🍳", "🔥",
    "⭐", "🎯", "🏆", "💯", "⚡", "🚀",
    "🐻", "🐱", "🐶", "🦊", "🐼", "🦁",
    "🐯", "🐸", "🐵", "🐨", "🦄", "🐺"
)

val FOOD_EMOJIS = listOf(
    "🍽️", "🍎", "🍏", "🍊", "🍋", "🍌",
    "🍉", "🍇", "🍓", "🫐", "🍒", "🥭",
    "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
    "🥦", "🥬", "🥒", "🌶️", "🌽", "🥕",
    "🥔", "🍠", "🥐", "🍞", "🥖", "🧀",
    "🥚", "🍳", "🥩", "🍗", "🍖", "🥓",
    "🌭", "🍔", "🍟", "🍕", "🥪", "🌮",
    "🌯", "🥙", "🍝", "🍜", "🍲", "🍛",
    "🍣", "🍚", "🍙", "🍤", "🥟", "🍦",
    "🍰", "🧁", "🍫", "🍬", "🍩", "🍪",
    "☕", "🍵", "🥤", "🧃", "🍺", "🍷",
    "🥛", "🍯", "🥜", "🌰", "🧂", "🍿"
)

// Scrollable emoji-grid picker. Used for the profile avatar and custom-food icons.
@Composable
fun EmojiPickerDialog(
    title: String,
    current: String,
    choices: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                choices.chunked(6).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { emoji ->
                            val selected = emoji == current
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onPick(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
