package com.emoji.overlay.import.util

import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile

/**
 * Validates image files for corruption.
 *
 * Checks:
 * - File size > 0
 * - Valid magic bytes (header)
 * - Decodable by BitmapFactory
 * - GIF structure integrity
 */
class FileValidator {
    companion object {
        private const val TAG = "FileValidator"

        // Magic bytes for supported formats
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val GIF_MAGIC = "GIF".toByteArray()
        private val WEBP_RIFF = "RIFF".toByteArray()
        private val WEBP_WEBP = "WEBP".toByteArray()

        private const val MAX_HEADER_SIZE = 12
    }

    /**
     * Validate a file for corruption.
     * Returns null if valid, error message if corrupted.
     */
    suspend fun validate(file: File, mimeType: String): String? = withContext(Dispatchers.IO) {
        try {
            // Check file exists and has content
            if (!file.exists()) return@withContext "File does not exist"
            if (file.length() == 0L) return@withContext "File is empty"
            if (file.length() < MAX_HEADER_SIZE) return@withContext "File too small"

            // Validate header based on MIME type
            val headerError = when {
                mimeType == "image/png" -> validatePng(file)
                mimeType == "image/jpeg" -> validateJpeg(file)
                mimeType == "image/gif" -> validateGif(file)
                mimeType == "image/webp" -> validateWebp(file)
                else -> "Unsupported format: $mimeType"
            }

            if (headerError != null) return@withContext headerError

            // Try to decode with BitmapFactory
            val decodeError = validateDecodability(file)
            if (decodeError != null) return@withContext decodeError

            null // Valid
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed for ${file.name}", e)
            "Validation error: ${e.message}"
        }
    }

    /**
     * Check if a file is valid (not corrupted).
     */
    suspend fun isValid(file: File, mimeType: String): Boolean {
        return validate(file, mimeType) == null
    }

    private fun validatePng(file: File): String? {
        val header = readHeader(file, PNG_MAGIC.size)
        return if (header.contentEquals(PNG_MAGIC)) {
            null
        } else {
            "Invalid PNG header"
        }
    }

    private fun validateJpeg(file: File): String? {
        val header = readHeader(file, JPEG_MAGIC.size)
        return if (header.contentEquals(JPEG_MAGIC)) {
            null
        } else {
            "Invalid JPEG header"
        }
    }

    private fun validateGif(file: File): String? {
        val header = readHeader(file, GIF_MAGIC.size)
        return if (header.contentEquals(GIF_MAGIC)) {
            // Additional GIF validation: check for valid version
            val version = String(readHeader(file, 6), 3, 3)
            if (version == "87a" || version == "89a") {
                null
            } else {
                "Invalid GIF version: $version"
            }
        } else {
            "Invalid GIF header"
        }
    }

    private fun validateWebp(file: File): String? {
        val header = readHeader(file, 12)
        val riff = header.copyOfRange(0, 4)
        val webp = header.copyOfRange(8, 12)

        return if (riff.contentEquals(WEBP_RIFF) && webp.contentEquals(WEBP_WEBP)) {
            null
        } else {
            "Invalid WebP header"
        }
    }

    private fun validateDecodability(file: File): String? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                "Unable to decode image"
            } else {
                null
            }
        } catch (e: Exception) {
            "Decoding error: ${e.message}"
        }
    }

    private fun readHeader(file: File, size: Int): ByteArray {
        val buffer = ByteArray(size)
        FileInputStream(file).use { fis ->
            fis.read(buffer)
        }
        return buffer
    }

    /**
     * Get image dimensions without loading the full image.
     */
    fun getImageDimensions(file: File): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Detect MIME type from file header.
     */
    fun detectMimeType(file: File): String? {
        if (!file.exists() || file.length() < 4) return null

        val header = readHeader(file, 12)
        return when {
            header.size >= 8 && header.copyOfRange(0, 8).contentEquals(PNG_MAGIC) -> "image/png"
            header.size >= 3 && header.copyOfRange(0, 3).contentEquals(JPEG_MAGIC) -> "image/jpeg"
            header.size >= 3 && header.copyOfRange(0, 3).contentEquals(GIF_MAGIC) -> "image/gif"
            header.size >= 12 && header.copyOfRange(0, 4).contentEquals(WEBP_RIFF) &&
                    header.copyOfRange(8, 12).contentEquals(WEBP_WEBP) -> "image/webp"
            else -> null
        }
    }
}
