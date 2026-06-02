package com.emoji.overlay.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emoji.overlay.data.entity.EmojiTagCrossRef
import com.emoji.overlay.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for tag operations.
 */
@Dao
interface TagDao {

    // ==================== TAG CRUD ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>): List<Long>

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteById(tagId: Long)

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getById(tagId: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY usage_count DESC, name ASC")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY usage_count DESC, name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' OR display_name LIKE '%' || :query || '%' ORDER BY usage_count DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 20): List<TagEntity>

    // ==================== CROSS REF ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: EmojiTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<EmojiTagCrossRef>)

    @Query("DELETE FROM emoji_tag_cross_ref WHERE emoji_id = :emojiId AND tag_id = :tagId")
    suspend fun removeCrossRef(emojiId: Long, tagId: Long)

    @Query("DELETE FROM emoji_tag_cross_ref WHERE emoji_id = :emojiId")
    suspend fun removeAllTagsForEmoji(emojiId: Long)

    @Query("DELETE FROM emoji_tag_cross_ref WHERE tag_id = :tagId")
    suspend fun removeAllEmojisForTag(tagId: Long)

    @Query("SELECT tag_id FROM emoji_tag_cross_ref WHERE emoji_id = :emojiId")
    suspend fun getTagIdsForEmoji(emojiId: Long): List<Long>

    @Query("SELECT * FROM tags WHERE id IN (SELECT tag_id FROM emoji_tag_cross_ref WHERE emoji_id = :emojiId)")
    suspend fun getTagsForEmoji(emojiId: Long): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id IN (SELECT tag_id FROM emoji_tag_cross_ref WHERE emoji_id = :emojiId)")
    fun getTagsForEmojiFlow(emojiId: Long): Flow<List<TagEntity>>

    @Query("UPDATE tags SET usage_count = (SELECT COUNT(*) FROM emoji_tag_cross_ref WHERE tag_id = :tagId) WHERE id = :tagId")
    suspend fun updateUsageCount(tagId: Long)

    @Query("UPDATE tags SET usage_count = (SELECT COUNT(*) FROM emoji_tag_cross_ref WHERE tag_id = tags.id)")
    suspend fun updateAllUsageCounts()

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getCount(): Int
}
