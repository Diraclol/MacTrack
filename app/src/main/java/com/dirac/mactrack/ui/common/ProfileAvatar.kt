package com.dirac.mactrack.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

// The profile avatar, shown in the dashboard header, the More header, and the Profile screen. Renders
// the chosen photo (a file saved by AvatarStore) when one is set, otherwise the chosen emoji, inside a
// circular primary-container chip. The caller passes the size, the emoji text style, and any click
// modifier. If the photo file is missing or unreadable it falls back to the emoji.
@Composable
fun ProfileAvatar(
    emoji: String,
    photoPath: String?,
    size: Dp,
    emojiStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val image: ImageBitmap? = remember(photoPath) { photoPath?.let { decodeFile(it) } }
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(emoji, style = emojiStyle)
        }
    }
}

private fun decodeFile(path: String): ImageBitmap? = try {
    BitmapFactory.decodeFile(path)?.asImageBitmap()
} catch (e: Throwable) {
    null
}
