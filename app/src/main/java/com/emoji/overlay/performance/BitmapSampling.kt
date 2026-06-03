package com.emoji.overlay.performance

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlin.math.max

/**
 * Shared downsampled file decode for grid thumbnails (same visual intent, less IO/RAM).
 */
fun calculateInSampleSize(width: Int, height: Int, targetMaxPx: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / sampleSize >= targetMaxPx && halfHeight / sampleSize >= targetMaxPx) {
        sampleSize *= 2
    }
    return max(1, sampleSize)
}

fun decodeSampledBitmap(path: String, targetMaxPx: Int, config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, boundsOptions)
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null

    val sampleSize = calculateInSampleSize(width, height, targetMaxPx)

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = config
    }
    return BitmapFactory.decodeFile(path, decodeOptions)
}

fun loadMediaThumbnail(
    contentResolver: ContentResolver,
    uri: Uri,
    targetMaxPx: Int = 320
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            contentResolver.loadThumbnail(uri, Size(targetMaxPx, targetMaxPx), null)
        }.getOrNull()?.let { return it }
    }
    return decodeSampledBitmapFromUri(contentResolver, uri, targetMaxPx)
}

fun decodeSampledBitmapFromUri(
    contentResolver: ContentResolver,
    uri: Uri,
    targetMaxPx: Int,
    config: Bitmap.Config = Bitmap.Config.RGB_565
): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
    }
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null

    val sampleSize = calculateInSampleSize(width, height, targetMaxPx)

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = config
    }
    return contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
    }
}
