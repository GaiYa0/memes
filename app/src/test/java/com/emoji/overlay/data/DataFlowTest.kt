package com.emoji.overlay.data

import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.RecentHistoryEntity
import com.emoji.overlay.data.entity.TagEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for data entity creation and properties.
 * Verifies the data layer is correctly structured.
 */
class DataFlowTest {

    @Test
    fun `emoji entity has all required fields`() {
        val emoji = EmojiEntity(
            name = "test",
            filePath = "images/test.png",
            mimeType = "image/png",
            contentHash = "abc123",
            fileSize = 1024,
            categoryId = 1,
            keywords = "test,emoji",
            isFavorite = false,
            usageCount = 0
        )

        assertEquals("test", emoji.name)
        assertEquals("images/test.png", emoji.filePath)
        assertEquals("image/png", emoji.mimeType)
        assertEquals("abc123", emoji.contentHash)
        assertEquals(1024L, emoji.fileSize)
        assertEquals(1L, emoji.categoryId)
        assertEquals("test,emoji", emoji.keywords)
        assertFalse(emoji.isFavorite)
        assertEquals(0L, emoji.usageCount)
    }

    @Test
    fun `category entity has correct defaults`() {
        val category = CategoryEntity(
            name = "Test Category",
            icon = "📁"
        )

        assertEquals("Test Category", category.name)
        assertEquals("📁", category.icon)
        assertNull(category.parentId)
        assertEquals(0, category.sortOrder)
        assertTrue(category.isVisible)
        assertFalse(category.isSystem)
        assertEquals(0, category.emojiCount)
    }

    @Test
    fun `tag entity normalizes correctly`() {
        val tag = TagEntity(
            name = "happy",
            displayName = "Happy"
        )

        assertEquals("happy", tag.name)
        assertEquals("Happy", tag.displayName)
        assertEquals(0, tag.usageCount)
    }

    @Test
    fun `recent history entity records timestamp`() {
        val before = System.currentTimeMillis()
        val history = RecentHistoryEntity(
            emojiId = 42,
            context = "test"
        )
        val after = System.currentTimeMillis()

        assertEquals(42L, history.emojiId)
        assertEquals("test", history.context)
        assertTrue(history.usedAt in before..after)
    }

    @Test
    fun `emoji entity can be favorited`() {
        val emoji = EmojiEntity(
            name = "heart",
            filePath = "images/heart.png",
            mimeType = "image/png",
            contentHash = "hash123",
            isFavorite = true
        )

        assertTrue(emoji.isFavorite)
    }

    @Test
    fun `emoji entity supports different mime types`() {
        val png = createEmojiWithType("image/png")
        val gif = createEmojiWithType("image/gif")
        val webp = createEmojiWithType("image/webp")
        val jpg = createEmojiWithType("image/jpeg")

        assertEquals("image/png", png.mimeType)
        assertEquals("image/gif", gif.mimeType)
        assertEquals("image/webp", webp.mimeType)
        assertEquals("image/jpeg", jpg.mimeType)
    }

    @Test
    fun `emoji entity tracks usage count`() {
        val emoji = EmojiEntity(
            name = "popular",
            filePath = "images/popular.png",
            mimeType = "image/png",
            contentHash = "hash456",
            usageCount = 999
        )

        assertEquals(999L, emoji.usageCount)
    }

    @Test
    fun `category entity supports hierarchy`() {
        val parent = CategoryEntity(name = "Parent", icon = "📁")
        val child = CategoryEntity(name = "Child", icon = "📂", parentId = 1L)

        assertNull(parent.parentId)
        assertEquals(1L, child.parentId)
    }

    @Test
    fun `emoji entity default source is imported`() {
        val emoji = EmojiEntity(
            name = "test",
            filePath = "test.png",
            mimeType = "image/png",
            contentHash = "hash"
        )

        assertEquals("imported", emoji.source)
    }

    @Test
    fun `emoji entity supports soft delete`() {
        val active = EmojiEntity(
            name = "active",
            filePath = "a.png",
            mimeType = "image/png",
            contentHash = "h1",
            isDeleted = false
        )
        val deleted = EmojiEntity(
            name = "deleted",
            filePath = "d.png",
            mimeType = "image/png",
            contentHash = "h2",
            isDeleted = true
        )

        assertFalse(active.isDeleted)
        assertTrue(deleted.isDeleted)
    }

    private fun createEmojiWithType(mimeType: String) = EmojiEntity(
        name = "test",
        filePath = "test.png",
        mimeType = mimeType,
        contentHash = "hash_${mimeType.hashCode()}"
    )
}
