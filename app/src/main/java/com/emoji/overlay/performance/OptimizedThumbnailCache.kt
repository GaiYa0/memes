package com.emoji.overlay.performance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Optimized thumbnail cache with:
 * - LRU memory cache
 * - Disk cache awareness
 * - Concurrent access safety
 * - Memory pressure handling
 */
class OptimizedThumbnailCache(private val context: Context) {
    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 40 * 1024 * 1024 // 40MB
        private const val THUMB_MAX_SIZE = 96 // Reduced from 128 for better memory
        private val BITMAP_CONFIG = Bitmap.Config.RGB_565 // Half memory vs ARGB_8888
    }

    private val memoryCache = object : LruCache<String, Bitmap>(MAX_MEMORY_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            // Don't recycle here - let GC handle it to avoid use-after-free
        }
    }

    private val loadingMutex = ConcurrentHashMap<String, Mutex>()
    private val cacheHits = java.util.concurrent.atomic.AtomicLong(0)
    private val cacheMisses = java.util.concurrent.atomic.AtomicLong(0)

    /**
     * Get thumbnail with optimized loading.
     */
    suspend fun getThumbnail(path: String): Bitmap? = withContext(Dispatchers.IO) {
        // Check memory cache first
        memoryCache.get(path)?.let {
            cacheHits.incrementAndGet()
            return@withContext it
        }
        cacheMisses.incrementAndGet()

        // Ensure only one loader per path
        val mutex = loadingMutex.getOrPut(path) { Mutex() }
        mutex.withLock {
            // Double-check after acquiring lock
            memoryCache.get(path)?.let { return@withContext it }

            // Load from disk
            val file = File(path)
            if (!file.exists()) return@withContext null

            try {
                val bitmap = decodeThumbnail(file)
                bitmap?.let { memoryCache.put(path, it) }
                bitmap
            } catch (e: Exception) {
                null
            } finally {
                loadingMutex.remove(path)
            }
        }
    }

    private fun decodeThumbnail(file: File): Bitmap? {
        // First pass: get dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        // Calculate sample size
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, THUMB_MAX_SIZE)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = BITMAP_CONFIG
        options.inMutable = false

        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * Preload thumbnails for visible items.
     */
    suspend fun preload(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { path ->
            if (memoryCache.get(path) == null) {
                getThumbnail(path)
            }
        }
    }

    /**
     * Clear cache on memory pressure.
     */
    fun onMemoryPressure() {
        memoryCache.trimToSize(MAX_MEMORY_CACHE_SIZE / 4)
    }

    fun clear() {
        memoryCache.evictAll()
        loadingMutex.clear()
    }

    fun getStats(): CacheStats {
        val total = cacheHits.get() + cacheMisses.get()
        return CacheStats(
            hitCount = cacheHits.get(),
            missCount = cacheMisses.get(),
            hitRate = if (total > 0) cacheHits.get().toFloat() / total else 0f,
            size = memoryCache.size(),
            maxSize = memoryCache.maxSize()
        )
    }
}

data class CacheStats(
    val hitCount: Long,
    val missCount: Long,
    val hitRate: Float,
    val size: Int,
    val maxSize: Int
)
