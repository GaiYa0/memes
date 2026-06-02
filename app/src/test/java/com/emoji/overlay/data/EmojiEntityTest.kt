package com.emoji.overlay.data

import com.emoji.overlay.data.entity.EmojiEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for EmojiEntity data class.
 */
class EmojiEntityTest {

    @Test
    fun `create emoji with default values`() {
        val emoji = EmojiEntity(
            name = "test",
            filePath = "/test/path.png",
            mimeType = "image/png",
            contentHash = "abc123"
        )

        assertEquals("test", emoji.name)
        assertEquals("/test/path.png", emoji.filePath)
        assertEquals("image/png", emoji.mimeType)
        assertEquals("abc123", emoji.contentHash)
        assertEquals(0, emoji.usageCount)
        assertFalse(emoji.isFavorite)
        assertFalse(emoji.isDeleted)
        assertFalse(emoji.isDirty)
        assertNull(emoji.categoryId)
        assertNull(emoji.thumbPath)
        assertNull(emoji.syncId)
        assertNull(emoji.aiCategory)
    }

    @Test
    fun `create emoji with all fields`() {
        val now = System.currentTimeMillis()
        val emoji = EmojiEntity(
            id = 42,
            name = "smile",
            keywords = "happy,face,smile",
            categoryId = 1,
            filePath = "/images/smile.png",
            thumbPath = "/thumb/smile.jpg",
            mimeType = "image/png",
            fileSize = 1024,
            width = 128,
            height = 128,
            durationMs = 0,
            contentHash = "def456",
            usageCount = 100,
            isFavorite = true,
            source = "builtin",
            sourceApp = "com.example",
            aiCategory = "emotions",
            aiConfidence = 0.95f,
            syncId = "sync-001",
            syncedAt = now,
            sortOrder = 5,
            isDirty = false,
            isDeleted = false,
            createdAt = now,
            updatedAt = now
        )

        assertEquals(42L, emoji.id)
        assertEquals("smile", emoji.name)
        assertEquals("happy,face,smile", emoji.keywords)
        assertEquals(1L, emoji.categoryId)
        assertEquals(100L, emoji.usageCount)
        assertTrue(emoji.isFavorite)
        assertEquals("builtin", emoji.source)
        assertEquals("emotions", emoji.aiCategory)
        assertEquals(0.95f, emoji.aiConfidence, 0.001f)
        assertEquals("sync-001", emoji.syncId)
    }

    @Test
    fun `copy emoji with modified fields`() {
        val original = EmojiEntity(
            name = "test",
            filePath = "/test.png",
            mimeType = "image/png",
            contentHash = "hash1"
        )

        val modified = original.copy(
            name = "renamed",
            isFavorite = true,
            usageCount = 10
        )

        assertEquals("renamed", modified.name)
        assertTrue(modified.isFavorite)
        assertEquals(10L, modified.usageCount)
        // Original should be unchanged
        assertEquals("test", original.name)
        assertFalse(original.isFavorite)
    }
}
