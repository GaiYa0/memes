package com.emoji.overlay.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/**
 * Manages emoji file resources on the filesystem.
 *
 * Directory structure:
 * ```
 * EmojiVault/
 * ├── images/          # Static images (png, jpg, webp)
 * ├── gif/             # Animated GIFs
 * ├── animated/        # Animated WebP, APNG
 * ├── video/           # MP4 stickers (future)
 * ├── thumb/           # Thumbnails
 * ├── cache/           # Temporary processing cache
 * ├── import/          # Staging area for imports
 * └── .nomedia         # Prevents media scanner
 * ```
 */
class ResourceManager(private val context: Context) {

    companion object {
        private const val TAG = "ResourceManager"
        private const val VAULT_DIR = "EmojiVault"
        private const val IMAGES_DIR = "images"
        private const val GIF_DIR = "gif"
        private const val ANIMATED_DIR = "animated"
        private const val VIDEO_DIR = "video"
        private const val THUMB_DIR = "thumb"
        private const val CACHE_DIR = "cache"
        private const val IMPORT_DIR = "import"

        private const val THUMB_MAX_SIZE = 128
        private const val THUMB_QUALITY = 80

        private val SUPPORTED_MIME_TYPES = setOf(
            "image/png", "image/jpeg", "image/webp", "image/gif"
        )

        @Volatile
        private var INSTANCE: ResourceManager? = null

        fun getInstance(context: Context): ResourceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ResourceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ==================== DIRECTORY MANAGEMENT ====================

    /** Root vault directory — lazy init, NOT in constructor */
    val vaultDir: File by lazy {
        File(context.getExternalFilesDir(null), VAULT_DIR).apply { mkdirs() }
    }

    val imagesDir: File by lazy { File(vaultDir, IMAGES_DIR).apply { mkdirs() } }
    val gifDir: File by lazy { File(vaultDir, GIF_DIR).apply { mkdirs() } }
    val animatedDir: File by lazy { File(vaultDir, ANIMATED_DIR).apply { mkdirs() } }
    val videoDir: File by lazy { File(vaultDir, VIDEO_DIR).apply { mkdirs() } }
    val thumbDir: File by lazy { File(vaultDir, THUMB_DIR).apply { mkdirs() } }
    val cacheDir: File by lazy { File(vaultDir, CACHE_DIR).apply { mkdirs() } }
    val importDir: File by lazy { File(vaultDir, IMPORT_DIR).apply { mkdirs() } }

    /**
     * Initialize vault directories. Call from background thread.
     * Safe to call multiple times.
     */
    fun ensureDirectories() {
        vaultDir.mkdirs()
        imagesDir.mkdirs()
        gifDir.mkdirs()
        animatedDir.mkdirs()
        videoDir.mkdirs()
        thumbDir.mkdirs()
        cacheDir.mkdirs()
        importDir.mkdirs()
        File(vaultDir, ".nomedia").createNewFile()
    }

    // ==================== FILE OPERATIONS ====================

    /**
     * Import a file into the vault. Returns the stored file path relative to vault.
     * Handles deduplication by content hash.
     */
    fun importFile(sourceFile: File, mimeType: String): ImportResult {
        if (!sourceFile.exists()) {
            return ImportResult.Error("Source file does not exist: ${sourceFile.absolutePath}")
        }

        if (!isSupportedMimeType(mimeType)) {
            return ImportResult.Error("Unsupported MIME type: $mimeType")
        }

        // Validate file integrity
        if (!validateFile(sourceFile, mimeType)) {
            return ImportResult.Error("File validation failed: ${sourceFile.absolutePath}")
        }

        // Calculate content hash for deduplication
        val contentHash = calculateContentHash(sourceFile)

        // Determine target directory
        val targetDir = getDirectoryForMimeType(mimeType)
        val extension = getExtensionForMimeType(mimeType)
        val fileName = "${UUID.randomUUID()}.$extension"
        val targetFile = File(targetDir, fileName)

        // Copy file to vault
        return try {
            sourceFile.copyTo(targetFile, overwrite = true)
            val relativePath = targetFile.relativeTo(vaultDir).path

            // Generate thumbnail
            val thumbPath = generateThumbnail(targetFile, mimeType)

            ImportResult.Success(
                relativePath = relativePath,
                thumbPath = thumbPath,
                contentHash = contentHash,
                fileSize = targetFile.length()
            )
        } catch (e: IOException) {
            Log.e(TAG, "Failed to import file", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }

    /**
     * Delete a file from the vault.
     */
    fun deleteFile(relativePath: String): Boolean {
        val file = File(vaultDir, relativePath)
        return if (file.exists()) {
            file.delete()
        } else {
            true // Already deleted
        }
    }

    /**
     * Delete a file and its thumbnail.
     */
    fun deleteFileWithThumb(relativePath: String, thumbPath: String?) {
        deleteFile(relativePath)
        thumbPath?.let { deleteFile(it) }
    }

    /**
     * Check if a file exists in the vault.
     */
    fun fileExists(relativePath: String): Boolean {
        return File(vaultDir, relativePath).exists()
    }

    /**
     * Get the absolute file for a relative path.
     */
    fun getFile(relativePath: String): File {
        return File(vaultDir, relativePath)
    }

    /**
     * Get file size in bytes.
     */
    fun getFileSize(relativePath: String): Long {
        val file = File(vaultDir, relativePath)
        return if (file.exists()) file.length() else 0
    }

    // ==================== VALIDATION ====================

    /**
     * Validate file integrity based on MIME type.
     */
    fun validateFile(file: File, mimeType: String): Boolean {
        if (!file.exists() || file.length() == 0L) return false

        return when {
            mimeType.startsWith("image/") -> validateImageFile(file)
            mimeType == "image/gif" -> validateGifFile(file)
            else -> true
        }
    }

    private fun validateImageFile(file: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (e: Exception) {
            Log.w(TAG, "Image validation failed: ${file.name}", e)
            false
        }
    }

    private fun validateGifFile(file: File): Boolean {
        // Basic GIF magic number check
        if (file.length() < 6) return false
        return try {
            val header = ByteArray(6)
            FileInputStream(file).use { it.read(header) }
            header[0] == 'G'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a MIME type is supported.
     */
    fun isSupportedMimeType(mimeType: String): Boolean {
        return mimeType in SUPPORTED_MIME_TYPES
    }

    // ==================== HASHING ====================

    /**
     * Calculate MD5 content hash for deduplication.
     */
    fun calculateContentHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { fis ->
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate hash from byte array.
     */
    fun calculateContentHash(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }

    // ==================== THUMBNAILS ====================

    /**
     * Generate a thumbnail for an image file.
     * Returns the relative path to the thumbnail, or null if generation failed.
     */
    fun generateThumbnail(file: File, mimeType: String): String? {
        if (mimeType == "image/gif") {
            // For GIFs, use the first frame
            return generateGifThumbnail(file)
        }

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val scale = calculateSampleSize(options.outWidth, options.outHeight, THUMB_MAX_SIZE)
            options.inJustDecodeBounds = false
            options.inSampleSize = scale

            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null

            val thumbFileName = "thumb_${file.nameWithoutExtension}.jpg"
            val thumbFile = File(thumbDir, thumbFileName)

            FileOutputStream(thumbFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, fos)
            }
            bitmap.recycle()

            thumbFile.relativeTo(vaultDir).path
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail generation failed: ${file.name}", e)
            null
        }
    }

    private fun generateGifThumbnail(file: File): String? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(0)
            retriever.release()

            frame?.let { bitmap ->
                val scaled = Bitmap.createScaledBitmap(bitmap, THUMB_MAX_SIZE, THUMB_MAX_SIZE, true)
                if (scaled !== bitmap) bitmap.recycle()

                val thumbFileName = "thumb_${file.nameWithoutExtension}.jpg"
                val thumbFile = File(thumbDir, thumbFileName)
                FileOutputStream(thumbFile).use { fos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, fos)
                }
                scaled.recycle()
                thumbFile.relativeTo(vaultDir).path
            }
        } catch (e: Exception) {
            Log.w(TAG, "GIF thumbnail failed: ${file.name}", e)
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }

    // ==================== CLEANUP ====================

    /**
     * Delete orphaned files that have no database record.
     */
    fun cleanupOrphanedFiles(existingPaths: Set<String>) {
        val allFiles = getAllVaultFiles()
        allFiles.forEach { file ->
            val relativePath = file.relativeTo(vaultDir).path
            if (relativePath !in existingPaths && !relativePath.endsWith(".nomedia")) {
                file.delete()
                Log.d(TAG, "Deleted orphaned file: $relativePath")
            }
        }
    }

    /**
     * Get all files in the vault (excluding .nomedia and cache).
     */
    private fun getAllVaultFiles(): List<File> {
        val files = mutableListOf<File>()
        listOf(imagesDir, gifDir, animatedDir, videoDir, thumbDir).forEach { dir ->
            dir.listFiles()?.let { files.addAll(it) }
        }
        return files
    }

    /**
     * Clear the cache directory.
     */
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Get total vault size in bytes.
     */
    fun getVaultSize(): Long {
        return vaultDir.walkTopDown()
            .filter { it.isFile && it.name != ".nomedia" }
            .sumOf { it.length() }
    }

    /**
     * Scan import directory and return list of importable files.
     */
    fun scanImportDirectory(): List<File> {
        return importDir.listFiles()?.filter { file ->
            val ext = file.extension.lowercase()
            ext in listOf("png", "jpg", "jpeg", "webp", "gif")
        } ?: emptyList()
    }

    // ==================== HELPERS ====================

    private fun getDirectoryForMimeType(mimeType: String): File {
        return when {
            mimeType == "image/gif" -> gifDir
            mimeType == "image/webp" && false -> animatedDir // TODO: detect animated webp
            mimeType.startsWith("video/") -> videoDir
            else -> imagesDir
        }
    }

    private fun getExtensionForMimeType(mimeType: String): String {
        return when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "bin"
        }
    }

    /**
     * Detect MIME type with multi-step fallback:
     * 1) caller-provided mimeHint (e.g. ContentResolver.getType)
     * 2) file header signature sniffing
     * 3) file extension
     */
    fun detectMimeType(file: File, mimeHint: String? = null): String {
        val normalizedHint = mimeHint?.lowercase()
        if (normalizedHint != null && isSupportedMimeType(normalizedHint)) {
            return normalizedHint
        }

        detectMimeTypeByHeader(file)?.let { return it }

        return when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "application/octet-stream"
        }
    }

    private fun detectMimeTypeByHeader(file: File): String? {
        if (!file.exists() || file.length() < 12) return null
        return try {
            val header = ByteArray(12)
            FileInputStream(file).use { input ->
                val read = input.read(header)
                if (read < 12) return null
            }

            when {
                // PNG signature: 89 50 4E 47 0D 0A 1A 0A
                header[0] == 0x89.toByte() &&
                    header[1] == 0x50.toByte() &&
                    header[2] == 0x4E.toByte() &&
                    header[3] == 0x47.toByte() -> "image/png"

                // JPEG signature: FF D8 FF
                header[0] == 0xFF.toByte() &&
                    header[1] == 0xD8.toByte() &&
                    header[2] == 0xFF.toByte() -> "image/jpeg"

                // GIF signature: GIF87a / GIF89a
                header[0] == 'G'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() -> "image/gif"

                // WebP signature: RIFF....WEBP
                header[0] == 'R'.code.toByte() &&
                    header[1] == 'I'.code.toByte() &&
                    header[2] == 'F'.code.toByte() &&
                    header[3] == 'F'.code.toByte() &&
                    header[8] == 'W'.code.toByte() &&
                    header[9] == 'E'.code.toByte() &&
                    header[10] == 'B'.code.toByte() &&
                    header[11] == 'P'.code.toByte() -> "image/webp"

                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Header mime detect failed: ${file.name}", e)
            null
        }
    }
}

/**
 * Result of a file import operation.
 */
sealed class ImportResult {
    data class Success(
        val relativePath: String,
        val thumbPath: String?,
        val contentHash: String,
        val fileSize: Long
    ) : ImportResult()

    data class Error(val message: String) : ImportResult()
}
