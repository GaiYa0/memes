package com.emoji.overlay.import.model

import java.io.File

/**
 * Import source types.
 */
enum class ImportSource {
    ALBUM,      // Photo picker / SAF
    FOLDER,     // Directory scan
    ZIP,        // ZIP archive
    SHARE       // Share intent from other apps
}

/**
 * Status of an individual import item.
 */
enum class ImportItemStatus {
    PENDING,        // Waiting to be processed
    SCANNING,       // Being scanned
    VALID,          // Ready for import
    DUPLICATE,      // Content hash matches existing emoji
    CORRUPTED,      // File validation failed
    SELECTED,       // User selected for import
    IMPORTING,      // Currently being imported
    IMPORTED,       // Successfully imported
    FAILED          // Import failed
}

/**
 * A single item in the import preview.
 */
data class ImportPreviewItem(
    val id: String,
    val sourceUri: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val width: Int = 0,
    val height: Int = 0,
    val isAnimated: Boolean = false,
    val contentHash: String = "",
    val status: ImportItemStatus = ImportItemStatus.PENDING,
    val isSelected: Boolean = false,
    val isDuplicate: Boolean = false,
    val isCorrupted: Boolean = false,
    val existingEmojiId: Long? = null,  // ID of existing emoji if duplicate
    val thumbPath: String? = null,
    val error: String? = null
) {
    val isGif: Boolean get() = mimeType == "image/gif"
    val isWebp: Boolean get() = mimeType == "image/webp"
    val extension: String get() = fileName.substringAfterLast('.', "").lowercase()

    val formattedSize: String
        get() = when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            else -> "${fileSize / (1024 * 1024)}MB"
        }
}

/**
 * Import session state.
 */
data class ImportSession(
    val id: String,
    val source: ImportSource,
    val sourceUri: String? = null,
    val sourceName: String = "",
    val items: List<ImportPreviewItem> = emptyList(),
    val state: ImportState = ImportState.SCANNING,
    val progress: ImportProgress = ImportProgress(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val error: String? = null
) {
    val totalCount: Int get() = items.size
    val selectedCount: Int get() = items.count { it.isSelected }
    val duplicateCount: Int get() = items.count { it.isDuplicate }
    val corruptedCount: Int get() = items.count { it.isCorrupted }
    val validCount: Int get() = items.count { it.status == ImportItemStatus.VALID || it.status == ImportItemStatus.SELECTED }
    val importedCount: Int get() = items.count { it.status == ImportItemStatus.IMPORTED }
    val failedCount: Int get() = items.count { it.status == ImportItemStatus.FAILED }

    val selectedSize: Long
        get() = items.filter { it.isSelected }.sumOf { it.fileSize }

    val formattedSelectedSize: String
        get() = when {
            selectedSize < 1024 -> "${selectedSize}B"
            selectedSize < 1024 * 1024 -> "${selectedSize / 1024}KB"
            else -> "${selectedSize / (1024 * 1024)}MB"
        }
}

/**
 * Import state machine.
 */
enum class ImportState {
    SCANNING,       // Scanning source for files
    PREVIEW,        // Showing preview, waiting for user selection
    IMPORTING,      // Importing selected files
    PAUSED,         // Import paused
    COMPLETED,      // Import completed
    CANCELLED,      // Import cancelled
    ERROR           // Error occurred
}

/**
 * Import progress tracking.
 */
data class ImportProgress(
    val total: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val currentFile: String = "",
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val elapsedMs: Long = 0,
    val estimatedRemainingMs: Long = 0
) {
    val percentage: Float
        get() = if (total > 0) completed.toFloat() / total else 0f

    val isComplete: Boolean get() = completed + failed >= total

    val formattedProgress: String
        get() = "$completed / $total"

    val formattedSpeed: String
        get() {
            if (elapsedMs <= 0) return "—"
            val itemsPerSec = completed * 1000.0 / elapsedMs
            return "%.1f/s".format(itemsPerSec)
        }
}

/**
 * Filter options for import preview.
 */
data class ImportFilter(
    val showGifOnly: Boolean = false,
    val showImagesOnly: Boolean = false,
    val excludeDuplicates: Boolean = false,
    val excludeCorrupted: Boolean = false
) {
    fun matches(item: ImportPreviewItem): Boolean {
        if (showGifOnly && !item.isGif) return false
        if (showImagesOnly && item.isGif) return false
        if (excludeDuplicates && item.isDuplicate) return false
        if (excludeCorrupted && item.isCorrupted) return false
        return true
    }
}
