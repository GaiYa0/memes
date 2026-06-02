package com.emoji.overlay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emoji.overlay.data.entity.RecentHistoryEntity

/**
 * Data Access Object for recent history operations.
 *
 * History records are time-series data. Old records are pruned
 * to keep the table size manageable.
 */
@Dao
interface RecentHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: RecentHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<RecentHistoryEntity>)

    @Query("DELETE FROM recent_history WHERE emoji_id = :emojiId")
    suspend fun deleteByEmojiId(emojiId: Long)

    @Query("DELETE FROM recent_history WHERE emoji_id IN (:emojiIds)")
    suspend fun deleteByEmojiIds(emojiIds: List<Long>)

    @Query("DELETE FROM recent_history")
    suspend fun deleteAll()

    @Query("SELECT * FROM recent_history ORDER BY used_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<RecentHistoryEntity>

    @Query("SELECT DISTINCT emoji_id FROM recent_history ORDER BY used_at DESC LIMIT :limit")
    suspend fun getRecentEmojiIds(limit: Int = 50): List<Long>

    /**
     * Prune old history records, keeping only the most recent [keepCount].
     * This prevents unbounded table growth.
     */
    @Query("""
        DELETE FROM recent_history
        WHERE emoji_id NOT IN (
            SELECT emoji_id FROM recent_history
            GROUP BY emoji_id
            ORDER BY MAX(used_at) DESC
            LIMIT :keepCount
        )
    """)
    suspend fun pruneOldRecords(keepCount: Int = 1000)

    /**
     * Prune records older than [cutoffTime].
     */
    @Query("DELETE FROM recent_history WHERE used_at < :cutoffTime")
    suspend fun pruneBefore(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM recent_history")
    suspend fun getCount(): Int
}
