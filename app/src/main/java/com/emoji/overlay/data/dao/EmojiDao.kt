package com.emoji.overlay.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.emoji.overlay.data.entity.EmojiEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for emoji operations.
 *
 * Optimized queries for 100k+ records:
 * - Uses indexed columns for filtering
 * - Pagination support via LIMIT/OFFSET
 * - Flow-based reactive queries for UI
 */
@Dao
interface EmojiDao {

    // ==================== INSERT ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emoji: EmojiEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emojis: List<EmojiEntity>): List<Long>

    @Transaction
    suspend fun insertBatch(emojis: List<EmojiEntity>, batchSize: Int = 500): List<Long> {
        val results = mutableListOf<Long>()
        emojis.chunked(batchSize).forEach { batch ->
            results.addAll(insertAll(batch))
        }
        return results
    }

    // ==================== UPDATE ====================

    @Update
    suspend fun update(emoji: EmojiEntity)

    @Update
    suspend fun updateAll(emojis: List<EmojiEntity>)

    @Query("UPDATE emojis SET usage_count = usage_count + 1 WHERE id = :emojiId")
    suspend fun incrementUsage(emojiId: Long)

    @Query("UPDATE emojis SET is_favorite = :isFavorite, updated_at = :now WHERE id = :emojiId")
    suspend fun setFavorite(emojiId: Long, isFavorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE emojis SET is_favorite = 1, updated_at = :now WHERE id IN (:emojiIds)")
    suspend fun setFavorites(emojiIds: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE emojis SET is_favorite = 0, updated_at = :now WHERE id IN (:emojiIds)")
    suspend fun removeFavorites(emojiIds: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE emojis SET category_id = :categoryId, updated_at = :now WHERE id IN (:emojiIds)")
    suspend fun moveToCategory(emojiIds: List<Long>, categoryId: Long?, now: Long = System.currentTimeMillis())

    // ==================== DELETE ====================

    @Delete
    suspend fun delete(emoji: EmojiEntity)

    @Query("DELETE FROM emojis WHERE id = :emojiId")
    suspend fun deleteById(emojiId: Long)

    @Query("DELETE FROM emojis WHERE id IN (:emojiIds)")
    suspend fun deleteByIds(emojiIds: List<Long>)

    @Query("UPDATE emojis SET is_deleted = 1, updated_at = :now WHERE id IN (:emojiIds)")
    suspend fun softDelete(emojiIds: List<Long>, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM emojis WHERE is_deleted = 1")
    suspend fun purgeDeleted()

    // ==================== QUERY: SINGLE ====================

    @Query("SELECT * FROM emojis WHERE id = :emojiId AND is_deleted = 0")
    suspend fun getById(emojiId: Long): EmojiEntity?

    @Query("SELECT * FROM emojis WHERE content_hash = :hash AND is_deleted = 0 LIMIT 1")
    suspend fun getByContentHash(hash: String): EmojiEntity?

    @Query("SELECT * FROM emojis WHERE file_path = :path AND is_deleted = 0 LIMIT 1")
    suspend fun getByFilePath(path: String): EmojiEntity?

    // ==================== QUERY: LISTS ====================

    @Query("SELECT * FROM emojis WHERE is_deleted = 0 ORDER BY updated_at DESC")
    fun getAllFlow(): Flow<List<EmojiEntity>>

    @Query("SELECT * FROM emojis WHERE is_deleted = 0 ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int = 50, offset: Int = 0): List<EmojiEntity>

    @Query("SELECT * FROM emojis WHERE category_id = :categoryId AND is_deleted = 0 ORDER BY sort_order, name LIMIT :limit OFFSET :offset")
    suspend fun getByCategory(categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity>

    @Query("SELECT * FROM emojis WHERE category_id = :categoryId AND is_deleted = 0 ORDER BY sort_order, name")
    fun getByCategoryFlow(categoryId: Long): Flow<List<EmojiEntity>>

    @Query("SELECT * FROM emojis WHERE is_favorite = 1 AND is_deleted = 0 ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getFavorites(limit: Int = 50, offset: Int = 0): List<EmojiEntity>

    @Query("SELECT * FROM emojis WHERE is_favorite = 1 AND is_deleted = 0 ORDER BY updated_at DESC")
    fun getFavoritesFlow(): Flow<List<EmojiEntity>>

    // ==================== QUERY: SEARCH ====================

    @Query("""
        SELECT * FROM emojis
        WHERE is_deleted = 0
        AND (name LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%')
        ORDER BY usage_count DESC, name ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun search(query: String, limit: Int = 50, offset: Int = 0): List<EmojiEntity>

    @Query("""
        SELECT * FROM emojis
        WHERE is_deleted = 0
        AND (name LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%')
        ORDER BY usage_count DESC, name ASC
    """)
    fun searchFlow(query: String): Flow<List<EmojiEntity>>

    @Query("""
        SELECT * FROM emojis
        WHERE is_deleted = 0
        AND category_id = :categoryId
        AND (name LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%')
        ORDER BY usage_count DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchInCategory(query: String, categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity>

    // ==================== QUERY: BY TAG ====================

    @Query("""
        SELECT e.* FROM emojis e
        INNER JOIN emoji_tag_cross_ref ref ON e.id = ref.emoji_id
        WHERE ref.tag_id = :tagId AND e.is_deleted = 0
        ORDER BY e.usage_count DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTag(tagId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity>

    @Query("""
        SELECT e.* FROM emojis e
        INNER JOIN emoji_tag_cross_ref ref ON e.id = ref.emoji_id
        WHERE ref.tag_id = :tagId AND e.is_deleted = 0
        ORDER BY e.usage_count DESC
    """)
    fun getByTagFlow(tagId: Long): Flow<List<EmojiEntity>>

    // ==================== QUERY: RECENT ====================

    @Query("""
        SELECT e.* FROM emojis e
        INNER JOIN recent_history rh ON e.id = rh.emoji_id
        WHERE e.is_deleted = 0
        GROUP BY e.id
        ORDER BY MAX(rh.used_at) DESC
        LIMIT :limit
    """)
    suspend fun getRecent(limit: Int = 50): List<EmojiEntity>

    @Query("""
        SELECT e.* FROM emojis e
        INNER JOIN recent_history rh ON e.id = rh.emoji_id
        WHERE e.is_deleted = 0
        GROUP BY e.id
        ORDER BY MAX(rh.used_at) DESC
        LIMIT :limit
    """)
    fun getRecentFlow(limit: Int = 50): Flow<List<EmojiEntity>>

    // ==================== QUERY: COUNTS ====================

    @Query("SELECT COUNT(*) FROM emojis WHERE is_deleted = 0")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM emojis WHERE is_deleted = 0")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM emojis WHERE category_id = :categoryId AND is_deleted = 0")
    suspend fun getCountByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM emojis WHERE is_favorite = 1 AND is_deleted = 0")
    suspend fun getFavoriteCount(): Int

    // ==================== QUERY: SYNC ====================

    @Query("SELECT * FROM emojis WHERE is_dirty = 1 AND is_deleted = 0")
    suspend fun getDirtyRecords(): List<EmojiEntity>

    @Query("UPDATE emojis SET is_dirty = 0, synced_at = :now WHERE id IN (:emojiIds)")
    suspend fun markSynced(emojiIds: List<Long>, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM emojis WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): EmojiEntity?

    // ==================== CURSOR-BASED PAGING ====================

    @Query("SELECT * FROM emojis WHERE id > :lastId AND is_deleted = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun getAllPaged(lastId: Long, limit: Int = 50): List<EmojiEntity>

    @Query("SELECT * FROM emojis WHERE category_id = :categoryId AND id > :lastId AND is_deleted = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun getByCategoryPaged(categoryId: Long, lastId: Long, limit: Int = 50): List<EmojiEntity>

    @Query("SELECT * FROM emojis WHERE is_favorite = 1 AND id > :lastId AND is_deleted = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun getFavoritesPaged(lastId: Long, limit: Int = 50): List<EmojiEntity>

    @Query("""
        SELECT e.* FROM emojis e
        INNER JOIN recent_history rh ON e.id = rh.emoji_id
        WHERE e.is_deleted = 0
        GROUP BY e.id
        ORDER BY MAX(rh.used_at) DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getRecentPaged(offset: Int, limit: Int = 50): List<EmojiEntity>

    @Query("""
        SELECT * FROM emojis
        WHERE is_deleted = 0
        AND (name LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%')
        ORDER BY usage_count DESC, name ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchPaged(query: String, offset: Int, limit: Int = 50): List<EmojiEntity>
}
