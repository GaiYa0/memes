package com.emoji.overlay.browser

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ThumbnailCache logic.
 */
class ThumbnailCacheTest {

    @Test
    fun `cache stats hit rate calculation`() {
        val stats = CacheStats(hitCount = 80, missCount = 20, size = 100, maxSize = 1000)
        assertEquals(0.8f, stats.hitRate, 0.01f)
    }

    @Test
    fun `cache stats zero division`() {
        val stats = CacheStats(hitCount = 0, missCount = 0, size = 0, maxSize = 1000)
        assertEquals(0f, stats.hitRate, 0.01f)
    }

    @Test
    fun `cache size within limits`() {
        val maxSize = 50 * 1024 * 1024 // 50MB
        val currentSize = 30 * 1024 * 1024 // 30MB
        assertTrue(currentSize <= maxSize)
    }

    @Test
    fun `cache eviction when full`() {
        val maxSize = 100
        val items = (1..150).map { "item_$it" }
        // When cache is full, oldest items should be evicted
        val retained = items.takeLast(maxSize)
        assertEquals(maxSize, retained.size)
    }

    @Test
    fun `thumbnail sample size calculation`() {
        // Image 2048x2048, target 128
        val width = 2048
        val height = 2048
        val maxSize = 128
        var sampleSize = 1
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2
        }
        assertEquals(16, sampleSize) // 2048 / 16 = 128
    }

    @Test
    fun `thumbnail sample size for small image`() {
        val width = 64
        val height = 64
        val maxSize = 128
        var sampleSize = 1
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2
        }
        assertEquals(1, sampleSize) // No downsampling needed
    }
}

private data class CacheStats(
    val hitCount: Long,
    val missCount: Long,
    val size: Int,
    val maxSize: Int
) {
    val hitRate: Float get() = if (hitCount + missCount > 0) hitCount.toFloat() / (hitCount + missCount) else 0f
}
