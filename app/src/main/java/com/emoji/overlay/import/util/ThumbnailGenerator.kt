package com.emoji.overlay.import.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import com.emoji.overlay.data.util.ResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Generates thumbnails for emoji files.
 *
 * Directory structure:
 * EmojiVault/
 *   thumb/
 *     gif/      - GIF first frames
 *     image/    - Static image thumbnails
 *     webp/     - WebP thumbnails
 */
class ThumbnailGenerator(
    private val context: Context,
    private val resourceManager: ResourceManager
) {
    companion object {
        private const val TAG = "ThumbnailGenerator"
        private const val THUMB_MAX_SIZE = 128
        private const val THUMB_QUALITY = 80
    }

    private val gifThumbDir: File by lazy {
        File(resourceManager.vaultDir, "thumb/gif").apply { mkdirs() }
    }
    private val imageThumbDir: File by lazy {
        File(resourceManager.vaultDir, "thumb/image").apply { mkdirs() }
    }
    private val webpThumbDir: File by lazy {
        File(resourceManager.vaultDir, "thumb/webp").apply { mkdirs() }
    }

    /**
     * Generate thumbnail for a file.
     * Returns the relative path to the thumbnail, or null if generation failed.
     */
    suspend fun generate(file: File, mimeType: String): String? = withContext(Dispatchers.IO) {
        try {
            val thumbFile = when {
                mimeType == "image/gif" -> generateGifThumbnail(file)
                mimeType == "image/webp" -> generateWebpThumbnail(file)
                else -> generateImageThumbnail(file)
            }
            thumbFile?.relativeTo(resourceManager.vaultDir)?.path
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail generation failed: ${file.name}", e)
            null
        }
    }

    /**
     * Generate thumbnail for a GIF (first frame).
     */
    private fun generateGifThumbnail(file: File): File? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(0)
            retriever.release()

            frame?.let { bitmap ->
                val scaled = scaleBitmap(bitmap, THUMB_MAX_SIZE)
                val thumbFile = File(gifThumbDir, "thumb_${file.nameWithoutExtension}.jpg")
                saveBitmap(scaled, thumbFile)
                if (scaled !== bitmap) bitmap.recycle()
                scaled.recycle()
                thumbFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "GIF thumbnail failed: ${file.name}", e)
            null
        }
    }

    /**
     * Generate thumbnail for a WebP.
     */
    private fun generateWebpThumbnail(file: File): File? {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(file, THUMB_MAX_SIZE)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            val scaled = scaleBitmap(bitmap, THUMB_MAX_SIZE)
            val thumbFile = File(webpThumbDir, "thumb_${file.nameWithoutExtension}.jpg")
            saveBitmap(scaled, thumbFile)
            if (scaled !== bitmap) bitmap.recycle()
            scaled.recycle()
            thumbFile
        } catch (e: Exception) {
            Log.w(TAG, "WebP thumbnail failed: ${file.name}", e)
            null
        }
    }

    /**
     * Generate thumbnail for a static image (PNG, JPEG).
     */
    private fun generateImageThumbnail(file: File): File? {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(file, THUMB_MAX_SIZE)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return null
            val scaled = scaleBitmap(bitmap, THUMB_MAX_SIZE)
            val thumbFile = File(imageThumbDir, "thumb_${file.nameWithoutExtension}.jpg")
            saveBitmap(scaled, thumbFile)
            if (scaled !== bitmap) bitmap.recycle()
            scaled.recycle()
            thumbFile
        } catch (e: Exception) {
            Log.w(TAG, "Image thumbnail failed: ${file.name}", e)
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val scale = maxSize.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, fos)
        }
    }

    private fun calculateSampleSize(file: File, maxSize: Int): Int {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
