package com.emoji.overlay.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emoji.overlay.data.dao.CategoryDao
import com.emoji.overlay.data.dao.EmojiDao
import com.emoji.overlay.data.dao.RecentHistoryDao
import com.emoji.overlay.data.dao.TagDao
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.EmojiTagCrossRef
import com.emoji.overlay.data.entity.RecentHistoryEntity
import com.emoji.overlay.data.entity.TagEntity

/**
 * Room database for the emoji repository.
 *
 * Version history:
 * - 1: Initial schema with all core tables
 *
 * Export schema for migration testing:
 * `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
 */
@Database(
    entities = [
        EmojiEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        EmojiTagCrossRef::class,
        RecentHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EmojiDatabase : RoomDatabase() {

    abstract fun emojiDao(): EmojiDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun recentHistoryDao(): RecentHistoryDao

    companion object {
        const val DATABASE_NAME = "emoji_repository.db"

        @Volatile
        private var INSTANCE: EmojiDatabase? = null

        fun getInstance(context: Context): EmojiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): EmojiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                EmojiDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }

        /**
         * Example migration for future schema changes.
         * This demonstrates the pattern; actual migration will be added when needed.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Example: Add a new column
                // db.execSQL("ALTER TABLE emojis ADD COLUMN new_column TEXT DEFAULT NULL")
            }
        }
    }

    /**
     * Database callback for first-time creation.
     * Seeds default categories.
     */
    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed default categories
            val defaults = listOf(
                "('Smileys & People', '😀', 1, 1, 1)",
                "('Animals & Nature', '🐱', 2, 1, 1)",
                "('Food & Drink', '🍕', 3, 1, 1)",
                "('Activities', '⚽', 4, 1, 1)",
                "('Travel & Places', '✈️', 5, 1, 1)",
                "('Objects', '💡', 6, 1, 1)",
                "('Symbols', '❤️', 7, 1, 1)",
                "('Flags', '🏳️', 8, 1, 1)",
                "('Custom', '⭐', 9, 0, 1)"
            )
            defaults.forEach { values ->
                db.execSQL(
                    "INSERT INTO categories (name, icon, sort_order, is_system, is_visible) VALUES $values"
                )
            }
        }
    }
}
