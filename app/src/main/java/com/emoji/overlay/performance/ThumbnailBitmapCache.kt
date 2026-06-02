package com.emoji.overlay.performance

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * In-memory LRU for grid thumbnails. Reduces duplicate decode IO when scrolling.
 * Cleared on memory pressure via [clear].
 */
object ThumbnailBitmapCache {
    private const val MAX_ENTRIES = 64
    private val cache = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean {
            return size > MAX_ENTRIES
        }
    }

    fun loadGridThumbnail(cacheKey: String, absolutePath: String, targetMaxPx: Int = 240): ImageBitmap? {
        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }
        val bitmap = decodeSampledBitmap(absolutePath, targetMaxPx) ?: return null
        val imageBitmap = bitmap.asImageBitmap()
        synchronized(cache) {
            cache[cacheKey] = imageBitmap
        }
        return imageBitmap
    }

    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }
}
