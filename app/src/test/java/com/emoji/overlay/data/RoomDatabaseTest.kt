package com.emoji.overlay.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.emoji.overlay.data.dao.CategoryDao
import com.emoji.overlay.data.dao.EmojiDao
import com.emoji.overlay.data.dao.RecentHistoryDao
import com.emoji.overlay.data.dao.TagDao
import com.emoji.overlay.data.database.EmojiDatabase
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.EmojiTagCrossRef
import com.emoji.overlay.data.entity.RecentHistoryEntity
import com.emoji.overlay.data.entity.TagEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room database tests using Robolectric.
 *
 * These tests run on JVM (no Android device needed) and perform
 * actual Room database operations including insert, query, delete,
 * and performance benchmarks.
 *
 * Requirements: JDK 17 (Robolectric 4.13 doesn't support JDK 25)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RoomDatabaseTest {

    private lateinit var database: EmojiDatabase
    private lateinit var emojiDao: EmojiDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var tagDao: TagDao
    private lateinit var recentHistoryDao: RecentHistoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, EmojiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        emojiDao = database.emojiDao()
        categoryDao = database.categoryDao()
        tagDao = database.tagDao()
        recentHistoryDao = database.recentHistoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== INSERT TESTS ====================

    @Test
    fun insertSingleEmoji() = runBlocking {
        val emoji = createTestEmoji(1, "smile")
        val id = emojiDao.insert(emoji)
        assertTrue(id > 0)

        val retrieved = emojiDao.getById(id)
        assertNotNull(retrieved)
        assertEquals("smile", retrieved?.name)
    }

    @Test
    fun insert100Emojis() = runBlocking {
        val emojis = (1L..100L).map { createTestEmoji(it, "emoji_$it") }
        val ids = emojiDao.insertAll(emojis)

        assertEquals(100, ids.size)
        assertTrue(ids.all { it > 0 })

        val count = emojiDao.getCount()
        assertEquals(100, count)
    }

    @Test
    fun insert1000Emojis() = runBlocking {
        val emojis = (1L..1000L).map { createTestEmoji(it, "emoji_$it") }
        val start = System.currentTimeMillis()
        val ids = emojiDao.insertBatch(emojis, batchSize = 500)
        val elapsed = System.currentTimeMillis() - start

        assertEquals(1000, ids.size)
        println("=== PERFORMANCE: Insert 1000 emojis (Room) ===")
        println("Time: ${elapsed}ms")
        assertTrue("1000 inserts should complete within 5000ms", elapsed < 5000)
    }

    @Test
    fun insert10000Emojis() = runBlocking {
        val emojis = (1L..10000L).map { createTestEmoji(it, "emoji_$it") }
        val start = System.currentTimeMillis()
        val ids = emojiDao.insertBatch(emojis, batchSize = 500)
        val elapsed = System.currentTimeMillis() - start

        assertEquals(10000, ids.size)
        println("=== PERFORMANCE: Insert 10000 emojis (Room) ===")
        println("Time: ${elapsed}ms")
        println("Rate: ${10000 * 1000 / elapsed} inserts/sec")
        assertTrue("10000 inserts should complete within 30000ms", elapsed < 30000)
    }

    // ==================== QUERY TESTS ====================

    @Test
    fun queryRecentEmojis() = runBlocking {
        val emojis = (1L..50L).map { createTestEmoji(it, "emoji_$it") }
        emojiDao.insertAll(emojis)

        val histories = (1L..50L).map {
            RecentHistoryEntity(emojiId = it, usedAt = System.currentTimeMillis() - (50 - it) * 1000)
        }
        recentHistoryDao.insertAll(histories)

        val recent = emojiDao.getRecent(limit = 10)
        assertEquals(10, recent.size)
        assertEquals(50L, recent.first().id)
    }

    @Test
    fun queryFavorites() = runBlocking {
        val emojis = (1L..100L).map {
            createTestEmoji(it, "emoji_$it", isFavorite = it % 5 == 0L)
        }
        emojiDao.insertAll(emojis)

        val favorites = emojiDao.getFavorites(limit = 50)
        assertEquals(20, favorites.size)
        assertTrue(favorites.all { it.isFavorite })
    }

    @Test
    fun queryByCategory() = runBlocking {
        val category = CategoryEntity(name = "Smileys")
        val categoryId = categoryDao.insert(category)

        val emojis = (1L..50L).map {
            createTestEmoji(it, "emoji_$it", categoryId = if (it <= 20) categoryId else null)
        }
        emojiDao.insertAll(emojis)

        val result = emojiDao.getByCategory(categoryId, limit = 50)
        assertEquals(20, result.size)
    }

    @Test
    fun queryByTag() = runBlocking {
        val tag = TagEntity(name = "happy", displayName = "Happy")
        val tagId = tagDao.insert(tag)

        val emojis = (1L..50L).map { createTestEmoji(it, "emoji_$it") }
        emojiDao.insertAll(emojis)

        val refs = (1L..25L).map { EmojiTagCrossRef(emojiId = it, tagId = tagId) }
        tagDao.insertCrossRefs(refs)

        val result = emojiDao.getByTag(tagId, limit = 50)
        assertEquals(25, result.size)
    }

    @Test
    fun searchEmojis() = runBlocking {
        val emojis = listOf(
            createTestEmoji(1, "happy_face", keywords = "smile,joy"),
            createTestEmoji(2, "sad_face", keywords = "cry,tears"),
            createTestEmoji(3, "happy_heart", keywords = "love"),
            createTestEmoji(4, "angry_face", keywords = "mad"),
            createTestEmoji(5, "happy_cat", keywords = "animal")
        )
        emojiDao.insertAll(emojis)

        val results = emojiDao.search("happy")
        assertEquals(3, results.size)
        assertTrue(results.all { it.name.contains("happy") })
    }

    @Test
    fun searchInCategory() = runBlocking {
        val cat1 = categoryDao.insert(CategoryEntity(name = "Faces"))
        val cat2 = categoryDao.insert(CategoryEntity(name = "Hearts"))

        val emojis = listOf(
            createTestEmoji(1, "happy_face", categoryId = cat1),
            createTestEmoji(2, "sad_face", categoryId = cat1),
            createTestEmoji(3, "happy_heart", categoryId = cat2),
            createTestEmoji(4, "red_heart", categoryId = cat2)
        )
        emojiDao.insertAll(emojis)

        val results = emojiDao.searchInCategory("happy", cat1)
        assertEquals(1, results.size)
        assertEquals("happy_face", results.first().name)
    }

    // ==================== DELETE TESTS ====================

    @Test
    fun deleteSingleEmoji() = runBlocking {
        val emoji = createTestEmoji(1, "smile")
        val id = emojiDao.insert(emoji)

        emojiDao.deleteById(id)
        val retrieved = emojiDao.getById(id)
        assertNull(retrieved)
    }

    @Test
    fun batchDeleteEmojis() = runBlocking {
        val emojis = (1L..100L).map { createTestEmoji(it, "emoji_$it") }
        val ids = emojiDao.insertAll(emojis)

        val idsToDelete = ids.take(50)
        emojiDao.deleteByIds(idsToDelete)

        val remaining = emojiDao.getCount()
        assertEquals(50, remaining)
    }

    @Test
    fun softDeleteEmojis() = runBlocking {
        val emojis = (1L..100L).map { createTestEmoji(it, "emoji_$it") }
        emojiDao.insertAll(emojis)

        emojiDao.softDelete(listOf(1, 2, 3, 4, 5))

        val active = emojiDao.getCount()
        assertEquals(95, active)
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    fun performanceInsert10000() = runBlocking {
        val emojis = (1L..10000L).map { createTestEmoji(it, "emoji_$it") }

        val start = System.currentTimeMillis()
        emojiDao.insertBatch(emojis, batchSize = 500)
        val elapsed = System.currentTimeMillis() - start

        println("========================================")
        println("=== PERFORMANCE TEST: Insert 10000 emojis (Room) ===")
        println("Time: ${elapsed}ms")
        println("Rate: ${10000 * 1000 / elapsed} inserts/sec")
        println("========================================")
        assertTrue("Should complete within 30s", elapsed < 30000)
    }

    @Test
    fun performanceQueryByIndex() = runBlocking {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, "emoji_$it", categoryId = it % 10, usageCount = it)
        }
        emojiDao.insertBatch(emojis, batchSize = 500)

        // Query by category (indexed)
        val start1 = System.currentTimeMillis()
        val byCategory = emojiDao.getByCategory(5L, limit = 100)
        val elapsed1 = System.currentTimeMillis() - start1
        println("========================================")
        println("=== PERFORMANCE TEST: Query by category (10k records) ===")
        println("Time: ${elapsed1}ms, Results: ${byCategory.size}")
        println("========================================")

        // Query favorites (indexed)
        val start2 = System.currentTimeMillis()
        val favorites = emojiDao.getFavorites(limit = 100)
        val elapsed2 = System.currentTimeMillis() - start2
        println("=== PERFORMANCE TEST: Query favorites (10k records) ===")
        println("Time: ${elapsed2}ms, Results: ${favorites.size}")
        println("========================================")

        assertTrue("Index query should be < 100ms", elapsed1 < 100)
    }

    @Test
    fun performanceSearch() = runBlocking {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, "emoji_${it}_face", keywords = "kw_${it % 100}")
        }
        emojiDao.insertBatch(emojis, batchSize = 500)

        val start = System.currentTimeMillis()
        val results = emojiDao.search("500")
        val elapsed = System.currentTimeMillis() - start

        println("========================================")
        println("=== PERFORMANCE TEST: Search in 10k records (Room) ===")
        println("Time: ${elapsed}ms, Results: ${results.size}")
        println("========================================")
        assertTrue("Search should be < 200ms", elapsed < 200)
    }

    @Test
    fun performanceSortByUsage() = runBlocking {
        val emojis = (1L..10000L).map {
            createTestEmoji(it, "emoji_$it", usageCount = 10001 - it)
        }
        emojiDao.insertBatch(emojis, batchSize = 500)

        val start = System.currentTimeMillis()
        val all = emojiDao.getAll(limit = 10000)
        val elapsed = System.currentTimeMillis() - start

        println("========================================")
        println("=== PERFORMANCE TEST: Sort 10k records by usage (Room) ===")
        println("Time: ${elapsed}ms")
        println("========================================")
        assertTrue("Sort should be < 500ms", elapsed < 500)
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        id: Long = 0,
        name: String = "test",
        keywords: String = "",
        categoryId: Long? = null,
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
