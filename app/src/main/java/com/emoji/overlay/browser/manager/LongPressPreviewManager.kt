package com.emoji.overlay.browser.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.util.ResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages long press preview for emojis.
 *
 * Loads full-size images for preview with:
 * - Memory caching
 * - GIF first frame extraction
 * - WebP support
 */
class LongPressPreviewManager(
    private val context: Context,
    private val resourceManager: ResourceManager,
    private val thumbnailCache: ThumbnailCache
) {
    /**
     * Load a preview image for an emoji.
     * Returns the decoded Bitmap for display.
     */
    suspend fun loadPreview(emoji: EmojiEntity): Bitmap? = withContext(Dispatchers.IO) {
        val file = resourceManager.getFile(emoji.filePath)
        if (!file.exists()) return@withContext null

        try {
            // For preview, decode at higher resolution than thumbnail
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculatePreviewSampleSize(file, 512)
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if an emoji is animated (GIF/WebP).
     */
    fun isAnimated(emoji: EmojiEntity): Boolean {
        return emoji.mimeType == "image/gif" || emoji.mimeType == "image/webp"
    }

    /**
     * Get the file for an emoji.
     */
    fun getFile(emoji: EmojiEntity): File {
        return resourceManager.getFile(emoji.filePath)
    }

    private fun calculatePreviewSampleSize(file: File, maxSize: Int): Int {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
