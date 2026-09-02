package com.dirac.mactrack.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Copies a picked photo into app-internal storage as a small avatar file and returns its absolute path
// (null on failure). The picked content Uri is only a temporary grant, so the pixels must be copied out
// to survive restarts. Downscaled to ~256 px + JPEG, so the file stays a few KB. Each save uses a fresh
// timestamped filename -- so the stored path string changes (Compose re-decodes it) -- and older avatar
// files are deleted, so they can't pile up.
object AvatarStore {

    private const val PREFIX = "avatar_"

    suspend fun save(context: Context, uri: Uri, maxDim: Int = 256, quality: Int = 90): String? =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                // First pass: read bounds only, to pick an integer downsample factor.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val srcW = bounds.outWidth
                val srcH = bounds.outHeight
                if (srcW <= 0 || srcH <= 0) return@withContext null

                val longest = maxOf(srcW, srcH)
                var sample = 1
                while (longest / (sample * 2) >= maxDim) sample *= 2

                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?: return@withContext null
                val scaled = scaleToMax(decoded, maxDim)

                val file = File(context.filesDir, "$PREFIX${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                // Drop any previous avatar files so they don't accumulate.
                context.filesDir.listFiles { f -> f.name.startsWith(PREFIX) && f.name != file.name }
                    ?.forEach { it.delete() }
                file.absolutePath
            } catch (e: Throwable) {
                // Throwable (not Exception) so a decode OutOfMemoryError on a pathological image fails
                // safe to null rather than crashing.
                null
            }
        }

    private fun scaleToMax(src: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val ratio = maxDim.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
