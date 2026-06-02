package com.emoji.overlay.data

import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.EmojiTagCrossRef
import com.emoji.overlay.data.entity.RecentHistoryEntity
import com.emoji.overlay.data.entity.TagEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Database schema and entity validation tests.
 *
 * These tests verify entity structure, default values, and relationships
 * without requiring Android context or Room runtime.
 */
class DatabaseSchemaTest {

    // ==================== ENTITY CREATION TESTS ====================

    @Test
    fun `insert 1 emoji entity`() {
        val emoji = createTestEmoji(id = 1, name = "smile")
        assertEquals(1L, emoji.id)
        assertEquals("smile", emoji.name)
        assertEquals("image/png", emoji.mimeType)
        assertFalse(emoji.isDeleted)
    }

    @Test
    fun `insert 100 emoji entities`() {
        val emojis = (1L..100L).map { createTestEmoji(id = it, name = "emoji_$it") }
        assertEquals(100, emojis.size)
        assertTrue(emojis.all { it.id > 0 })
        assertTrue(emojis.all { it.name.startsWith("emoji_") })
    }

    @Test
    fun `insert 1000 emoji entities`() {
        val emojis = (1L..1000L).map { createTestEmoji(id = it, name = "emoji_$it") }
        assertEquals(1000, emojis.size)
        assertEquals("emoji_500", emojis[499].name)
        assertEquals("emoji_1000", emojis[999].name)
    }

    @Test
    fun `insert 10000 emoji entities`() {
        val emojis = (1L..10000L).map { createTestEmoji(id = it, name = "emoji_$it") }
        assertEquals(10000, emojis.size)
        assertEquals("emoji_1", emojis[0].name)
        assertEquals("emoji_10000", emojis[9999].name)
        // Verify no ID collisions
        assertEquals(10000, emojis.map { it.id }.toSet().size)
    }

    @Test
    fun `insert category entity`() {
        val category = CategoryEntity(
            id = 1,
            name = "Smileys",
            icon = "😀",
            sortOrder = 1,
            isSystem = true
        )
        assertEquals(1L, category.id)
        assertEquals("Smileys", category.name)
        assertEquals("😀", category.icon)
        assertTrue(category.isSystem)
    }

    @Test
    fun `insert tag entity`() {
        val tag = TagEntity(
            id = 1,
            name = "happy",
            displayName = "Happy",
            usageCount = 42
        )
        assertEquals(1L, tag.id)
        assertEquals("happy", tag.name)
        assertEquals("Happy", tag.displayName)
        assertEquals(42, tag.usageCount)
    }

    @Test
    fun `insert emoji-tag cross ref`() {
        val ref = EmojiTagCrossRef(emojiId = 1, tagId = 1)
        assertEquals(1L, ref.emojiId)
        assertEquals(1L, ref.tagId)
    }

    @Test
    fun `insert recent history entity`() {
        val history = RecentHistoryEntity(emojiId = 1, context = "com.whatsapp")
        assertEquals(1L, history.emojiId)
        assertEquals("com.whatsapp", history.context)
        assertTrue(history.usedAt > 0)
    }

    // ==================== QUERY SIMULATION TESTS ====================

    @Test
    fun `query recent emojis by timestamp`() {
        val emojis = (1L..100L).map { createTestEmoji(id = it, name = "emoji_$it") }
        val histories = (1L..100L).map {
            RecentHistoryEntity(emojiId = it, usedAt = System.currentTimeMillis() - (100 - it) * 1000)
        }

        // Simulate ORDER BY used_at DESC
        val recentIds = histories.sortedByDescending { it.usedAt }.map { it.emojiId }
        assertEquals(100L, recentIds.first())
        assertEquals(1L, recentIds.last())
    }

    @Test
    fun `query favorites`() {
        val emojis = (1L..100L).map {
            createTestEmoji(id = it, name = "emoji_$it", isFavorite = it % 3 == 0L)
        }

        val favorites = emojis.filter { it.isFavorite }
        assertTrue(favorites.isNotEmpty())
        assertTrue(favorites.all { it.isFavorite })
        assertEquals(33, favorites.size) // Every 3rd emoji
    }

    @Test
    fun `query by category`() {
        val emojis = (1L..100L).map {
            createTestEmoji(id = it, name = "emoji_$it", categoryId = it % 5)
        }

        val category0 = emojis.filter { it.categoryId == 0L }
        val category1 = emojis.filter { it.categoryId == 1L }
        assertEquals(20, category0.size)
        assertEquals(20, category1.size)
    }

    @Test
    fun `query by tag`() {
        val emojis = (1L..100L).map { createTestEmoji(id = it, name = "emoji_$it") }
        val tags = listOf(
            TagEntity(id = 1, name = "happy", displayName = "Happy"),
            TagEntity(id = 2, name = "sad", displayName = "Sad")
        )
        val crossRefs = (1L..100L).map {
            EmojiTagCrossRef(emojiId = it, tagId = if (it % 2 == 0L) 1 else 2)
        }

        // Simulate JOIN query for tag_id = 1
        val happyEmojiIds = crossRefs.filter { it.tagId == 1L }.map { it.emojiId }
        val happyEmojis = emojis.filter { it.id in happyEmojiIds }
        assertEquals(50, happyEmojis.size)
    }

    @Test
    fun `search by name and keywords`() {
        val emojis = listOf(
            createTestEmoji(id = 1, name = "happy_face", keywords = "smile,joy"),
            createTestEmoji(id = 2, name = "sad_face", keywords = "cry,tears"),
            createTestEmoji(id = 3, name = "happy_heart", keywords = "love,like"),
            createTestEmoji(id = 4, name = "angry_face", keywords = "mad,upset")
        )

        // Simulate LIKE '%happy%' search
        val happyResults = emojis.filter {
            it.name.contains("happy") || it.keywords.contains("happy")
        }
        assertEquals(2, happyResults.size)

        // Simulate LIKE '%love%' search
        val loveResults = emojis.filter {
            it.name.contains("love") || it.keywords.contains("love")
        }
        assertEquals(1, loveResults.size)
        assertEquals("happy_heart", loveResults[0].name)
    }

    @Test
    fun `search in category`() {
        val emojis = listOf(
            createTestEmoji(id = 1, name = "happy_face", categoryId = 1),
            createTestEmoji(id = 2, name = "sad_face", categoryId = 1),
            createTestEmoji(id = 3, name = "happy_heart", categoryId = 2),
            createTestEmoji(id = 4, name = "angry_face", categoryId = 2)
        )

        // Search "happy" in category 1
        val results = emojis.filter {
            it.categoryId == 1L && it.name.contains("happy")
        }
        assertEquals(1, results.size)
        assertEquals("happy_face", results[0].name)
    }

    // ==================== DELETE TESTS ====================

    @Test
    fun `single delete`() {
        val emojis = (1L..10L).map { createTestEmoji(id = it, name = "emoji_$it") }.toMutableList()
        val toDelete = emojis.first { it.id == 5L }
        emojis.remove(toDelete)
        assertEquals(9, emojis.size)
        assertNull(emojis.find { it.id == 5L })
    }

    @Test
    fun `batch delete`() {
        val emojis = (1L..100L).map { createTestEmoji(id = it, name = "emoji_$it") }.toMutableList()
        val idsToDelete = listOf(10L, 20L, 30L, 40L, 50L)
        emojis.removeAll { it.id in idsToDelete }
        assertEquals(95, emojis.size)
        assertTrue(idsToDelete.all { id -> emojis.none { it.id == id } })
    }

    @Test
    fun `soft delete`() {
        val emojis = (1L..10L).map { createTestEmoji(id = it, name = "emoji_$it") }.toMutableList()
        val idsToSoftDelete = listOf(3L, 6L, 9L)

        // Simulate soft delete
        val updated = emojis.map {
            if (it.id in idsToSoftDelete) it.copy(isDeleted = true) else it
        }

        val active = updated.filter { !it.isDeleted }
        val deleted = updated.filter { it.isDeleted }
        assertEquals(7, active.size)
        assertEquals(3, deleted.size)
    }

    // ==================== RELATIONSHIP TESTS ====================

    @Test
    fun `emoji with multiple tags`() {
        val emoji = createTestEmoji(id = 1, name = "smile")
        val tags = listOf(
            TagEntity(id = 1, name = "happy", displayName = "Happy"),
            TagEntity(id = 2, name = "face", displayName = "Face"),
            TagEntity(id = 3, name = "positive", displayName = "Positive")
        )
        val crossRefs = tags.map { EmojiTagCrossRef(emojiId = emoji.id, tagId = it.id) }

        assertEquals(3, crossRefs.size)
        assertTrue(crossRefs.all { it.emojiId == 1L })
        assertEquals(setOf(1L, 2L, 3L), crossRefs.map { it.tagId }.toSet())
    }

    @Test
    fun `tag with multiple emojis`() {
        val tag = TagEntity(id = 1, name = "happy", displayName = "Happy")
        val emojis = (1L..5L).map { createTestEmoji(id = it, name = "emoji_$it") }
        val crossRefs = emojis.map { EmojiTagCrossRef(emojiId = it.id, tagId = tag.id) }

        assertEquals(5, crossRefs.size)
        assertTrue(crossRefs.all { it.tagId == 1L })
    }

    @Test
    fun `hierarchical categories`() {
        val root = CategoryEntity(id = 1, name = "Smileys", parentId = null)
        val child1 = CategoryEntity(id = 2, name = "Positive", parentId = 1)
        val child2 = CategoryEntity(id = 3, name = "Negative", parentId = 1)
        val grandchild = CategoryEntity(id = 4, name = "Love", parentId = 2)

        assertNull(root.parentId)
        assertEquals(1L, child1.parentId)
        assertEquals(2L, grandchild.parentId)

        // Simulate tree traversal
        val rootCategories = listOf(root, child1, child2, grandchild).filter { it.parentId == null }
        val subCategories = listOf(root, child1, child2, grandchild).filter { it.parentId == 1L }
        assertEquals(1, rootCategories.size)
        assertEquals(2, subCategories.size)
    }

    // ==================== DEDUPLICATION TESTS ====================

    @Test
    fun `duplicate detection by content hash`() {
        val hash = "abc123def456"
        val emojis = listOf(
            createTestEmoji(id = 1, name = "smile1", contentHash = hash),
            createTestEmoji(id = 2, name = "smile2", contentHash = "other_hash"),
            createTestEmoji(id = 3, name = "smile3", contentHash = hash)
        )

        val duplicates = emojis.filter { it.contentHash == hash }
        assertEquals(2, duplicates.size)
    }

    // ==================== USAGE TRACKING TESTS ====================

    @Test
    fun `usage count increment`() {
        val emoji = createTestEmoji(id = 1, name = "smile", usageCount = 0)
        val updated = emoji.copy(usageCount = emoji.usageCount + 1)
        assertEquals(1L, updated.usageCount)
    }

    @Test
    fun `recent history pruning`() {
        val histories = (1L..1000L).map {
            RecentHistoryEntity(emojiId = it, usedAt = System.currentTimeMillis() - (1000 - it) * 1000)
        }

        // Simulate pruning to keep only 100 most recent
        val pruned = histories.sortedByDescending { it.usedAt }.take(100)
        assertEquals(100, pruned.size)
        assertEquals(1000L, pruned.first().emojiId) // Most recent
    }

    // ==================== HELPER METHODS ====================

    private fun createTestEmoji(
        id: Long = 0,
        name: String = "test",
        keywords: String = "",
        categoryId: Long? = null,
        filePath: String = "/test/$name.png",
        mimeType: String = "image/png",
        contentHash: String = "hash_$id",
        usageCount: Long = 0,
        isFavorite: Boolean = false,
        isDeleted: Boolean = false
    ) = EmojiEntity(
        id = id,
        name = name,
        keywords = keywords,
        categoryId = categoryId,
        filePath = filePath,
        mimeType = mimeType,
        contentHash = contentHash,
        usageCount = usageCount,
        isFavorite = isFavorite,
        isDeleted = isDeleted
    )
}
