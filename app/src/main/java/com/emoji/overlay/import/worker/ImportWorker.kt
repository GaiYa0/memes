package com.emoji.overlay.import.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.emoji.overlay.data.database.EmojiDatabase
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.import.manager.ImportRepository
import com.emoji.overlay.import.model.ImportPreviewItem
import com.emoji.overlay.import.util.DuplicateDetector
import com.emoji.overlay.import.util.FileValidator
import com.emoji.overlay.import.util.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker for importing emoji files.
 *
 * Uses WorkManager for reliable background execution with:
 * - Battery optimization
 * - Retry logic
 * - Progress reporting
 */
class ImportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ImportWorker"
        const val WORK_NAME = "emoji_import"
        const val KEY_ITEM_IDS = "item_ids"
        const val KEY_ITEM_URIS = "item_uris"
        const val KEY_ITEM_NAMES = "item_names"
        const val KEY_ITEM_MIME_TYPES = "item_mime_types"
        const val KEY_ITEM_SIZES = "item_sizes"
        const val KEY_ITEM_HASHES = "item_hashes"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ids = inputData.getStringArray(KEY_ITEM_IDS) ?: return@withContext Result.failure()
            val uris = inputData.getStringArray(KEY_ITEM_URIS) ?: return@withContext Result.failure()
            val names = inputData.getStringArray(KEY_ITEM_NAMES) ?: return@withContext Result.failure()
            val mimeTypes = inputData.getStringArray(KEY_ITEM_MIME_TYPES) ?: return@withContext Result.failure()
            val sizes = inputData.getLongArray(KEY_ITEM_SIZES) ?: return@withContext Result.failure()
            val hashes = inputData.getStringArray(KEY_ITEM_HASHES) ?: return@withContext Result.failure()

            val items = ids.indices.map { i ->
                ImportPreviewItem(
                    id = ids[i],
                    sourceUri = uris[i],
                    fileName = names[i],
                    mimeType = mimeTypes[i],
                    fileSize = sizes[i],
                    contentHash = hashes[i]
                )
            }

            Log.d(TAG, "Starting import of ${items.size} items")

            // Initialize dependencies
            val database = EmojiDatabase.getInstance(applicationContext)
            val resourceManager = ResourceManager(applicationContext)
            val emojiRepository = EmojiRepository(
                database.emojiDao(),
                database.categoryDao(),
                database.tagDao(),
                database.recentHistoryDao(),
                resourceManager
            )
            val importRepository = ImportRepository(
                applicationContext,
                emojiRepository,
                resourceManager,
                DuplicateDetector(applicationContext, database.emojiDao()),
                FileValidator(),
                ThumbnailGenerator(applicationContext, resourceManager)
            )

            var completed = 0
            var failed = 0

            items.forEachIndexed { index, item ->
                // Check for cancellation
                if (isStopped) {
                    Log.d(TAG, "Import cancelled at item $index")
                    return@withContext Result.failure()
                }

                // Report progress
                setProgress(
                    Data.Builder()
                        .putInt("completed", completed)
                        .putInt("failed", failed)
                        .putInt("total", items.size)
                        .putString("current_file", item.fileName)
                        .build()
                )

                // Import the item
                val result = importRepository.importItem(item)
                when (result) {
                    is com.emoji.overlay.data.util.ImportResult.Success -> {
                        completed++
                        Log.d(TAG, "Imported: ${item.fileName}")
                    }
                    is com.emoji.overlay.data.util.ImportResult.Error -> {
                        failed++
                        Log.w(TAG, "Failed to import ${item.fileName}: ${result.message}")
                    }
                }
            }

            Log.d(TAG, "Import completed: $completed success, $failed failed")

            val outputData = Data.Builder()
                .putInt("completed", completed)
                .putInt("failed", failed)
                .putInt("total", items.size)
                .build()

            if (failed == 0) {
                Result.success(outputData)
            } else if (completed > 0) {
                Result.success(outputData)
            } else {
                Result.failure(outputData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Import worker failed", e)
            Result.failure(Data.Builder().putString("error", e.message).build())
        }
    }
}
