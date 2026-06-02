package com.emoji.overlay.browser.repository

import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.TagEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.repository.VaultStats
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository for emoji browser operations.
 *
 * Wraps EmojiRepository with browser-specific logic:
 * - Paging support
 * - Cache management
 * - Statistics
 */
class EmojiBrowserRepository(
    private val emojiRepository: EmojiRepository
) {
    // ==================== EMOJI OPERATIONS ====================

    fun getAllEmojisFlow(): Flow<List<EmojiEntity>> = emojiRepository.getAllEmojisFlow()

    suspend fun getAllEmojis(limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiRepository.getAllEmojis(limit, offset)

    suspend fun getEmojiById(id: Long): EmojiEntity? = emojiRepository.getEmojiById(id)

    // ==================== CATEGORY OPERATIONS ====================

    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = emojiRepository.getAllCategoriesFlow()

    fun getRootCategoriesFlow(): Flow<List<CategoryEntity>> = emojiRepository.getRootCategoriesFlow()

    fun getSubCategoriesFlow(parentId: Long): Flow<List<CategoryEntity>> =
        emojiRepository.getSubCategoriesFlow(parentId)

    fun getEmojisByCategoryFlow(categoryId: Long): Flow<List<EmojiEntity>> =
        emojiRepository.getEmojisByCategoryFlow(categoryId)

    suspend fun getEmojisByCategory(categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiRepository.getEmojisByCategory(categoryId, limit, offset)

    suspend fun getCategoryById(id: Long): CategoryEntity? = emojiRepository.getCategoryById(id)

    // ==================== FAVORITE OPERATIONS ====================

    fun getFavoritesFlow(): Flow<List<EmojiEntity>> = emojiRepository.getFavoritesFlow()

    suspend fun getFavorites(limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiRepository.getFavorites(limit, offset)

    suspend fun toggleFavorite(emojiId: Long) = emojiRepository.toggleFavorite(emojiId)

    suspend fun addToFavorites(emojiIds: List<Long>) = emojiRepository.addToFavorites(emojiIds)

    suspend fun removeFromFavorites(emojiIds: List<Long>) = emojiRepository.removeFromFavorites(emojiIds)

    suspend fun isFavorite(emojiId: Long): Boolean = emojiRepository.isFavorite(emojiId)

    // ==================== RECENT OPERATIONS ====================

    fun getRecentEmojisFlow(limit: Int = 50): Flow<List<EmojiEntity>> =
        emojiRepository.getRecentEmojisFlow(limit)

    suspend fun getRecentEmojis(limit: Int = 50): List<EmojiEntity> =
        emojiRepository.getRecentEmojis(limit)

    suspend fun recordUsage(emojiId: Long) = emojiRepository.recordUsage(emojiId)

    suspend fun clearHistory() = emojiRepository.clearHistory()

    // ==================== SEARCH OPERATIONS ====================

    fun searchEmojisFlow(query: String): Flow<List<EmojiEntity>> =
        emojiRepository.searchEmojisFlow(query)

    suspend fun searchEmojis(query: String, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiRepository.searchEmojis(query, limit, offset)

    suspend fun searchInCategory(query: String, categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiRepository.searchInCategory(query, categoryId, limit, offset)

    suspend fun getEmojisByTag(tagId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiRepository.getEmojisByTag(tagId, limit, offset)

    // ==================== TAG OPERATIONS ====================

    fun getAllTagsFlow(): Flow<List<TagEntity>> = emojiRepository.getAllTagsFlow()

    suspend fun getAllTags(): List<TagEntity> = emojiRepository.getAllTags()

    suspend fun searchTags(query: String): List<TagEntity> = emojiRepository.searchTags(query)

    suspend fun getTagsForEmoji(emojiId: Long): List<TagEntity> = emojiRepository.getTagsForEmoji(emojiId)

    // ==================== FILE OPERATIONS ====================

    fun getFile(filePath: String): File = emojiRepository.getFile(filePath)

    fun fileExists(filePath: String): Boolean = emojiRepository.getFile(filePath).exists()

    // ==================== STATISTICS ====================

    suspend fun getVaultStats(): VaultStats = emojiRepository.getVaultStats()

    suspend fun getEmojiCount(): Int = emojiRepository.getEmojiCount()

    fun getEmojiCountFlow(): Flow<Int> = emojiRepository.getEmojiCountFlow()

    // ==================== MAINTENANCE ====================

    suspend fun repairMissingFiles(): Int = emojiRepository.repairMissingFiles()

    suspend fun verifyConsistency(): List<Long> = emojiRepository.verifyConsistency()
}
