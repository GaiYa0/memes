package com.emoji.overlay.performance

import com.emoji.overlay.data.entity.EmojiEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Performance optimization tests.
 *
 * Tests cover:
 * - Scroll performance simulation
 * - Cache hit rate optimization
 * - Memory management
 * - Search performance
 * - Batch operations
 */
class PerformanceOptimizationTest {

    // ==================== SCROLL PERFORMANCE ====================

    @Test
    fun `scroll simulation 1000 items`() {
        val items = (1L..1000L).map { createTestEmoji(it) }
        val visibleRange = 0..20 // Typical visible items

        val start = System.nanoTime()
        val visibleItems = items.slice(visibleRange)
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(21, visibleItems.size)
        assertTrue("Visible items load should be < 1ms", elapsed < 1.0)
        println("Scroll 1000 items (visible range): ${elapsed}ms")
    }

    @Test
    fun `scroll simulation 10000 items`() {
        val items = (1L..10000L).map { createTestEmoji(it) }
        val visibleRange = 100..120

        val start = System.nanoTime()
        val visibleItems = items.slice(visibleRange)
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(21, visibleItems.size)
        assertTrue("Visible items load should be < 1ms", elapsed < 1.0)
        println("Scroll 10000 items (visible range): ${elapsed}ms")
    }

    @Test
    fun `scroll simulation 50000 items`() {
        val items = (1L..50000L).map { createTestEmoji(it) }
        val visibleRange = 1000..1020

        val start = System.nanoTime()
        val visibleItems = items.slice(visibleRange)
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(21, visibleItems.size)
        assertTrue("Visible items load should be < 1ms", elapsed < 1.0)
        println("Scroll 50000 items (visible range): ${elapsed}ms")
    }

    @Test
    fun `paging simulation multiple pages`() {
        val allItems = (1L..50000L).map { createTestEmoji(it) }
        val pageSize = 50

        val start = System.nanoTime()
        val pages = (0..9).map { page ->
            allItems.drop(page * pageSize).take(pageSize)
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(10, pages.size)
        assertTrue(pages.all { it.size == pageSize })
        assertTrue("10 pages load should be < 10ms", elapsed < 10.0)
        println("Paging simulation (10 pages from 50000): ${elapsed}ms")
    }

    // ==================== CACHE PERFORMANCE ====================

    @Test
    fun `cache hit rate optimization`() {
        val cache = mutableMapOf<String, Int>()
        var hits = 0
        var misses = 0

        // Simulate cache access pattern
        // Access 100 unique items, then re-access them
        val accessPattern = (1..100).toList() + (1..100).toList() + (1..50).toList()

        val start = System.nanoTime()
        accessPattern.forEach { idx ->
            val key = "item_$idx"
            if (cache.containsKey(key)) {
                hits++
            } else {
                misses++
                cache[key] = idx
            }
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        val hitRate = hits.toFloat() / (hits + misses)
        println("Cache simulation: hitRate=${(hitRate * 100).toInt()}%, time=${elapsed}ms")
        assertTrue("Hit rate should be > 50%", hitRate > 0.5f)
    }

    @Test
    fun `LRU cache eviction`() {
        val maxSize = 100
        val cache = LinkedHashMap<Int, String>(maxSize, 0.75f, true)

        // Fill cache
        (1..150).forEach { i ->
            cache[i] = "value_$i"
            if (cache.size > maxSize) {
                cache.entries.firstOrNull()?.let { cache.remove(it.key) }
            }
        }

        assertEquals(maxSize, cache.size)
        // Oldest items should be evicted
        assertNull(cache[1])
        assertNull(cache[50])
        // Newest items should exist
        assertNotNull(cache[150])
        assertNotNull(cache[101])
    }

    // ==================== SEARCH PERFORMANCE ====================

    @Test
    fun `search 10000 items by name`() {
        val items = (1L..10000L).map { createTestEmoji(it, name = "emoji_${it}_face") }

        val start = System.nanoTime()
        val results = items.filter { it.name.contains("500") }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(results.isNotEmpty())
        assertTrue("Search 10000 should be < 30ms", elapsed < 30.0)
        println("Search 10000 by name: ${elapsed}ms, results=${results.size}")
    }

    @Test
    fun `search 50000 items by keywords`() {
        val items = (1L..50000L).map {
            createTestEmoji(it, keywords = "kw_${it % 100},tag_${it % 50}")
        }

        val start = System.nanoTime()
        val results = items.filter { it.keywords.contains("kw_42") }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(results.isNotEmpty())
        assertTrue("Search 50000 should be < 100ms", elapsed < 100.0)
        println("Search 50000 by keywords: ${elapsed}ms, results=${results.size}")
    }

    @Test
    fun `combined search name and keywords`() {
        val items = (1L..10000L).map {
            createTestEmoji(it, name = "emoji_$it", keywords = "kw_${it % 50}")
        }

        val query = "500"
        val start = System.nanoTime()
        val results = items.filter {
            it.name.contains(query) || it.keywords.contains(query)
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue("Combined search should be < 50ms", elapsed < 50.0)
        println("Combined search 10000: ${elapsed}ms, results=${results.size}")
    }

    // ==================== BATCH OPERATIONS ====================

    @Test
    fun `batch favorite toggle 10000 items`() {
        val items = (1L..10000L).map {
            createTestEmoji(it, isFavorite = it % 2 == 0L)
        }

        val start = System.nanoTime()
        val toggled = items.map { it.copy(isFavorite = !it.isFavorite) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(10000, toggled.size)
        assertTrue("Batch toggle should be < 20ms", elapsed < 20.0)
        println("Batch favorite toggle 10000: ${elapsed}ms")
    }

    @Test
    fun `batch usage update 10000 items`() {
        val items = (1L..10000L).map { createTestEmoji(it, usageCount = it) }

        val start = System.nanoTime()
        val updated = items.map { it.copy(usageCount = it.usageCount + 1) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(10000, updated.size)
        assertTrue(updated.all { it.usageCount == it.id + 1 })
        assertTrue("Batch update should be < 20ms", elapsed < 20.0)
        println("Batch usage update 10000: ${elapsed}ms")
    }

    @Test
    fun `batch sort 50000 items`() {
        val items = (1L..50000L).map {
            createTestEmoji(it, usageCount = 50001 - it)
        }

        val start = System.nanoTime()
        val sorted = items.sortedByDescending { it.usageCount }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(50000L, sorted.first().usageCount)
        assertTrue("Sort 50000 should be < 300ms", elapsed < 300.0)
        println("Sort 50000 items: ${elapsed}ms")
    }

    // ==================== MEMORY SIMULATION ====================

    @Test
    fun `memory efficient list processing`() {
        // Simulate processing without holding all in memory
        val batchSize = 100
        val totalItems = 50000
        var processed = 0

        val start = System.nanoTime()
        (0 until totalItems step batchSize).forEach { offset ->
            val batch = (offset until minOf(offset + batchSize, totalItems)).map {
                createTestEmoji(it.toLong())
            }
            processed += batch.size
            // Process batch and release
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(totalItems, processed)
        println("Memory efficient processing 50000 (batch=$batchSize): ${elapsed}ms")
    }

    @Test
    fun `chunked search for large datasets`() {
        val items = (1L..50000L).map { createTestEmoji(it, name = "emoji_$it") }
        val chunkSize = 5000
        val query = "500"

        val start = System.nanoTime()
        val results = items.chunked(chunkSize).flatMap { chunk ->
            chunk.filter { it.name.contains(query) }
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(results.isNotEmpty())
        println("Chunked search 50000 (chunk=$chunkSize): ${elapsed}ms, results=${results.size}")
    }

    // ==================== OVERLAY ANIMATION SIMULATION ====================

    @Test
    fun `overlay state transition performance`() {
        var isVisible = false
        var selectedEmoji: EmojiEntity? = null

        val start = System.nanoTime()
        repeat(10000) { i ->
            isVisible = !isVisible
            if (isVisible) {
                selectedEmoji = createTestEmoji(i.toLong())
            } else {
                selectedEmoji = null
            }
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue("10000 transitions should be < 10ms", elapsed < 10.0)
        println("Overlay state transitions (10000): ${elapsed}ms")
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        id: Long,
        name: String = "emoji_$id",
        keywords: String = "",
        usageCount: Long = 0,
        isFavorite: Boolean = false
    ) = EmojiEntity(
        id = id,
        name = name,
        keywords = keywords,
        filePath = "/images/$name.png",
        mimeType = "image/png",
        contentHash = "hash_$id",
        usageCount = usageCount,
        isFavorite = isFavorite
    )
}
