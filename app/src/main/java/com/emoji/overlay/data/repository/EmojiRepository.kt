package com.emoji.overlay.data.repository

import android.util.Log
import com.emoji.overlay.data.dao.CategoryDao
import com.emoji.overlay.data.dao.EmojiDao
import com.emoji.overlay.data.dao.RecentHistoryDao
import com.emoji.overlay.data.dao.TagDao
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.EmojiTagCrossRef
import com.emoji.overlay.data.entity.RecentHistoryEntity
import com.emoji.overlay.data.entity.TagEntity
import com.emoji.overlay.data.util.ImportResult
import com.emoji.overlay.data.util.ResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository encapsulating all emoji data operations.
 *
 * This is the single source of truth for emoji data.
 * All business logic should go through this repository
 * instead of accessing DAOs directly.
 */
class EmojiRepository(
    private val emojiDao: EmojiDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val recentHistoryDao: RecentHistoryDao,
    private val resourceManager: ResourceManager
) {
    companion object {
        private const val TAG = "EmojiRepository"
        private const val MAX_RECENT_HISTORY = 1000
    }

    // ==================== EMOJI OPERATIONS ====================

    suspend fun getEmojiById(id: Long): EmojiEntity? = emojiDao.getById(id)

    suspend fun getEmojiByHash(hash: String): EmojiEntity? = emojiDao.getByContentHash(hash)

    fun getAllEmojisFlow(): Flow<List<EmojiEntity>> = emojiDao.getAllFlow()

    suspend fun getAllEmojis(limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiDao.getAll(limit, offset)

    suspend fun getEmojisByCategory(categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiDao.getByCategory(categoryId, limit, offset)

    fun getEmojisByCategoryFlow(categoryId: Long): Flow<List<EmojiEntity>> =
        emojiDao.getByCategoryFlow(categoryId)

    suspend fun getFavorites(limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiDao.getFavorites(limit, offset)

    fun getFavoritesFlow(): Flow<List<EmojiEntity>> = emojiDao.getFavoritesFlow()

    suspend fun searchEmojis(query: String, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiDao.search(query, limit, offset)

    fun searchEmojisFlow(query: String): Flow<List<EmojiEntity>> = emojiDao.searchFlow(query)

    suspend fun searchInCategory(query: String, categoryId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiDao.searchInCategory(query, categoryId, limit, offset)

    suspend fun getEmojisByTag(tagId: Long, limit: Int = 50, offset: Int = 0): List<EmojiEntity> =
        emojiDao.getByTag(tagId, limit, offset)

    fun getEmojisByTagFlow(tagId: Long): Flow<List<EmojiEntity>> = emojiDao.getByTagFlow(tagId)

    suspend fun getRecentEmojis(limit: Int = 50): List<EmojiEntity> = emojiDao.getRecent(limit)

    fun getRecentEmojisFlow(limit: Int = 50): Flow<List<EmojiEntity>> = emojiDao.getRecentFlow(limit)

    suspend fun getEmojiCount(): Int = emojiDao.getCount()

    fun getEmojiCountFlow(): Flow<Int> = emojiDao.getCountFlow()

    // ==================== IMPORT ====================

    /**
     * Import a single emoji file.
     * Handles deduplication, file storage, and database insertion.
     */
    suspend fun importEmoji(
        file: File,
        name: String,
        keywords: String = "",
        categoryId: Long? = null,
        tags: List<String> = emptyList()
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val mimeType = resourceManager.detectMimeType(file)

            // Check for duplicate by content hash
            val hash = resourceManager.calculateContentHash(file)
            val existing = emojiDao.getByContentHash(hash)
            if (existing != null) {
                return@withContext Result.failure(
                    DuplicateEmojiException(existing.id, existing.name)
                )
            }

            // Import file to vault
            val importResult = resourceManager.importFile(file, mimeType)
            if (importResult is ImportResult.Error) {
                return@withContext Result.failure(IOException(importResult.message))
            }
            val success = importResult as ImportResult.Success

            // Create database record
            val emoji = EmojiEntity(
                name = name,
                keywords = keywords,
                categoryId = categoryId,
                filePath = success.relativePath,
                thumbPath = success.thumbPath,
                mimeType = mimeType,
                fileSize = success.fileSize,
                contentHash = success.contentHash,
                source = "imported"
            )
            val id = emojiDao.insert(emoji)

            // Process tags
            if (tags.isNotEmpty()) {
                addTagsToEmoji(id, tags)
            }

            // Update category count
            categoryId?.let { categoryDao.updateEmojiCount(it) }

            Log.d(TAG, "Imported emoji: $name (id=$id)")
            Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            Result.failure(e)
        }
    }

    /**
     * Batch import multiple emoji files.
     */
    suspend fun importBatch(
        files: List<File>,
        defaultName: (File) -> String = { it.nameWithoutExtension },
        defaultCategoryId: Long? = null
    ): ImportBatchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        val duplicates = mutableListOf<String>()

        files.forEach { file ->
            val name = defaultName(file)
            val result = importEmoji(file, name, categoryId = defaultCategoryId)
            result.fold(
                onSuccess = { results.add(it) },
                onFailure = { e ->
                    when (e) {
                        is DuplicateEmojiException -> duplicates.add(name)
                        else -> errors.add("$name: ${e.message}")
                    }
                }
            )
        }

        ImportBatchResult(
            imported = results.size,
            duplicates = duplicates.size,
            errors = errors,
            importedIds = results
        )
    }

    /**
     * Delete an emoji and its associated files.
     */
    suspend fun deleteEmoji(emojiId: Long) = withContext(Dispatchers.IO) {
        val emoji = emojiDao.getById(emojiId) ?: return@withContext
        resourceManager.deleteFileWithThumb(emoji.filePath, emoji.thumbPath)
        tagDao.removeAllTagsForEmoji(emojiId)
        recentHistoryDao.deleteByEmojiId(emojiId)
        emojiDao.deleteById(emojiId)
        emoji.categoryId?.let { categoryDao.updateEmojiCount(it) }
        Log.d(TAG, "Deleted emoji: ${emoji.name} (id=$emojiId)")
    }

    /**
     * Batch delete emojis.
     */
    suspend fun deleteBatch(emojiIds: List<Long>) = withContext(Dispatchers.IO) {
        emojiIds.forEach { deleteEmoji(it) }
    }

    /**
     * Soft delete emojis (mark as deleted, keep files for recovery).
     */
    suspend fun softDelete(emojiIds: List<Long>) {
        emojiDao.softDelete(emojiIds)
    }

    // ==================== FAVORITES ====================

    suspend fun toggleFavorite(emojiId: Long) {
        val emoji = emojiDao.getById(emojiId) ?: return
        emojiDao.setFavorite(emojiId, !emoji.isFavorite)
    }

    suspend fun addToFavorites(emojiIds: List<Long>) {
        emojiDao.setFavorites(emojiIds)
    }

    suspend fun removeFromFavorites(emojiIds: List<Long>) {
        emojiDao.removeFavorites(emojiIds)
    }

    suspend fun isFavorite(emojiId: Long): Boolean {
        return emojiDao.getById(emojiId)?.isFavorite ?: false
    }

    // ==================== RECENT HISTORY ====================

    suspend fun recordUsage(emojiId: Long, context: String? = null) {
        recentHistoryDao.insert(RecentHistoryEntity(emojiId = emojiId, context = context))
        emojiDao.incrementUsage(emojiId)
        // Prune old records periodically
        recentHistoryDao.pruneOldRecords(MAX_RECENT_HISTORY)
    }

    suspend fun clearHistory() {
        recentHistoryDao.deleteAll()
    }

    // ==================== CATEGORIES ====================

    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getAllFlow()

    fun getRootCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getRootCategoriesFlow()

    fun getSubCategoriesFlow(parentId: Long): Flow<List<CategoryEntity>> =
        categoryDao.getSubCategoriesFlow(parentId)

    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun createCategory(name: String, icon: String = "📁", parentId: Long? = null): Long {
        val category = CategoryEntity(name = name, icon = icon, parentId = parentId)
        return categoryDao.insert(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(categoryId: Long) {
        // Move emojis to uncategorized
        val emojis = emojiDao.getByCategory(categoryId, limit = Int.MAX_VALUE)
        emojiDao.moveToCategory(emojis.map { it.id }, null)
        categoryDao.deleteById(categoryId)
    }

    // ==================== TAGS ====================

    fun getAllTagsFlow(): Flow<List<TagEntity>> = tagDao.getAllFlow()

    suspend fun getAllTags(): List<TagEntity> = tagDao.getAll()

    suspend fun searchTags(query: String): List<TagEntity> = tagDao.search(query)

    suspend fun getTagsForEmoji(emojiId: Long): List<TagEntity> = tagDao.getTagsForEmoji(emojiId)

    fun getTagsForEmojiFlow(emojiId: Long): Flow<List<TagEntity>> = tagDao.getTagsForEmojiFlow(emojiId)

    suspend fun addTagsToEmoji(emojiId: Long, tagNames: List<String>) {
        tagNames.forEach { tagName ->
            val normalized = tagName.trim().lowercase()
            if (normalized.isEmpty()) return@forEach

            var tag = tagDao.getByName(normalized)
            if (tag == null) {
                val id = tagDao.insert(TagEntity(name = normalized, displayName = tagName.trim()))
                tag = tagDao.getById(id)
            }
            tag?.let {
                tagDao.insertCrossRef(EmojiTagCrossRef(emojiId = emojiId, tagId = it.id))
                tagDao.updateUsageCount(it.id)
            }
        }
    }

    suspend fun removeTagFromEmoji(emojiId: Long, tagId: Long) {
        tagDao.removeCrossRef(emojiId, tagId)
        tagDao.updateUsageCount(tagId)
    }

    suspend fun setTagsForEmoji(emojiId: Long, tagNames: List<String>) {
        tagDao.removeAllTagsForEmoji(emojiId)
        addTagsToEmoji(emojiId, tagNames)
    }

    // ==================== SYNC & MAINTENANCE ====================

    /**
     * Verify database-file system consistency.
     * Returns list of emoji IDs with missing files.
     */
    suspend fun verifyConsistency(): List<Long> = withContext(Dispatchers.IO) {
        val allEmojis = emojiDao.getAll(limit = Int.MAX_VALUE)
        val missingFiles = mutableListOf<Long>()

        allEmojis.forEach { emoji ->
            if (!resourceManager.fileExists(emoji.filePath)) {
                missingFiles.add(emoji.id)
                Log.w(TAG, "Missing file for emoji ${emoji.id}: ${emoji.filePath}")
            }
        }

        // Check for orphaned files
        val allPaths = allEmojis.map { it.filePath }.toSet()
        resourceManager.cleanupOrphanedFiles(allPaths)

        missingFiles
    }

    /**
     * Repair emojis with missing files (soft delete them).
     */
    suspend fun repairMissingFiles(): Int {
        val missingIds = verifyConsistency()
        if (missingIds.isNotEmpty()) {
            emojiDao.softDelete(missingIds)
            Log.w(TAG, "Soft-deleted ${missingIds.size} emojis with missing files")
        }
        return missingIds.size
    }

    /**
     * Get dirty records for sync.
     */
    suspend fun getDirtyRecords(): List<EmojiEntity> = emojiDao.getDirtyRecords()

    /**
     * Mark records as synced.
     */
    suspend fun markSynced(emojiIds: List<Long>) {
        emojiDao.markSynced(emojiIds)
    }

    /**
     * Get vault statistics.
     */
    suspend fun getVaultStats(): VaultStats {
        return VaultStats(
            totalEmojis = emojiDao.getCount(),
            favoriteCount = emojiDao.getFavoriteCount(),
            categoryCount = categoryDao.getCount(),
            tagCount = tagDao.getCount(),
            vaultSizeBytes = resourceManager.getVaultSize()
        )
    }

    /**
     * Get the absolute file for an emoji.
     */
    fun getFile(filePath: String): File {
        return resourceManager.getFile(filePath)
    }
}

// ==================== DATA CLASSES ====================

data class ImportBatchResult(
    val imported: Int,
    val duplicates: Int,
    val errors: List<String>,
    val importedIds: List<Long>
)

data class VaultStats(
    val totalEmojis: Int,
    val favoriteCount: Int,
    val categoryCount: Int,
    val tagCount: Int,
    val vaultSizeBytes: Long
)

class DuplicateEmojiException(val existingId: Long, val existingName: String) :
    Exception("Duplicate emoji found: $existingName (id=$existingId)")

class IOException(message: String) : Exception(message)
