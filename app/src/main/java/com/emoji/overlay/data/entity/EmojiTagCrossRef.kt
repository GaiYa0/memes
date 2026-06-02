package com.emoji.overlay.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Many-to-many relationship between emojis and tags.
 *
 * Uses composite primary key (emoji_id, tag_id).
 * Additional index on tag_id for reverse lookups.
 */
@Entity(
    tableName = "emoji_tag_cross_ref",
    primaryKeys = ["emoji_id", "tag_id"],
    indices = [
        Index(value = ["tag_id"]),
        Index(value = ["emoji_id"])
    ]
)
data class EmojiTagCrossRef(
    @ColumnInfo(name = "emoji_id")
    val emojiId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
