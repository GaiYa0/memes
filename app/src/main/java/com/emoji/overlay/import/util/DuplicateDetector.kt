package com.emoji.overlay.import.util

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.emoji.overlay.data.dao.EmojiDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Detects duplicate emojis using content hash (SHA-256).
 *
 * Duplicate detection is based on file content, not filename.
 * Two files with different names but identical content are duplicates.
 */
class DuplicateDetector(
    private val context: Context,
    private val emojiDao: EmojiDao
) {
    companion object {
        private const val TAG = "DuplicateDetector"
        private const val HASH_ALGORITHM = "SHA-256"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Calculate SHA-256 hash of a file.
     */
    suspend fun calculateHash(file: File): String = withContext(Dispatchers.IO) {
        try {
            val md = MessageDigest.getInstance(HASH_ALGORITHM)
            val buffer = ByteArray(BUFFER_SIZE)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate hash for ${file.name}", e)
            ""
        }
    }

    /**
     * Calculate SHA-256 hash from byte array.
     */
    fun calculateHash(data: ByteArray): String {
        val md = MessageDigest.getInstance(HASH_ALGORITHM)
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }

    /**
     * Check if a file is a duplicate of an existing emoji.
     * Returns the existing emoji ID if duplicate, null otherwise.
     */
    suspend fun findDuplicate(file: File): Long? {
        val hash = calculateHash(file)
        if (hash.isEmpty()) return null

        val existing = emojiDao.getByContentHash(hash)
        return existing?.id
    }

    /**
     * Check if a hash already exists in the database.
     */
    suspend fun isDuplicate(hash: String): Boolean {
        if (hash.isEmpty()) return false
        return emojiDao.getByContentHash(hash) != null
    }

    /**
     * Batch check for duplicates.
     * Returns map of file path -> existing emoji ID (null if not duplicate).
     */
    suspend fun findDuplicates(files: List<File>): Map<String, Long?> {
        return withContext(Dispatchers.IO) {
            files.associate { file ->
                val hash = calculateHash(file)
                val existing = if (hash.isNotEmpty()) {
                    emojiDao.getByContentHash(hash)
                } else null
                file.absolutePath to existing?.id
            }
        }
    }

    /**
     * Find duplicates within a batch of files (internal duplicates).
     * Returns groups of files with identical content.
     */
    suspend fun findInternalDuplicates(files: List<File>): Map<String, List<File>> {
        return withContext(Dispatchers.IO) {
            val hashMap = mutableMapOf<String, MutableList<File>>()
            files.forEach { file ->
                val hash = calculateHash(file)
                if (hash.isNotEmpty()) {
                    hashMap.getOrPut(hash) { mutableListOf() }.add(file)
                }
            }
            hashMap.filter { it.value.size > 1 }
        }
    }
}
