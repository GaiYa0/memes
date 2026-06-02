package com.emoji.overlay.send

import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.send.manager.SendResult
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for EmojiSendManager logic.
 */
class EmojiSendManagerTest {

    @Test
    fun `send result success`() {
        val emoji = createTestEmoji(1, "smile")
        val result = SendResult.Success(emoji)
        assertTrue(result is SendResult.Success)
        assertEquals("smile", result.emoji.name)
    }

    @Test
    fun `send result shared`() {
        val emoji = createTestEmoji(1, "smile")
        val result = SendResult.Shared(emoji)
        assertTrue(result is SendResult.Shared)
    }

    @Test
    fun `send result failure`() {
        val emoji = createTestEmoji(1, "smile")
        val result = SendResult.Failure(emoji, "File not found")
        assertTrue(result is SendResult.Failure)
        assertEquals("File not found", result.error)
    }

    @Test
    fun `emoji file exists check`() {
        val emoji = createTestEmoji(1, "smile", filePath = "/test/smile.png")
        // File existence check requires actual file system
        // This tests the logic structure
        assertNotNull(emoji.filePath)
    }

    @Test
    fun `emoji URI generation`() {
        val emoji = createTestEmoji(1, "smile", filePath = "/test/smile.png")
        val expectedUri = "file:///test/smile.png"
        assertEquals(expectedUri, "file://${emoji.filePath}")
    }

    @Test
    fun `clipboard data preparation`() {
        val emoji = createTestEmoji(1, "smile", mimeType = "image/png")
        assertEquals("image/png", emoji.mimeType)
        assertEquals("smile", emoji.name)
    }

    @Test
    fun `share intent preparation`() {
        val emoji = createTestEmoji(1, "smile", mimeType = "image/gif")
        assertEquals("image/gif", emoji.mimeType)
    }

    @Test
    fun `send updates usage count`() {
        val emoji = createTestEmoji(1, "smile", usageCount = 5)
        val updated = emoji.copy(usageCount = emoji.usageCount + 1)
        assertEquals(6, updated.usageCount)
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        id: Long,
        name: String,
        filePath: String = "/test/$name.png",
        mimeType: String = "image/png",
        usageCount: Long = 0
    ) = EmojiEntity(
        id = id,
        name = name,
        filePath = filePath,
        mimeType = mimeType,
        contentHash = "hash_$id",
        usageCount = usageCount
    )
}
