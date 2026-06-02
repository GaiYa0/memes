package com.emoji.overlay.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tag entity for flexible emoji labeling.
 *
 * Tags are many-to-many with emojis, allowing:
 * - Multiple tags per emoji
 * - Same tag on multiple emojis
 * - Fast tag-based search
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["usage_count"], orders = [Index.Order.DESC]),
        Index(value = ["sync_id"])
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Tag name (lowercase, trimmed) */
    @ColumnInfo(name = "name")
    val name: String,

    /** Display name (preserves original casing) */
    @ColumnInfo(name = "display_name")
    val displayName: String,

    /** How many emojis use this tag (denormalized) */
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    /** Cloud sync ID */
    @ColumnInfo(name = "sync_id")
    val syncId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
