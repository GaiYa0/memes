package com.emoji.overlay.browser

import com.emoji.overlay.data.entity.EmojiEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Performance tests for browser operations.
 *
 * Measures:
 * - List filtering performance
 * - Search performance
 * - Sort performance
 * - Paging performance
 */
class BrowserPerformanceTest {

    @Test
    fun `filter 1000 emojis by category`() {
        val emojis = (1L..1000L).map { createTestEmoji(it, categoryId = it % 10) }

        val start = System.nanoTime()
        val filtered = emojis.filter { it.categoryId == 5L }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(100, filtered.size)
        assertTrue("Filter 1000 should be < 5ms", elapsed < 5.0)
        println("Filter 1000 emojis by category: ${elapsed}ms")
    }

    @Test
    fun `filter 10000 emojis by category`() {
        val emojis = (1L..10000L).map { createTestEmoji(it, categoryId = it % 10) }

        val start = System.nanoTime()
        val filtered = emojis.filter { it.categoryId == 5L }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, filtered.size)
        assertTrue("Filter 10000 should be < 20ms", elapsed < 20.0)
        println("Filter 10000 emojis by category: ${elapsed}ms")
    }

    @Test
    fun `filter 50000 emojis by category`() {
        val emojis = (1L..50000L).map { createTestEmoji(it, categoryId = it % 10) }

        val start = System.nanoTime()
        val filtered = emojis.filter { it.categoryId == 5L }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(5000, filtered.size)
        assertTrue("Filter 50000 should be < 50ms", elapsed < 50.0)
        println("Filter 50000 emojis by category: ${elapsed}ms")
    }

    @Test
    fun `search 10000 emojis by name`() {
        val emojis = (1L..10000L).map { createTestEmoji(it, name = "emoji_${it}_face") }

        val start = System.nanoTime()
        val results = emojis.filter { it.name.contains("500") }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(results.isNotEmpty())
        assertTrue("Search 10000 should be < 20ms", elapsed < 20.0)
        println("Search 10000 emojis by name: ${elapsed}ms, results: ${results.size}")
    }

    @Test
    fun `search 50000 emojis by keywords`() {
        val emojis = (1L..50000L).map {
            createTestEmoji(it, keywords = "kw_${it % 100},tag_${it % 50}")
        }

        val start = System.nanoTime()
        val results = emojis.filter { it.keywords.contains("kw_42") }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(results.isNotEmpty())
        assertTrue("Search 50000 should be < 100ms", elapsed < 100.0)
        println("Search 50000 emojis by keywords: ${elapsed}ms, results: ${results.size}")
    }

    @Test
    fun `sort 10000 emojis by usage`() {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, usageCount = 10001 - it)
        }

        val start = System.nanoTime()
        val sorted = emojis.sortedByDescending { it.usageCount }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(10000L, sorted.first().usageCount)
        assertTrue("Sort 10000 should be < 50ms", elapsed < 50.0)
        println("Sort 10000 emojis by usage: ${elapsed}ms")
    }

    @Test
    fun `sort 50000 emojis by usage`() {
        val emojis = (1L..50000L).map {
            createTestEmoji(it, usageCount = 50001 - it)
        }

        val start = System.nanoTime()
        val sorted = emojis.sortedByDescending { it.usageCount }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(50000L, sorted.first().usageCount)
        assertTrue("Sort 50000 should be < 200ms", elapsed < 200.0)
        println("Sort 50000 emojis by usage: ${elapsed}ms")
    }

    @Test
    fun `paging simulation 10000 items`() {
        val allItems = (1L..10000L).map { createTestEmoji(it) }
        val pageSize = 50

        val start = System.nanoTime()
        val page1 = allItems.drop(0).take(pageSize)
        val page2 = allItems.drop(50).take(pageSize)
        val page100 = allItems.drop(4950).take(pageSize)
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(50, page1.size)
        assertEquals(50, page2.size)
        assertEquals(50, page100.size)
        assertTrue("Paging should be < 5ms", elapsed < 5.0)
        println("Paging simulation (3 pages from 10000): ${elapsed}ms")
    }

    @Test
    fun `favorites filtering 10000`() {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, isFavorite = it % 10 == 0L)
        }

        val start = System.nanoTime()
        val favorites = emojis.filter { it.isFavorite }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, favorites.size)
        assertTrue("Favorites filter should be < 20ms", elapsed < 20.0)
        println("Favorites filtering 10000: ${elapsed}ms")
    }

    @Test
    fun `recent emojis top N`() {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, usageCount = it)
        }

        val start = System.nanoTime()
        val recent = emojis.sortedByDescending { it.usageCount }.take(50)
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(50, recent.size)
        assertEquals(10000L, recent.first().usageCount)
        assertTrue("Recent top 50 should be < 50ms", elapsed < 50.0)
        println("Recent emojis top 50 from 10000: ${elapsed}ms")
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        id: Long,
        name: String = "emoji_$id",
        keywords: String = "",
        categoryId: Long? = id % 10,
        usageCount: Long = 0,
        isFavorite: Boolean = false
    ) = EmojiEntity(
        id = id,
        name = name,
        keywords = keywords,
        categoryId = categoryId,
        filePath = "/images/$name.png",
        mimeType = "image/png",
        contentHash = "hash_$id",
        usageCount = usageCount,
        isFavorite = isFavorite
    )
}
