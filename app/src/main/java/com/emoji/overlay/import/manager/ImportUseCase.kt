package com.emoji.overlay.import.manager

import android.content.Context
import android.net.Uri
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ImportResult
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.import.model.*
import com.emoji.overlay.import.util.DuplicateDetector
import com.emoji.overlay.import.util.FileValidator
import com.emoji.overlay.import.util.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Use cases for import operations.
 * Encapsulates business logic and orchestrates between manager and repository.
 */
class ImportUseCase(
    private val importManager: ImportManager,
    private val repository: EmojiRepository,
    private val resourceManager: ResourceManager,
    private val duplicateDetector: DuplicateDetector,
    private val fileValidator: FileValidator,
    private val thumbnailGenerator: ThumbnailGenerator
) {
    // ==================== SESSION OBSERVATION ====================

    fun observeSession(): Flow<ImportSession?> = importManager.currentSession

    fun observeProgress(): Flow<ImportProgress?> = importManager.currentSession.map { it?.progress }

    fun observeState(): Flow<ImportState?> = importManager.currentSession.map { it?.state }

    // ==================== IMPORT SOURCES ====================

    fun startAlbumImport(uris: List<Uri>): ImportSession {
        return importManager.startAlbumImport(uris)
    }

    fun startFolderImport(folderUri: Uri, folderName: String): ImportSession {
        return importManager.startFolderImport(folderUri, folderName)
    }

    fun startZipImport(zipUri: Uri, zipName: String): ImportSession {
        return importManager.startZipImport(zipUri, zipName)
    }

    fun startShareImport(uris: List<Uri>, sourceApp: String? = null): ImportSession {
        return importManager.startShareImport(uris, sourceApp)
    }

    // ==================== SELECTION ====================

    fun toggleSelection(itemId: String) = importManager.toggleSelection(itemId)

    fun selectAll() = importManager.selectAll()

    fun deselectAll() = importManager.deselectAll()

    fun selectGifOnly() = importManager.selectGifOnly()

    fun selectImagesOnly() = importManager.selectImagesOnly()

    fun excludeDuplicates() = importManager.excludeDuplicates()

    // ==================== IMPORT EXECUTION ====================

    fun startImport() = importManager.startImport()

    fun cancelImport() = importManager.cancelImport()

    fun clearSession() = importManager.clearSession()

    // ==================== DATABASE SYNC ====================

    /**
     * Sync imported items to database.
     * Called after successful import to ensure Room is updated.
     */
    suspend fun syncToDatabase(items: List<ImportPreviewItem>) = withContext(Dispatchers.IO) {
        items.filter { it.status == ImportItemStatus.IMPORTED }.forEach { item ->
            try {
                val file = File(item.sourceUri)
                if (file.exists()) {
                    repository.importEmoji(
                        file = file,
                        name = item.fileName.substringBeforeLast('.'),
                        keywords = "",
                        categoryId = null,
                        tags = emptyList()
                    )
                }
            } catch (e: Exception) {
                // Log but don't fail the whole sync
            }
        }
    }

    /**
     * Verify import consistency.
     * Returns list of items that failed verification.
     */
    suspend fun verifyImportConsistency(items: List<ImportPreviewItem>): List<String> {
        return withContext(Dispatchers.IO) {
            val failures = mutableListOf<String>()
            items.filter { it.status == ImportItemStatus.IMPORTED }.forEach { item ->
                val exists = resourceManager.fileExists(item.sourceUri)
                if (!exists) {
                    failures.add(item.id)
                }
            }
            failures
        }
    }
}
