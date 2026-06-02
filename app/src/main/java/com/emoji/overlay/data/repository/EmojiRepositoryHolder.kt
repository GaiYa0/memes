package com.emoji.overlay.data.repository

import android.content.Context
import com.emoji.overlay.data.database.EmojiDatabase
import com.emoji.overlay.data.util.ResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Singleton holder for [EmojiRepository].
 *
 * Ensures:
 * - Single database instance
 * - Single ResourceManager instance
 * - Repository initialized on first access (lazy)
 * - Thread-safe initialization
 *
 * This prevents:
 * - Multiple Room database instances (can cause "database already closed" crashes)
 * - Multiple ResourceManager instances (duplicate I/O for directory creation)
 * - Main thread I/O during Compose composition
 */
object EmojiRepositoryHolder {

    @Volatile
    private var repository: EmojiRepository? = null

    /**
     * Get the singleton repository instance.
     * First call initializes the database and repository (should be from background thread).
     */
    fun getRepository(context: Context): EmojiRepository {
        return repository ?: synchronized(this) {
            repository ?: createRepository(context).also { repository = it }
        }
    }

    private fun createRepository(context: Context): EmojiRepository {
        val appContext = context.applicationContext
        val db = EmojiDatabase.getInstance(appContext)
        val resourceManager = ResourceManager.getInstance(appContext)
        return EmojiRepository(
            emojiDao = db.emojiDao(),
            categoryDao = db.categoryDao(),
            tagDao = db.tagDao(),
            recentHistoryDao = db.recentHistoryDao(),
            resourceManager = resourceManager
        )
    }

    /**
     * Initialize the repository on a background thread.
     * Call this early (e.g., in Application.onCreate or ViewModel init).
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        val repo = getRepository(context)
        // Ensure directories exist
        repo.getFile("").parentFile?.mkdirs()
    }

    /**
     * Check if repository is initialized.
     */
    fun isInitialized(): Boolean = repository != null
}
