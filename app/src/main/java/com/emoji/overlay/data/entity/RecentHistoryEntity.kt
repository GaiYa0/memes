package com.emoji.overlay.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Tracks recent emoji usage for the "Recently Used" section.
 *
 * Uses composite primary key to prevent duplicate entries.
 * The [usedAt] timestamp enables sorting by recency.
 */
@Entity(
    tableName = "recent_history",
    primaryKeys = ["emoji_id", "used_at"],
    indices = [
        Index(value = ["emoji_id"]),
        Index(value = ["used_at"], orders = [Index.Order.DESC]),
        // For querying recent emojis within a time window
        Index(value = ["used_at", "emoji_id"])
    ]
)
data class RecentHistoryEntity(
    @ColumnInfo(name = "emoji_id")
    val emojiId: Long,

    /** Timestamp when the emoji was used */
    @ColumnInfo(name = "used_at")
    val usedAt: Long = System.currentTimeMillis(),

    /** Context where it was used (e.g., app package name) */
    @ColumnInfo(name = "context")
    val context: String? = null
)
