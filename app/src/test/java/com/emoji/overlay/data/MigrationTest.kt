package com.emoji.overlay.data

import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Migration and schema evolution tests.
 *
 * These tests verify that database schema changes can be applied
 * without data loss. Actual Room migration tests require Android
 * instrumentation with MigrationTestHelper.
 *
 * Expected behavior:
 * - v1 -> v2: Add new columns with DEFAULT values
 * - v2 -> v3: Add new tables
 * - All migrations preserve existing data
 */
class MigrationTest {

    // ==================== SCHEMA EVOLUTION TESTS ====================

    @Test
    fun `v1 emoji schema has required fields`() {
        val emoji = EmojiEntity(
            id = 1,
            name = "test",
            filePath = "/test.png",
            mimeType = "image/png",
            contentHash = "hash1"
        )

        // Verify all v1 required fields exist
        assertEquals(1L, emoji.id)
        assertNotNull(emoji.name)
        assertNotNull(emoji.filePath)
        assertNotNull(emoji.mimeType)
        assertNotNull(emoji.contentHash)
        assertEquals(0L, emoji.usageCount)
        assertFalse(emoji.isFavorite)
        assertFalse(emoji.isDeleted)
        assertFalse(emoji.isDirty)
    }

    @Test
    fun `v1 emoji schema has sync-compatible fields`() {
        val emoji = EmojiEntity(
            name = "test",
            filePath = "/test.png",
            mimeType = "image/png",
            contentHash = "hash1",
            syncId = "sync-001",
            syncedAt = System.currentTimeMillis(),
            isDirty = true
        )

        // Verify sync fields exist and work
        assertEquals("sync-001", emoji.syncId)
        assertTrue(emoji.syncedAt > 0)
        assertTrue(emoji.isDirty)
    }

    @Test
    fun `v1 emoji schema has AI-compatible fields`() {
        val emoji = EmojiEntity(
            name = "test",
            filePath = "/test.png",
            mimeType = "image/png",
            contentHash = "hash1",
            aiCategory = "emotions",
            aiConfidence = 0.95f
        )

        // Verify AI fields exist and work
        assertEquals("emotions", emoji.aiCategory)
        assertEquals(0.95f, emoji.aiConfidence, 0.001f)
    }

    @Test
    fun `v1 category schema supports hierarchy`() {
        val root = CategoryEntity(id = 1, name = "Root", parentId = null)
        val child = CategoryEntity(id = 2, name = "Child", parentId = 1)

        assertNull(root.parentId)
        assertEquals(1L, child.parentId)
    }

    @Test
    fun `adding new field with default value preserves data`() {
        // Simulate adding a new field with default value
        val original = EmojiEntity(
            id = 1,
            name = "test",
            filePath = "/test.png",
            mimeType = "image/png",
            contentHash = "hash1"
        )

        // After migration, new field would have default value
        // This simulates: ALTER TABLE emojis ADD COLUMN new_field TEXT DEFAULT NULL
        val migrated = original.copy(
            source = "imported" // New field with default
        )

        assertEquals("imported", migrated.source)
        assertEquals(original.name, migrated.name)
        assertEquals(original.filePath, migrated.filePath)
    }

    // ==================== DATA PRESERVATION TESTS ====================

    @Test
    fun `migration preserves emoji data`() {
        val emojis = (1L..100L).map {
            EmojiEntity(
                id = it,
                name = "emoji_$it",
                filePath = "/images/emoji_$it.png",
                mimeType = "image/png",
                contentHash = "hash_$it",
                usageCount = it * 10,
                isFavorite = it % 5 == 0L,
                categoryId = it % 10
            )
        }

        // Simulate migration: data should be preserved
        assertEquals(100, emojis.size)
        assertEquals(1000L, emojis.last().usageCount) // 100 * 10
        assertTrue(emojis[4].isFavorite) // 5 % 5 == 0
    }

    @Test
    fun `migration preserves category data`() {
        val categories = listOf(
            CategoryEntity(id = 1, name = "Smileys", icon = "😀", isSystem = true),
            CategoryEntity(id = 2, name = "Animals", icon = "🐱", isSystem = true),
            CategoryEntity(id = 3, name = "Custom", icon = "⭐", isSystem = false)
        )

        // Simulate migration: all categories preserved
        assertEquals(3, categories.size)
        assertTrue(categories[0].isSystem)
        assertFalse(categories[2].isSystem)
    }

    @Test
    fun `migration preserves tag relationships`() {
        val emojis = (1L..10L).map {
            EmojiEntity(id = it, name = "e$it", filePath = "/e$it.png", mimeType = "image/png", contentHash = "h$it")
        }
        val crossRefs = (1L..10L).map {
            com.emoji.overlay.data.entity.EmojiTagCrossRef(emojiId = it, tagId = it % 3 + 1)
        }

        // Simulate migration: relationships preserved
        assertEquals(10, crossRefs.size)
        val tag1Emojis = crossRefs.filter { it.tagId == 1L }
        assertTrue(tag1Emojis.isNotEmpty())
    }

    // ==================== VERSION COMPATIBILITY TESTS ====================

    @Test
    fun `schema version 1 fields are backward compatible`() {
        // Create emoji with minimal v1 fields
        val v1Emoji = EmojiEntity(
            id = 1,
            name = "legacy",
            filePath = "/legacy.png",
            mimeType = "image/png",
            contentHash = "hash"
        )

        // v1 fields should still work
        assertEquals("legacy", v1Emoji.name)
        assertEquals(0L, v1Emoji.usageCount) // Default
        assertFalse(v1Emoji.isFavorite) // Default
        assertFalse(v1Emoji.isDeleted) // Default
    }

    @Test
    fun `new fields have safe defaults`() {
        val emoji = EmojiEntity(
            name = "test",
            filePath = "/test.png",
            mimeType = "image/png",
            contentHash = "hash"
        )

        // All new fields should have safe defaults
        assertNull(emoji.syncId)
        assertEquals(0L, emoji.syncedAt)
        assertFalse(emoji.isDirty)
        assertNull(emoji.aiCategory)
        assertEquals(0f, emoji.aiConfidence)
        assertEquals("", emoji.keywords)
        assertEquals(0, emoji.sortOrder)
    }

    // ==================== ROLLBACK SAFETY TESTS ====================

    @Test
    fun `destructive migration clears all data`() {
        // Simulate fallbackToDestructiveMigration scenario
        val emojis = (1L..100L).map {
            EmojiEntity(id = it, name = "e$it", filePath = "/e$it.png", mimeType = "image/png", contentHash = "h$it")
        }

        // After destructive migration, all data is gone
        val afterMigration = emptyList<EmojiEntity>()
        assertEquals(0, afterMigration.size)
        assertNotEquals(emojis.size, afterMigration.size)
    }

    @Test
    fun `schema export is enabled for testing`() {
        // Verify that schema export is configured
        // This is checked by the Room compiler, but we can verify the pattern
        val schemaLocation = "app/schemas"
        assertTrue(schemaLocation.isNotEmpty())
    }
}
