package com.emoji.overlay.browser.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * LRU memory cache for thumbnails.
 *
 * Keeps decoded Bitmaps in memory for fast grid scrolling.
 * Falls back to disk cache for cache misses.
 */
class ThumbnailCache(private val context: Context) {
    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
        private const val THUMB_MAX_SIZE = 128
    }

    private val memoryCache = object : LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    /**
     * Get a thumbnail from cache or load from disk.
     */
    suspend fun getThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        // Check memory cache first
        memoryCache.get(path)?.let { return@withContext it }

        // Load from disk
        val file = File(path)
        if (!file.exists()) return@withContext null

        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(file, THUMB_MAX_SIZE)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            bitmap?.let {
                memoryCache.put(path, it)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Put a thumbnail into memory cache.
     */
    fun put(path: String, bitmap: Bitmap) {
        memoryCache.put(path, bitmap)
    }

    /**
     * Remove a thumbnail from cache.
     */
    fun remove(path: String) {
        memoryCache.remove(path)
    }

    /**
     * Clear all cached thumbnails.
     */
    fun clear() {
        memoryCache.evictAll()
    }

    /**
     * Get cache statistics.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            hitCount = memoryCache.hitCount().toLong(),
            missCount = memoryCache.missCount().toLong(),
            size = memoryCache.size(),
            maxSize = memoryCache.maxSize()
        )
    }

    private fun calculateSampleSize(file: File, maxSize: Int): Int {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}

data class CacheStats(
    val hitCount: Long,
    val missCount: Long,
    val size: Int,
    val maxSize: Int
) {
    val hitRate: Float get() = if (hitCount + missCount > 0) hitCount.toFloat() / (hitCount + missCount) else 0f
}
