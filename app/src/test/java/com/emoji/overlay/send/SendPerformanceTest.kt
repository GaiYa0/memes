package com.emoji.overlay.send

import com.emoji.overlay.data.entity.EmojiEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Performance tests for send operations.
 */
class SendPerformanceTest {

    @Test
    fun `send operation latency simulation`() {
        val emoji = createTestEmoji(1, "smile")

        val start = System.nanoTime()
        // Simulate send preparation
        val file = emoji.filePath
        val mimeType = emoji.mimeType
        val name = emoji.name
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertNotNull(file)
        assertNotNull(mimeType)
        assertNotNull(name)
        assertTrue("Send preparation should be < 1ms", elapsed < 1.0)
        println("Send preparation: ${elapsed}ms")
    }

    @Test
    fun `batch send preparation 100 items`() {
        val emojis = (1L..100L).map { createTestEmoji(it, "emoji_$it") }

        val start = System.nanoTime()
        val prepared = emojis.map { emoji ->
            Triple(emoji.filePath, emoji.mimeType, emoji.name)
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(100, prepared.size)
        assertTrue("Batch preparation should be < 5ms", elapsed < 5.0)
        println("Batch send preparation (100): ${elapsed}ms")
    }

    @Test
    fun `batch send preparation 1000 items`() {
        val emojis = (1L..1000L).map { createTestEmoji(it, "emoji_$it") }

        val start = System.nanoTime()
        val prepared = emojis.map { emoji ->
            Triple(emoji.filePath, emoji.mimeType, emoji.name)
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, prepared.size)
        assertTrue("Batch preparation should be < 20ms", elapsed < 20.0)
        println("Batch send preparation (1000): ${elapsed}ms")
    }

    @Test
    fun `recent usage update performance`() {
        val emojis = (1L..1000L).map {
            createTestEmoji(it, "emoji_$it", usageCount = it)
        }

        val start = System.nanoTime()
        val updated = emojis.map { it.copy(usageCount = it.usageCount + 1) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, updated.size)
        assertTrue(updated.all { it.usageCount == it.id + 1 })
        assertTrue("Usage update should be < 10ms", elapsed < 10.0)
        println("Recent usage update (1000): ${elapsed}ms")
    }

    @Test
    fun `favorite toggle performance`() {
        val emojis = (1L..1000L).map {
            createTestEmoji(it, "emoji_$it", isFavorite = it % 2 == 0L)
        }

        val start = System.nanoTime()
        val toggled = emojis.map { it.copy(isFavorite = !it.isFavorite) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, toggled.size)
        assertTrue("Favorite toggle should be < 10ms", elapsed < 10.0)
        println("Favorite toggle (1000): ${elapsed}ms")
    }

    @Test
    fun `overlay state transitions`() {
        var isVisible = false
        var selected: EmojiEntity? = null

        val start = System.nanoTime()
        repeat(1000) {
            isVisible = !isVisible
            if (isVisible) {
                selected = createTestEmoji(1, "test")
            } else {
                selected = null
            }
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertTrue("State transitions should be < 5ms", elapsed < 5.0)
        println("1000 state transitions: ${elapsed}ms")
    }

    @Test
    fun `send result creation performance`() {
        val emojis = (1L..1000L).map { createTestEmoji(it, "emoji_$it") }

        val start = System.nanoTime()
        val results = emojis.map { SendResult.Success(it) }
        val elapsed = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(1000, results.size)
        assertTrue("Result creation should be < 10ms", elapsed < 10.0)
        println("Send result creation (1000): ${elapsed}ms")
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        id: Long,
        name: String,
        usageCount: Long = 0,
        isFavorite: Boolean = false
    ) = EmojiEntity(
        id = id,
        name = name,
        filePath = "/test/$name.png",
        mimeType = "image/png",
        contentHash = "hash_$id",
        usageCount = usageCount,
        isFavorite = isFavorite
    )
}

private sealed class SendResult {
    data class Success(val emoji: EmojiEntity) : SendResult()
    data class Shared(val emoji: EmojiEntity) : SendResult()
    data class Failure(val emoji: EmojiEntity, val error: String) : SendResult()
}
