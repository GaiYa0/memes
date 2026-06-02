package com.emoji.overlay.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Core emoji entity storing metadata for each emoji resource.
 *
 * Design considerations:
 * - [contentHash] for file deduplication (MD5/SHA-256 of file content)
 * - [mimeType] for future format-specific handling
 * - [syncId] for future cloud sync compatibility
 * - [aiCategory] for future AI auto-classification
 * - Compound indexes for common query patterns
 */
@Entity(
    tableName = "emojis",
    indices = [
        Index(value = ["content_hash"], unique = true),
        Index(value = ["category_id"]),
        Index(value = ["mime_type"]),
        Index(value = ["created_at"]),
        Index(value = ["usage_count"], orders = [Index.Order.DESC]),
        Index(value = ["is_favorite"], orders = [Index.Order.DESC]),
        Index(value = ["sync_id"]),
        Index(value = ["ai_category"]),
        // Compound index for common search pattern
        Index(value = ["category_id", "is_favorite", "usage_count"]),
        // For name/keyword search
        Index(value = ["name", "keywords"])
    ]
)
data class EmojiEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display name for the emoji */
    @ColumnInfo(name = "name")
    val name: String,

    /** Searchable keywords, comma-separated */
    @ColumnInfo(name = "keywords")
    val keywords: String = "",

    /** Category ID reference */
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    /** File path relative to app storage root */
    @ColumnInfo(name = "file_path")
    val filePath: String,

    /** Thumbnail file path (nullable for small files) */
    @ColumnInfo(name = "thumb_path")
    val thumbPath: String? = null,

    /** MIME type: image/png, image/gif, image/webp, etc. */
    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    /** File size in bytes */
    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0,

    /** Image width in pixels */
    @ColumnInfo(name = "width")
    val width: Int = 0,

    /** Image height in pixels */
    @ColumnInfo(name = "height")
    val height: Int = 0,

    /** Duration in ms for animated formats (0 for static) */
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0,

    /** MD5/SHA-256 hash for deduplication */
    @ColumnInfo(name = "content_hash")
    val contentHash: String,

    /** Usage count for ranking */
    @ColumnInfo(name = "usage_count")
    val usageCount: Long = 0,

    /** Whether user has favorited this emoji */
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    /** Source: "builtin", "imported", "downloaded" */
    @ColumnInfo(name = "source")
    val source: String = "imported",

    /** Source app/package name (e.g., "com.whatsapp") */
    @ColumnInfo(name = "source_app")
    val sourceApp: String? = null,

    /** AI-assigned category (for future AI classification) */
    @ColumnInfo(name = "ai_category")
    val aiCategory: String? = null,

    /** AI confidence score (0.0-1.0) */
    @ColumnInfo(name = "ai_confidence")
    val aiConfidence: Float = 0f,

    /** Cloud sync ID for future multi-device sync */
    @ColumnInfo(name = "sync_id")
    val syncId: String? = null,

    /** Last sync timestamp */
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long = 0,

    /** Custom sort order within category */
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    /** Whether this record needs to be synced */
    @ColumnInfo(name = "is_dirty")
    val isDirty: Boolean = false,

    /** Soft delete flag */
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    /** Creation timestamp */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** Last update timestamp */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
