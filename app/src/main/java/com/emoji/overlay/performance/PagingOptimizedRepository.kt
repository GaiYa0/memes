package com.emoji.overlay.performance

import com.emoji.overlay.data.dao.EmojiDao
import com.emoji.overlay.data.entity.EmojiEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Paging-optimized repository for large datasets.
 *
 * Uses cursor-based pagination for efficient database access.
 * Avoids OFFSET-based pagination which becomes slow on large datasets.
 */
class PagingOptimizedRepository(private val emojiDao: EmojiDao) {

    companion object {
        private const val PAGE_SIZE = 50
        private const val PREFETCH_DISTANCE = 10
    }

    /**
     * Load emojis with cursor-based pagination.
     * More efficient than OFFSET for large datasets.
     */
    fun loadEmojisPaged(
        categoryId: Long? = null,
        pageSize: Int = PAGE_SIZE
    ): Flow<PagingResult<EmojiEntity>> = flow {
        var lastId = 0L
        var hasMore = true

        while (hasMore) {
            val items = withContext(Dispatchers.IO) {
                if (categoryId != null) {
                    emojiDao.getByCategoryPaged(categoryId, lastId, pageSize)
                } else {
                    emojiDao.getAllPaged(lastId, pageSize)
                }
            }

            if (items.isEmpty()) {
                hasMore = false
            } else {
                lastId = items.last().id
                emit(PagingResult(items, hasMore = items.size == pageSize))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Load favorites with pagination.
     */
    fun loadFavoritesPaged(pageSize: Int = PAGE_SIZE): Flow<PagingResult<EmojiEntity>> = flow {
        var lastId = 0L
        var hasMore = true

        while (hasMore) {
            val items = withContext(Dispatchers.IO) {
                emojiDao.getFavoritesPaged(lastId, pageSize)
            }

            if (items.isEmpty()) {
                hasMore = false
            } else {
                lastId = items.last().id
                emit(PagingResult(items, hasMore = items.size == pageSize))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Load recent emojis with pagination.
     */
    fun loadRecentPaged(pageSize: Int = PAGE_SIZE): Flow<PagingResult<EmojiEntity>> = flow {
        var offset = 0
        var hasMore = true

        while (hasMore) {
            val items = withContext(Dispatchers.IO) {
                emojiDao.getRecentPaged(offset, pageSize)
            }

            if (items.isEmpty()) {
                hasMore = false
            } else {
                offset += items.size
                emit(PagingResult(items, hasMore = items.size == pageSize))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Search with pagination.
     */
    fun searchPaged(
        query: String,
        pageSize: Int = PAGE_SIZE
    ): Flow<PagingResult<EmojiEntity>> = flow {
        var offset = 0
        var hasMore = true

        while (hasMore) {
            val items = withContext(Dispatchers.IO) {
                emojiDao.searchPaged(query, offset, pageSize)
            }

            if (items.isEmpty()) {
                hasMore = false
            } else {
                offset += items.size
                emit(PagingResult(items, hasMore = items.size == pageSize))
            }
        }
    }.flowOn(Dispatchers.IO)
}

data class PagingResult<T>(
    val items: List<T>,
    val hasMore: Boolean
)
