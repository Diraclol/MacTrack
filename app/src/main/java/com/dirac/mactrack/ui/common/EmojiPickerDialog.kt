package com.dirac.mactrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// Emoji sets shared by the avatar picker (profile) and the custom-food icon picker.
val AVATAR_EMOJIS = listOf(
    // People & faces
    "🧑", "👩", "👨", "🧔", "👵", "👴",
    "🧑‍🦰", "👩‍🦰", "👨‍🦰", "🧑‍🦱", "👩‍🦱", "👨‍🦱",
    "🧑‍🦳", "👩‍🦳", "👨‍🦳", "🧑‍🦲", "👱", "👱‍♀️",
    "🧑‍🎤", "🧑‍🍳", "🧑‍💻", "🧑‍🔬", "🧑‍🏫", "🧑‍🚀",
    "🧑‍⚕️", "🥷", "🦸", "🦹", "🧙", "🧑‍🌾",
    // Fitness & activity
    "💪", "🏃", "🏃‍♀️", "🏋️", "🏋️‍♀️", "🚴",
    "🚴‍♀️", "🧘", "🧘‍♀️", "🤸", "🤸‍♀️", "🏊",
    "🚵", "⛹️", "🤾", "🤺", "🏄", "🧗",
    "⛷️", "🏂", "🤼", "🤽", "🏌️", "🤹",
    // Symbols & vibes
    "🥗", "🍎", "🥦", "🥑", "🍳", "🔥",
    "⭐", "🌟", "✨", "🎯", "🏆", "🥇",
    "💯", "⚡", "🚀", "💥", "🎉", "👑",
    "💎", "🌈", "❤️", "🧡", "💛", "💚",
    "💙", "💜", "🖤", "🤍", "🎖️", "🌙",
    // Animals
    "🐻", "🐱", "🐶", "🦊", "🐼", "🦁",
    "🐯", "🐸", "🐵", "🐨", "🦄", "🐺",
    "🐮", "🐷", "🐰", "🐭", "🐹", "🐧",
    "🦉", "🦅", "🦖", "🐢", "🐙", "🦈",
    "🐳", "🦋", "🐝", "🦌", "🐴", "🐲",
    // More roles & characters
    "🧑‍⚖️", "🧑‍✈️", "🧑‍🚒", "🧑‍🏭", "🧑‍🔧", "🧑‍🎨",
    "🧑‍🎓", "🧑‍💼", "🕵️", "💂", "👷", "🤴",
    "👸", "🧚", "🧛", "🧜", "🧝", "🧞",
    "🧟", "🎅", "🤶", "🦾", "🧠", "👀",
    // More hobbies & gear
    "🏇", "🎳", "🏹", "🎣", "🥊", "🥋",
    "🛹", "⛸️", "🎸", "🎹", "🥁", "🎺",
    "🎧", "🎮", "🎲", "♟️", "🎨", "📚",
    // More animals & nature
    "🐔", "🐣", "🦆", "🦜", "🦚", "🕊️",
    "🐎", "🦒", "🐘", "🐬", "🐋", "🐌",
    "🐞", "🦗", "🐍", "🦎", "🌸", "🌻",
    "🌹", "🍀", "🌴", "🌵", "🌊", "☀️",
    "❄️", "⚽", "🏀", "🏈", "⚾", "🎾"
)

val FOOD_EMOJIS = listOf(
    // Plate / generic
    "🍽️", "🥣", "🥡", "🍱", "🧊", "🥢",
    // Fruit
    "🍎", "🍏", "🍐", "🍊", "🍋", "🍌",
    "🍉", "🍇", "🍓", "🫐", "🍒", "🍑",
    "🥭", "🍍", "🥥", "🥝", "🍈", "🫒",
    // Vegetables
    "🍅", "🍆", "🥑", "🥦", "🥬", "🥒",
    "🌶️", "🫑", "🌽", "🥕", "🧄", "🧅",
    "🥔", "🍠", "🍄", "🫘", "🌰", "🫛",
    // Grains, bread, dairy
    "🥐", "🍞", "🥖", "🥨", "🥯", "🧇",
    "🥞", "🧀", "🍚", "🍙", "🍘", "🫓",
    // Protein & mains
    "🥚", "🍳", "🥩", "🍗", "🍖", "🥓",
    "🌭", "🍔", "🍟", "🍕", "🥪", "🌮",
    "🌯", "🥙", "🧆", "🥘", "🍲", "🍛",
    "🍜", "🍝", "🥟", "🫕", "🥫", "🍢",
    // Seafood
    "🍣", "🍤", "🦐", "🦑", "🦞", "🦀",
    "🐟", "🐠",
    // Sweets
    "🍦", "🍨", "🍧", "🥧", "🍰", "🎂",
    "🧁", "🍮", "🍫", "🍬", "🍭", "🍩",
    "🍪", "🍯", "🍿",
    // Drinks
    "☕", "🍵", "🧋", "🥤", "🧃", "🧉",
    "🥛", "🍶", "🍺", "🍷", "🍸", "🍹",
    "🥂", "🥃", "🍾",
    // Seasoning & misc
    "🧂", "🧈", "🥜", "🫙",
    // More dishes & drinks
    "🫔", "🥮", "🍡", "🍥", "🥠", "🫖",
    "🫚", "🫗"
)

// Scrollable emoji-grid picker. A plain Dialog + LazyVerticalGrid (not AlertDialog's text slot,
// which does not scroll a large grid). Used for the profile avatar and custom-food icons.
@Composable
fun EmojiPickerDialog(
    title: String,
    current: String,
    choices: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(choices) { emoji ->
                        val selected = emoji == current
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
