package com.emoji.overlay.data

import com.emoji.overlay.data.entity.EmojiEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Performance simulation tests for database operations.
 *
 * These tests measure operation timing without requiring Android/Room runtime.
 * Actual Room performance tests require Android instrumentation.
 *
 * Expected performance benchmarks (on modern Android device):
 * - Insert 1 record: < 5ms
 * - Insert 100 records (batch): < 50ms
 * - Insert 1000 records (batch): < 200ms
 * - Insert 10000 records (batch): < 1000ms
 * - Query by index: < 5ms
 * - Search with LIKE: < 20ms for 10k records
 */
class PerformanceTest {

    @Test
    fun `create 1 emoji - timing`() {
        val start = System.nanoTime()
        val emoji = createTestEmoji(1)
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertNotNull(emoji)
        assertTrue("Single entity creation should be < 1ms", elapsed < 1.0)
        println("Create 1 emoji: ${elapsed}ms")
    }

    @Test
    fun `create 100 emojis - timing`() {
        val start = System.nanoTime()
        val emojis = (1L..100L).map { createTestEmoji(it) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(100, emojis.size)
        assertTrue("100 entities creation should be < 10ms", elapsed < 10.0)
        println("Create 100 emojis: ${elapsed}ms")
    }

    @Test
    fun `create 1000 emojis - timing`() {
        val start = System.nanoTime()
        val emojis = (1L..1000L).map { createTestEmoji(it) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, emojis.size)
        assertTrue("1000 entities creation should be < 50ms", elapsed < 50.0)
        println("Create 1000 emojis: ${elapsed}ms")
    }

    @Test
    fun `create 10000 emojis - timing`() {
        val start = System.nanoTime()
        val emojis = (1L..10000L).map { createTestEmoji(it) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(10000, emojis.size)
        assertTrue("10000 entities creation should be < 200ms", elapsed < 200.0)
        println("Create 10000 emojis: ${elapsed}ms")
    }

    @Test
    fun `filter 10000 emojis by category - timing`() {
        val emojis = (1L..10000L).map { createTestEmoji(it, categoryId = it % 10) }

        val start = System.nanoTime()
        val filtered = emojis.filter { it.categoryId == 5L }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, filtered.size)
        assertTrue("Filter 10k records should be < 10ms", elapsed < 10.0)
        println("Filter 10000 emojis by category: ${elapsed}ms")
    }

    @Test
    fun `filter 10000 emojis by favorite - timing`() {
        val emojis = (1L..10000L).map { createTestEmoji(it, isFavorite = it % 5 == 0L) }

        val start = System.nanoTime()
        val favorites = emojis.filter { it.isFavorite }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(2000, favorites.size)
        assertTrue("Filter favorites from 10k should be < 10ms", elapsed < 10.0)
        println("Filter 10000 emojis by favorite: ${elapsed}ms")
    }

    @Test
    fun `search 10000 emojis by name - timing`() {
        val emojis = (1L..10000L).map { createTestEmoji(it, name = "emoji_${it}_face") }

        val start = System.nanoTime()
        val results = emojis.filter { it.name.contains("500") }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(results.isNotEmpty())
        assertTrue("Search 10k records should be < 20ms", elapsed < 20.0)
        println("Search 10000 emojis by name: ${elapsed}ms, found: ${results.size}")
    }

    @Test
    fun `search 10000 emojis by keywords - timing`() {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, keywords = "tag${it % 100},keyword${it % 50}")
        }

        val start = System.nanoTime()
        val results = emojis.filter { it.keywords.contains("tag42") }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(results.isNotEmpty())
        assertTrue("Keyword search on 10k should be < 20ms", elapsed < 20.0)
        println("Search 10000 emojis by keywords: ${elapsed}ms, found: ${results.size}")
    }

    @Test
    fun `sort 10000 emojis by usage count - timing`() {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, usageCount = (10001 - it))
        }

        val start = System.nanoTime()
        val sorted = emojis.sortedByDescending { it.usageCount }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(10000L, sorted.first().usageCount)
        assertEquals(1L, sorted.last().usageCount)
        assertTrue("Sort 10k records should be < 50ms", elapsed < 50.0)
        println("Sort 10000 emojis by usage: ${elapsed}ms")
    }

    @Test
    fun `batch operations on 10000 emojis - timing`() {
        val emojis = (1L..10000L).map { createTestEmoji(it) }.toMutableList()

        // Batch soft delete (1001 items: 1000..2000 inclusive)
        val start1 = System.nanoTime()
        val idsToDelete = (1000L..2000L).toSet()
        val updated = emojis.map {
            if (it.id in idsToDelete) it.copy(isDeleted = true) else it
        }
        val elapsed1 = (System.nanoTime() - start1) / 1_000_000.0

        val active = updated.filter { !it.isDeleted }
        assertEquals(8999, active.size) // 10000 - 1001
        println("Batch soft-delete 1001 from 10000: ${elapsed1}ms")

        // Batch favorite (1001 items: 5000..6000 inclusive)
        val start2 = System.nanoTime()
        val favorited = updated.map {
            if (it.id in (5000L..6000L)) it.copy(isFavorite = true) else it
        }
        val elapsed2 = (System.nanoTime() - start2) / 1_000_000.0

        val favorites = favorited.filter { it.isFavorite }
        assertEquals(1001, favorites.size)
        println("Batch favorite 1001 from 10000: ${elapsed2}ms")
    }

    @Test
    fun `hash deduplication check on 10000 - timing`() {
        val hashes = (1L..10000L).map { "hash_${it % 5000}" }.toSet()
        val emojis = (1L..10000L).map { createTestEmoji(it, contentHash = "hash_${it % 5000}") }

        val start = System.nanoTime()
        val seen = mutableSetOf<String>()
        var duplicates = 0
        emojis.forEach {
            if (!seen.add(it.contentHash)) duplicates++
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(5000, duplicates) // Half are duplicates
        assertTrue("Dedup check on 10k should be < 20ms", elapsed < 20.0)
        println("Dedup check on 10000 emojis: ${elapsed}ms, duplicates: $duplicates")
    }

    // ==================== HELPER ====================

    private fun createTestEmoji(
        id: Long,
        name: String = "emoji_$id",
        keywords: String = "kw_$id",
        categoryId: Long? = id % 10,
        usageCount: Long = 0,
        isFavorite: Boolean = false,
        isDeleted: Boolean = false,
        contentHash: String = "hash_$id"
    ) = EmojiEntity(
        id = id,
        name = name,
        keywords = keywords,
        categoryId = categoryId,
        filePath = "/images/$name.png",
        mimeType = "image/png",
        contentHash = contentHash,
        usageCount = usageCount,
        isFavorite = isFavorite,
        isDeleted = isDeleted
    )
}
