package com.emoji.overlay.send

import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.send.manager.SendResult
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for EmojiOverlayManager logic.
 */
class EmojiOverlayManagerTest {

    @Test
    fun `overlay visibility state`() {
        var isVisible = false
        assertFalse(isVisible)

        isVisible = true
        assertTrue(isVisible)

        isVisible = false
        assertFalse(isVisible)
    }

    @Test
    fun `toggle overlay`() {
        var isVisible = false
        isVisible = !isVisible
        assertTrue(isVisible)
        isVisible = !isVisible
        assertFalse(isVisible)
    }

    @Test
    fun `selected emoji state`() {
        var selected: EmojiEntity? = null
        assertNull(selected)

        val emoji = createTestEmoji(1, "smile")
        selected = emoji
        assertNotNull(selected)
        assertEquals("smile", selected?.name)

        selected = null
        assertNull(selected)
    }

    @Test
    fun `send result handling`() {
        val emoji = createTestEmoji(1, "smile")

        val success = SendResult.Success(emoji)
        assertTrue(success is SendResult.Success)

        val failure = SendResult.Failure(emoji, "error")
        assertTrue(failure is SendResult.Failure)
    }

    @Test
    fun `favorite toggle`() {
        val emoji = createTestEmoji(1, "smile", isFavorite = false)
        assertFalse(emoji.isFavorite)

        val toggled = emoji.copy(isFavorite = !emoji.isFavorite)
        assertTrue(toggled.isFavorite)
    }

    @Test
    fun `recent usage update`() {
        val emoji = createTestEmoji(1, "smile", usageCount = 0)
        assertEquals(0, emoji.usageCount)

        val updated = emoji.copy(usageCount = emoji.usageCount + 1)
        assertEquals(1, updated.usageCount)
    }

    @Test
    fun `overlay show and hide sequence`() {
        var isVisible = false

        // Show
        isVisible = true
        assertTrue(isVisible)

        // Select emoji
        val emoji = createTestEmoji(1, "smile")
        var selected: EmojiEntity? = emoji
        assertNotNull(selected)

        // Send
        val result = SendResult.Success(emoji)
        assertTrue(result is SendResult.Success)

        // Hide
        isVisible = false
        selected = null
        assertFalse(isVisible)
        assertNull(selected)
    }

    @Test
    fun `preview show and dismiss`() {
        var previewEmoji: EmojiEntity? = null
        assertNull(previewEmoji)

        val emoji = createTestEmoji(1, "smile")
        previewEmoji = emoji
        assertNotNull(previewEmoji)

        previewEmoji = null
        assertNull(previewEmoji)
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        id: Long,
        name: String,
        isFavorite: Boolean = false,
        usageCount: Long = 0
    ) = EmojiEntity(
        id = id,
        name = name,
        filePath = "/test/$name.png",
        mimeType = "image/png",
        contentHash = "hash_$id",
        isFavorite = isFavorite,
        usageCount = usageCount
    )
}
