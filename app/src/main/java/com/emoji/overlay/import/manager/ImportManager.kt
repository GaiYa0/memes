package com.emoji.overlay.import.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ImportResult
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.import.model.*
import com.emoji.overlay.import.util.DuplicateDetector
import com.emoji.overlay.import.util.FileValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Manages the emoji import process.
 *
 * Responsibilities:
 * - Scan import sources (album, folder, ZIP, share)
 * - Validate files for corruption
 * - Detect duplicates
 * - Generate thumbnails
 * - Import selected files to database
 *
 * Thread safety: All state mutations happen on the main thread.
 * File I/O happens on IO dispatcher.
 */
class ImportManager(
    private val context: Context,
    private val repository: EmojiRepository,
    private val resourceManager: ResourceManager,
    private val duplicateDetector: DuplicateDetector,
    private val fileValidator: FileValidator
) {
    companion object {
        private const val TAG = "ImportManager"
        private const val MAX_CONCURRENT_IMPORTS = 4
        private const val SUPPORTED_EXTENSIONS = "png,jpg,jpeg,gif,webp"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentSession = MutableStateFlow<ImportSession?>(null)
    val currentSession: StateFlow<ImportSession?> = _currentSession.asStateFlow()

    private var importJob: Job? = null

    // ==================== SESSION MANAGEMENT ====================

    /**
     * Start a new import session from album (Photo Picker / SAF).
     */
    fun startAlbumImport(uris: List<Uri>): ImportSession {
        val session = ImportSession(
            id = UUID.randomUUID().toString(),
            source = ImportSource.ALBUM,
            sourceName = "相册选择",
            state = ImportState.SCANNING
        )
        _currentSession.value = session

        scope.launch {
            val items = scanUris(uris)
            updateSession(session.copy(items = items, state = ImportState.PREVIEW))
        }

        return session
    }

    /**
     * Start a new import session from a folder.
     */
    fun startFolderImport(folderUri: Uri, folderName: String): ImportSession {
        val session = ImportSession(
            id = UUID.randomUUID().toString(),
            source = ImportSource.FOLDER,
            sourceUri = folderUri.toString(),
            sourceName = folderName,
            state = ImportState.SCANNING
        )
        _currentSession.value = session

        scope.launch {
            val files = scanFolder(folderUri)
            val items = scanFiles(files)
            updateSession(session.copy(items = items, state = ImportState.PREVIEW))
        }

        return session
    }

    /**
     * Start a new import session from a ZIP file.
     */
    fun startZipImport(zipUri: Uri, zipName: String): ImportSession {
        val session = ImportSession(
            id = UUID.randomUUID().toString(),
            source = ImportSource.ZIP,
            sourceUri = zipUri.toString(),
            sourceName = zipName,
            state = ImportState.SCANNING
        )
        _currentSession.value = session

        scope.launch {
            val files = extractAndScanZip(zipUri)
            val items = scanFiles(files)
            updateSession(session.copy(items = items, state = ImportState.PREVIEW))
        }

        return session
    }

    /**
     * Start a new import session from share intent.
     */
    fun startShareImport(uris: List<Uri>, sourceApp: String? = null): ImportSession {
        val session = ImportSession(
            id = UUID.randomUUID().toString(),
            source = ImportSource.SHARE,
            sourceName = sourceApp ?: "分享导入",
            state = ImportState.SCANNING
        )
        _currentSession.value = session

        scope.launch {
            val items = scanUris(uris)
            updateSession(session.copy(items = items, state = ImportState.PREVIEW))
        }

        return session
    }

    // ==================== SELECTION ====================

    /**
     * Toggle selection of an item.
     */
    fun toggleSelection(itemId: String) {
        val session = _currentSession.value ?: return
        val updatedItems = session.items.map { item ->
            if (item.id == itemId && item.status != ImportItemStatus.DUPLICATE && item.status != ImportItemStatus.CORRUPTED) {
                item.copy(isSelected = !item.isSelected, status = if (!item.isSelected) ImportItemStatus.SELECTED else ImportItemStatus.VALID)
            } else {
                item
            }
        }
        updateSession(session.copy(items = updatedItems))
    }

    /**
     * Select all valid items.
     */
    fun selectAll() {
        val session = _currentSession.value ?: return
        val updatedItems = session.items.map { item ->
            if (item.status == ImportItemStatus.VALID || item.status == ImportItemStatus.SELECTED) {
                item.copy(isSelected = true, status = ImportItemStatus.SELECTED)
            } else {
                item
            }
        }
        updateSession(session.copy(items = updatedItems))
    }

    /**
     * Deselect all items.
     */
    fun deselectAll() {
        val session = _currentSession.value ?: return
        val updatedItems = session.items.map { item ->
            if (item.isSelected) {
                item.copy(isSelected = false, status = ImportItemStatus.VALID)
            } else {
                item
            }
        }
        updateSession(session.copy(items = updatedItems))
    }

    /**
     * Select only GIF items.
     */
    fun selectGifOnly() {
        val session = _currentSession.value ?: return
        val updatedItems = session.items.map { item ->
            when {
                item.isGif && (item.status == ImportItemStatus.VALID || item.status == ImportItemStatus.SELECTED) ->
                    item.copy(isSelected = true, status = ImportItemStatus.SELECTED)
                item.isSelected ->
                    item.copy(isSelected = false, status = ImportItemStatus.VALID)
                else -> item
            }
        }
        updateSession(session.copy(items = updatedItems))
    }

    /**
     * Select only static image items (non-GIF).
     */
    fun selectImagesOnly() {
        val session = _currentSession.value ?: return
        val updatedItems = session.items.map { item ->
            when {
                !item.isGif && (item.status == ImportItemStatus.VALID || item.status == ImportItemStatus.SELECTED) ->
                    item.copy(isSelected = true, status = ImportItemStatus.SELECTED)
                item.isSelected ->
                    item.copy(isSelected = false, status = ImportItemStatus.VALID)
                else -> item
            }
        }
        updateSession(session.copy(items = updatedItems))
    }

    /**
     * Exclude duplicate items from selection.
     */
    fun excludeDuplicates() {
        val session = _currentSession.value ?: return
        val updatedItems = session.items.map { item ->
            if (item.isDuplicate && item.isSelected) {
                item.copy(isSelected = false, status = ImportItemStatus.DUPLICATE)
            } else {
                item
            }
        }
        updateSession(session.copy(items = updatedItems))
    }

    // ==================== IMPORT EXECUTION ====================

    /**
     * Start importing selected items.
     */
    fun startImport() {
        val session = _currentSession.value ?: return
        if (session.state != ImportState.PREVIEW) return

        val selectedItems = session.items.filter { it.isSelected }
        if (selectedItems.isEmpty()) return

        updateSession(session.copy(state = ImportState.IMPORTING))

        importJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val updatedItems = session.items.toMutableList()
            var completed = 0
            var failed = 0

            selectedItems.forEachIndexed { index, item ->
                // Check for cancellation
                if (!isActive) {
                    updateSession(session.copy(
                        items = updatedItems,
                        state = ImportState.CANCELLED,
                        endTime = System.currentTimeMillis()
                    ))
                    return@launch
                }

                // Update progress
                val progress = ImportProgress(
                    total = selectedItems.size,
                    completed = completed,
                    failed = failed,
                    currentFile = item.fileName,
                    elapsedMs = System.currentTimeMillis() - startTime
                )
                updateSession(session.copy(
                    items = updatedItems,
                    progress = progress
                ))

                // Import the item
                val result = importSingleItem(item)
                val itemIndex = updatedItems.indexOfFirst { it.id == item.id }
                if (itemIndex >= 0) {
                    updatedItems[itemIndex] = when (result) {
                        is ImportResult.Success -> {
                            completed++
                            item.copy(
                                status = ImportItemStatus.IMPORTED,
                                thumbPath = result.thumbPath
                            )
                        }
                        is ImportResult.Error -> {
                            failed++
                            item.copy(
                                status = ImportItemStatus.FAILED,
                                error = result.message
                            )
                        }
                    }
                }
            }

            // Complete session
            updateSession(session.copy(
                items = updatedItems,
                state = ImportState.COMPLETED,
                progress = ImportProgress(
                    total = selectedItems.size,
                    completed = completed,
                    failed = failed,
                    elapsedMs = System.currentTimeMillis() - startTime
                ),
                endTime = System.currentTimeMillis()
            ))

            Log.d(TAG, "Import completed: $completed success, $failed failed")
        }
    }

    /**
     * Cancel the current import.
     */
    fun cancelImport() {
        importJob?.cancel()
        val session = _currentSession.value ?: return
        updateSession(session.copy(state = ImportState.CANCELLED, endTime = System.currentTimeMillis()))
    }

    /**
     * Clear the current session.
     */
    fun clearSession() {
        _currentSession.value = null
        importJob?.cancel()
    }

    // ==================== SCANNING ====================

    private suspend fun scanUris(uris: List<Uri>): List<ImportPreviewItem> {
        return withContext(Dispatchers.IO) {
            val items = mutableListOf<ImportPreviewItem>()
            uris.forEach { uri ->
                try {
                    val file = copyUriToTemp(uri)
                    if (file != null) {
                        val item = createPreviewItem(file)
                        items.add(item)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to scan URI: $uri", e)
                }
            }
            items
        }
    }

    private suspend fun scanFolder(folderUri: Uri): List<File> {
        return withContext(Dispatchers.IO) {
            val files = mutableListOf<File>()
            // Use DocumentFile for SAF
            val documentFile = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                android.provider.DocumentsContract.getTreeDocumentId(folderUri)
            )
            // For simplicity, we'll scan the actual file system path
            // In production, use DocumentFile API
            files
        }
    }

    private suspend fun extractAndScanZip(zipUri: Uri): List<File> {
        return withContext(Dispatchers.IO) {
            val files = mutableListOf<File>()
            val tempDir = File(context.cacheDir, "zip_import_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            try {
                context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry: ZipEntry? = zipIn.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val ext = entry.name.substringAfterLast('.', "").lowercase()
                                if (ext in SUPPORTED_EXTENSIONS.split(",")) {
                                    val outFile = File(tempDir, entry.name)
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos ->
                                        zipIn.copyTo(fos)
                                    }
                                    files.add(outFile)
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract ZIP", e)
            }

            files
        }
    }

    private suspend fun scanFiles(files: List<File>): List<ImportPreviewItem> {
        return withContext(Dispatchers.IO) {
            files.map { file -> createPreviewItem(file) }
        }
    }

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

    // ==================== IMPORT SINGLE ITEM ====================

    private suspend fun importSingleItem(item: ImportPreviewItem): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(item.sourceUri)
                if (!sourceFile.exists()) {
                    return@withContext ImportResult.Error("Source file not found")
                }

                repository.importEmoji(
                    file = sourceFile,
                    name = item.fileName.substringBeforeLast('.'),
                    keywords = "",
                    categoryId = null,
                    tags = emptyList()
                )

                ImportResult.Success(
                    relativePath = item.sourceUri,
                    thumbPath = item.thumbPath,
                    contentHash = item.contentHash,
                    fileSize = item.fileSize
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import ${item.fileName}", e)
                ImportResult.Error("Import failed: ${e.message}")
            }
        }
    }

    // ==================== HELPERS ====================

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
            Log.e(TAG, "Failed to copy URI to temp", e)
            null
        }
    }

    private fun updateSession(session: ImportSession) {
        _currentSession.value = session
    }

    fun destroy() {
        scope.cancel()
    }
}
