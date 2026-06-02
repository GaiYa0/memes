package com.emoji.overlay.import.manager

import android.content.Context
import android.net.Uri
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ImportResult
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.import.model.ImportItemStatus
import com.emoji.overlay.import.model.ImportPreviewItem
import com.emoji.overlay.import.util.DuplicateDetector
import com.emoji.overlay.import.util.FileValidator
import com.emoji.overlay.import.util.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Repository for import operations.
 * Handles file I/O, validation, deduplication, and database sync.
 */
class ImportRepository(
    private val context: Context,
    private val emojiRepository: EmojiRepository,
    private val resourceManager: ResourceManager,
    private val duplicateDetector: DuplicateDetector,
    private val fileValidator: FileValidator,
    private val thumbnailGenerator: ThumbnailGenerator
) {
    companion object {
        private const val SUPPORTED_EXTENSIONS = "png,jpg,jpeg,gif,webp"
    }

    /**
     * Scan URIs and create preview items.
     */
    suspend fun scanUris(uris: List<Uri>): List<ImportPreviewItem> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            try {
                val file = copyUriToTemp(uri)
                if (file != null) createPreviewItem(file) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Scan a directory recursively for supported files.
     */
    suspend fun scanDirectory(directory: File): List<ImportPreviewItem> = withContext(Dispatchers.IO) {
        val files = directory.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in SUPPORTED_EXTENSIONS.split(",") }
            .toList()
        files.map { createPreviewItem(it) }
    }

    /**
     * Scan ZIP contents.
     */
    suspend fun scanZip(zipFile: File): List<ImportPreviewItem> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "zip_scan_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val files = mutableListOf<File>()
            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val ext = entry.name.substringAfterLast('.', "").lowercase()
                        if (ext in SUPPORTED_EXTENSIONS.split(",")) {
                            val outFile = File(tempDir, entry.name)
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { zipIn.copyTo(it) }
                            files.add(outFile)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            files.map { createPreviewItem(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Import a single item to the vault and database.
     */
    suspend fun importItem(item: ImportPreviewItem): ImportResult = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(item.sourceUri)
            if (!sourceFile.exists()) {
                return@withContext ImportResult.Error("Source file not found")
            }

            // Import to vault
            val result = resourceManager.importFile(sourceFile, item.mimeType)
            if (result is ImportResult.Error) {
                return@withContext result
            }
            val success = result as ImportResult.Success

            // Generate thumbnail
            val thumbPath = thumbnailGenerator.generate(sourceFile, item.mimeType)

            // Write to database
            val emojiId = emojiRepository.importEmoji(
                file = sourceFile,
                name = item.fileName.substringBeforeLast('.'),
                keywords = "",
                categoryId = null,
                tags = emptyList()
            )

            if (emojiId.isFailure) {
                return@withContext ImportResult.Error("Database write failed: ${emojiId.exceptionOrNull()?.message}")
            }

            ImportResult.Success(
                relativePath = success.relativePath,
                thumbPath = thumbPath ?: success.thumbPath,
                contentHash = success.contentHash,
                fileSize = success.fileSize
            )
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Batch import items.
     */
    suspend fun importBatch(
        items: List<ImportPreviewItem>,
        onProgress: (ImportPreviewItem, ImportResult) -> Unit
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var success = 0
        var failed = 0

        items.forEach { item ->
            val result = importItem(item)
            when (result) {
                is ImportResult.Success -> success++
                is ImportResult.Error -> failed++
            }
            onProgress(item, result)
        }

        Pair(success, failed)
    }

    /**
     * Verify database consistency after import.
     */
    suspend fun verifyConsistency(items: List<ImportPreviewItem>): List<String> = withContext(Dispatchers.IO) {
        items.filter { it.status == ImportItemStatus.IMPORTED }.mapNotNull { item ->
            val exists = resourceManager.fileExists(item.sourceUri)
            if (!exists) item.id else null
        }
    }

    /**
     * Create preview item from file.
     */
    private suspend fun createPreviewItem(file: File): ImportPreviewItem {
        val mimeType = fileValidator.detectMimeType(file) ?: "application/octet-stream"
        val dimensions = fileValidator.getImageDimensions(file)
        val hash = duplicateDetector.calculateHash(file)
        val existingDuplicate = if (hash.isNotEmpty()) duplicateDetector.findDuplicate(file) else null
        val validationError = fileValidator.validate(file, mimeType)

        return ImportPreviewItem(
            id = UUID.randomUUID().toString(),
            sourceUri = file.absolutePath,
            fileName = file.name,
            mimeType = mimeType,
            fileSize = file.length(),
            width = dimensions?.first ?: 0,
            height = dimensions?.second ?: 0,
            isAnimated = mimeType == "image/gif",
            contentHash = hash,
            status = when {
                existingDuplicate != null -> ImportItemStatus.DUPLICATE
                validationError != null -> ImportItemStatus.CORRUPTED
                else -> ImportItemStatus.VALID
            },
            isDuplicate = existingDuplicate != null,
            isCorrupted = validationError != null,
            existingEmojiId = existingDuplicate,
            error = validationError
        )
    }

    private fun copyUriToTemp(uri: Uri): File? {
        return try {
            val tempFile = File(context.cacheDir, "import_${UUID.randomUUID()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
