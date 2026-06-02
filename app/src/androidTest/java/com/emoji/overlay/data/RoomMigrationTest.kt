package com.emoji.overlay.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emoji.overlay.data.database.EmojiDatabase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Room migration tests using MigrationTestHelper.
 *
 * These tests verify that database schema migrations preserve data.
 * They run on a real Android device/emulator.
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    companion object {
        private const val TEST_DB = "migration-test-db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EmojiDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create database at version 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data at version 1
            execSQL("""
                INSERT INTO categories (name, icon, sort_order, is_system, is_visible, emoji_count, created_at, updated_at)
                VALUES ('Smileys', '😀', 1, 1, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent())

            execSQL("""
                INSERT INTO emojis (name, keywords, file_path, mime_type, content_hash, usage_count, is_favorite, source, ai_confidence, sync_id, synced_at, sort_order, is_dirty, is_deleted, created_at, updated_at)
                VALUES ('test_emoji', 'test', '/images/test.png', 'image/png', 'hash_1', 0, 0, 'imported', 0.0, NULL, 0, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent())

            close()
        }

        // Re-open at version 2 (migration should run)
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, EmojiDatabase.MIGRATION_1_2)

        // Verify data survived migration
        val cursor = db.query("SELECT COUNT(*) FROM emojis")
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        assertEquals(1, count)

        val cursor2 = db.query("SELECT name FROM emojis WHERE id = 1")
        cursor2.moveToFirst()
        val name = cursor2.getString(0)
        assertEquals("test_emoji", name)

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migratePreservesAllColumns() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO emojis (name, keywords, file_path, mime_type, content_hash, usage_count, is_favorite, source, ai_confidence, sync_id, synced_at, sort_order, is_dirty, is_deleted, created_at, updated_at)
                VALUES ('smile', 'happy,face', '/images/smile.png', 'image/png', 'abc123', 42, 1, 'builtin', 0.95, 'sync-001', ${System.currentTimeMillis()}, 5, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent())
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, EmojiDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT name, keywords, usage_count, is_favorite, source, ai_confidence FROM emojis WHERE id = 1")
        cursor.moveToFirst()

        assertEquals("smile", cursor.getString(0))
        assertEquals("happy,face", cursor.getString(1))
        assertEquals(42, cursor.getInt(2))
        assertEquals(1, cursor.getInt(3))
        assertEquals("builtin", cursor.getString(4))
        assertEquals(0.95f, cursor.getFloat(5), 0.001f)

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migratePreservesCategories() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO categories (name, icon, sort_order, is_system, is_visible, emoji_count, created_at, updated_at)
                VALUES ('Animals', '🐱', 2, 1, 1, 10, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent())
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, EmojiDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT name, icon, emoji_count FROM categories WHERE id = 1")
        cursor.moveToFirst()

        assertEquals("Animals", cursor.getString(0))
        assertEquals("🐱", cursor.getString(1))
        assertEquals(10, cursor.getInt(2))

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migratePreservesTags() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO tags (name, display_name, usage_count, created_at)
                VALUES ('happy', 'Happy', 25, ${System.currentTimeMillis()})
            """.trimIndent())
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, EmojiDatabase.MIGRATION_1_2)

        val cursor = db.query("SELECT name, display_name, usage_count FROM tags WHERE id = 1")
        cursor.moveToFirst()

        assertEquals("happy", cursor.getString(0))
        assertEquals("Happy", cursor.getString(1))
        assertEquals(25, cursor.getInt(2))

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migratePreservesRelationships() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO emojis (name, keywords, file_path, mime_type, content_hash, usage_count, is_favorite, source, ai_confidence, sync_id, synced_at, sort_order, is_dirty, is_deleted, created_at, updated_at)
                VALUES ('smile', '', '/smile.png', 'image/png', 'h1', 0, 0, 'imported', 0.0, NULL, 0, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent())
            execSQL("""
                INSERT INTO tags (name, display_name, usage_count, created_at)
                VALUES ('face', 'Face', 0, ${System.currentTimeMillis()})
            """.trimIndent())
            execSQL("""
                INSERT INTO emoji_tag_cross_ref (emoji_id, tag_id, created_at)
                VALUES (1, 1, ${System.currentTimeMillis()})
            """.trimIndent())
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, EmojiDatabase.MIGRATION_1_2)

        val cursor = db.query("""
            SELECT e.name, t.name FROM emojis e
            INNER JOIN emoji_tag_cross_ref ref ON e.id = ref.emoji_id
            INNER JOIN tags t ON ref.tag_id = t.id
            WHERE e.id = 1
        """)
        cursor.moveToFirst()

        assertEquals("smile", cursor.getString(0))
        assertEquals("face", cursor.getString(1))

        db.close()
    }
}
