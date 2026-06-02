package com.emoji.overlay.data.usecase

import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.TagEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.repository.VaultStats
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Use cases for emoji operations.
 * These encapsulate business logic and orchestrate repository calls.
 */
class EmojiUseCases(private val repository: EmojiRepository) {

    // ==================== QUERY USE CASES ====================

    suspend fun getRecentEmojis(limit: Int = 50): List<EmojiEntity> {
        return repository.getRecentEmojis(limit)
    }

    fun getRecentEmojisFlow(limit: Int = 50): Flow<List<EmojiEntity>> {
        return repository.getRecentEmojisFlow(limit)
    }

    suspend fun getFavoriteEmojis(limit: Int = 50, offset: Int = 0): List<EmojiEntity> {
        return repository.getFavorites(limit, offset)
    }

    fun getFavoriteEmojisFlow(): Flow<List<EmojiEntity>> {
        return repository.getFavoritesFlow()
    }

    suspend fun searchEmojis(query: String, limit: Int = 50, offset: Int = 0): List<EmojiEntity> {
        if (query.isBlank()) return emptyList()
        return repository.searchEmojis(query.trim(), limit, offset)
    }

    fun searchEmojisFlow(query: String): Flow<List<EmojiEntity>> {
        return repository.searchEmojisFlow(query.trim())
    }

    suspend fun getEmojisByCategory(
        categoryId: Long,
        limit: Int = 50,
        offset: Int = 0
    ): List<EmojiEntity> {
        return repository.getEmojisByCategory(categoryId, limit, offset)
    }

    suspend fun getEmojisByTag(tagId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> {
        return repository.getEmojisByTag(tagId, limit, offset)
    }

    // ==================== ACTION USE CASES ====================

    /**
     * Select an emoji (record usage and return the file).
     */
    suspend fun selectEmoji(emojiId: Long): File? {
        val emoji = repository.getEmojiById(emojiId) ?: return null
        repository.recordUsage(emojiId)
        return repository.getFile(emoji.filePath)
    }

    suspend fun toggleFavorite(emojiId: Long): Boolean {
        repository.toggleFavorite(emojiId)
        return repository.isFavorite(emojiId)
    }

    suspend fun importEmoji(
        file: File,
        name: String,
        keywords: String = "",
        categoryId: Long? = null,
        tags: List<String> = emptyList()
    ): Result<Long> {
        return repository.importEmoji(file, name, keywords, categoryId, tags)
    }

    suspend fun importBatch(
        files: List<File>,
        categoryId: Long? = null
    ): com.emoji.overlay.data.repository.ImportBatchResult {
        return repository.importBatch(files, defaultCategoryId = categoryId)
    }

    suspend fun deleteEmojis(emojiIds: List<Long>) {
        repository.deleteBatch(emojiIds)
    }

    suspend fun setEmojiTags(emojiId: Long, tagNames: List<String>) {
        repository.setTagsForEmoji(emojiId, tagNames)
    }

    suspend fun addEmojiTag(emojiId: Long, tagName: String) {
        repository.addTagsToEmoji(emojiId, listOf(tagName))
    }

    suspend fun removeEmojiTag(emojiId: Long, tagId: Long) {
        repository.removeTagFromEmoji(emojiId, tagId)
    }

    suspend fun getEmojiTags(emojiId: Long): List<TagEntity> {
        return repository.getTagsForEmoji(emojiId)
    }

    // ==================== MAINTENANCE USE CASES ====================

    suspend fun repairDatabase(): Int {
        return repository.repairMissingFiles()
    }

    suspend fun getVaultStats(): VaultStats {
        return repository.getVaultStats()
    }

    suspend fun clearHistory() {
        repository.clearHistory()
    }
}
