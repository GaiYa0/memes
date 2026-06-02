package com.emoji.overlay.performance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max

/**
 * Shared downsampled file decode for grid thumbnails (same visual intent, less IO/RAM).
 */
fun decodeSampledBitmap(path: String, targetMaxPx: Int, config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, boundsOptions)
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null

    var sampleSize = 1
    var halfWidth = width / 2
    var halfHeight = height / 2
    while (halfWidth / sampleSize >= targetMaxPx && halfHeight / sampleSize >= targetMaxPx) {
        sampleSize *= 2
    }
    sampleSize = max(1, sampleSize)

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = config
    }
    return BitmapFactory.decodeFile(path, decodeOptions)
}
