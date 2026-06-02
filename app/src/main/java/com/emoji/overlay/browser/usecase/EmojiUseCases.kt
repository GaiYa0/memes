package com.emoji.overlay.browser.usecase

import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.TagEntity
import com.emoji.overlay.data.repository.EmojiRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for category operations.
 */
class EmojiCategoryUseCase(private val repository: EmojiRepository) {
    fun getAllCategories(): Flow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
    fun getRootCategories(): Flow<List<CategoryEntity>> = repository.getRootCategoriesFlow()
    fun getSubCategories(parentId: Long): Flow<List<CategoryEntity>> = repository.getSubCategoriesFlow(parentId)
    fun getEmojisByCategoryFlow(categoryId: Long): Flow<List<EmojiEntity>> = repository.getEmojisByCategoryFlow(categoryId)
    suspend fun getEmojisByCategory(categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        repository.getEmojisByCategory(categoryId, limit, offset)
    suspend fun getCategoryById(id: Long): CategoryEntity? = repository.getCategoryById(id)
}

/**
 * Use case for recent usage operations.
 */
class RecentUseCase(private val repository: EmojiRepository) {
    fun getRecentEmojisFlow(limit: Int = 50): Flow<List<EmojiEntity>> = repository.getRecentEmojisFlow(limit)
    suspend fun getRecentEmojis(limit: Int = 50): List<EmojiEntity> = repository.getRecentEmojis(limit)
    suspend fun recordUsage(emojiId: Long) = repository.recordUsage(emojiId)
    suspend fun clearHistory() = repository.clearHistory()
}

/**
 * Use case for favorite operations.
 */
class FavoriteUseCase(private val repository: EmojiRepository) {
    fun getFavoritesFlow(): Flow<List<EmojiEntity>> = repository.getFavoritesFlow()
    suspend fun getFavorites(limit: Int = 50, offset: Int = 0): List<EmojiEntity> = repository.getFavorites(limit, offset)
    suspend fun toggleFavorite(emojiId: Long) = repository.toggleFavorite(emojiId)
    suspend fun addToFavorites(emojiIds: List<Long>) = repository.addToFavorites(emojiIds)
    suspend fun removeFromFavorites(emojiIds: List<Long>) = repository.removeFromFavorites(emojiIds)
    suspend fun isFavorite(emojiId: Long): Boolean = repository.isFavorite(emojiId)
}

/**
 * Use case for search operations.
 */
class SearchUseCase(private val repository: EmojiRepository) {
    fun searchFlow(query: String): Flow<List<EmojiEntity>> = repository.searchEmojisFlow(query)
    suspend fun search(query: String, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        repository.searchEmojis(query, limit, offset)
    suspend fun searchInCategory(query: String, categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        repository.searchInCategory(query, categoryId, limit, offset)
    suspend fun searchByTag(tagId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        repository.getEmojisByTag(tagId, limit, offset)
    suspend fun getAllTags(): List<TagEntity> = repository.getAllTags()
    suspend fun searchTags(query: String): List<TagEntity> = repository.searchTags(query)
}
