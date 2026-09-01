package com.dirac.mactrack.data.ai

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

// Turns a picked image into a compact data URL to send to the vision model. Downscales the longest
// edge to ~maxDim px and JPEG-compresses it so the request stays small (a few hundred KB), which keeps
// upload/latency/quota reasonable. Off the main thread; returns null on any failure.
object ImageEncoder {

    suspend fun toDataUrl(
        resolver: ContentResolver,
        uri: Uri,
        maxDim: Int = 1024,
        quality: Int = 85
    ): String? = withContext(Dispatchers.IO) {
        try {
            // First pass: read bounds only, to pick an integer downsample factor.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return@withContext null

            var sample = 1
            while (srcW / (sample * 2) >= maxDim && srcH / (sample * 2) >= maxDim) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@withContext null

            val scaled = scaleToMax(decoded, maxDim)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleToMax(src: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val ratio = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(src, (src.width * ratio).toInt().coerceAtLeast(1), (src.height * ratio).toInt().coerceAtLeast(1), true)
    }
}
