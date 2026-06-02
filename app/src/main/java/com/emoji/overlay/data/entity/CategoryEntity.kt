package com.emoji.overlay.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Category entity for organizing emojis into groups.
 *
 * Supports hierarchical categories via [parentId].
 * Example: "Smileys" > "Positive" > "Love"
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["parent_id"]),
        Index(value = ["name"], unique = true),
        Index(value = ["sort_order"]),
        Index(value = ["sync_id"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Category display name */
    @ColumnInfo(name = "name")
    val name: String,

    /** Parent category ID for hierarchical structure (null = root) */
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,

    /** Icon/emoji representing this category */
    @ColumnInfo(name = "icon")
    val icon: String = "📁",

    /** Description text */
    @ColumnInfo(name = "description")
    val description: String = "",

    /** Custom sort order */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** Whether this is a system/builtin category */
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,

    /** Cloud sync ID */
    @ColumnInfo(name = "sync_id")
    val syncId: String? = null,

    /** Whether this category is visible */
    @ColumnInfo(name = "is_visible")
    val isVisible: Boolean = true,

    /** Emoji count in this category (denormalized for performance) */
    @ColumnInfo(name = "emoji_count")
    val emojiCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
