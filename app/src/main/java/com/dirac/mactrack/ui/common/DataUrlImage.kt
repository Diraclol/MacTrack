package com.dirac.mactrack.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

// Renders a base64 "data:image/...;base64,..." URL without any image-loading library: decode the
// base64 to a Bitmap once and draw it. Used for the attached-photo thumbnail and in chat bubbles.
@Composable
fun DataUrlImage(dataUrl: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap: ImageBitmap? = remember(dataUrl) { decode(dataUrl) }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "Attached image", modifier = modifier, contentScale = contentScale)
    }
}

private fun decode(dataUrl: String): ImageBitmap? {
    val comma = dataUrl.indexOf(',')
    val base64 = if (comma >= 0) dataUrl.substring(comma + 1) else dataUrl
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
