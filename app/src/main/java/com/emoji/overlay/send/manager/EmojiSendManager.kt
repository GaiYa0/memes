package com.emoji.overlay.send.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.send.SharePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages emoji sending to various input targets.
 *
 * Supports:
 * - Clipboard copy (for paste)
 * - Share intent (for apps that support image sharing)
 * - Direct URI sharing
 */
class EmojiSendManager(
    private val context: Context,
    private val repository: EmojiRepository,
    private val resourceManager: ResourceManager
) {
    companion object {
        private const val TAG = "EmojiSendManager"
    }

    /**
     * Send an emoji by copying to clipboard.
     * This is the most compatible method - works with any input field.
     */
    suspend fun sendToClipboard(emoji: EmojiEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = resourceManager.getFile(emoji.filePath)
            if (!file.exists()) {
                Log.w(TAG, "Emoji file not found: ${emoji.filePath}")
                return@withContext false
            }
            val contentUri = getContentUri(file)

            repository.recordUsage(emoji.id)

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(context.contentResolver, emoji.name, contentUri)
            clipboard.setPrimaryClip(clip)

            Log.d(TAG, "Emoji copied to clipboard: ${emoji.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emoji to clipboard", e)
            false
        }
    }

    /**
     * Prepare share payload for the in-app bottom sheet (records usage).
     */
    suspend fun prepareShare(emoji: EmojiEntity): SharePayload? = withContext(Dispatchers.IO) {
        try {
            val file = resourceManager.getFile(emoji.filePath)
            if (!file.exists()) {
                Log.w(TAG, "Emoji file not found: ${emoji.filePath}")
                return@withContext null
            }
            val uri = getContentUri(file)
            repository.recordUsage(emoji.id)
            SharePayload(
                uri = uri,
                mimeType = emoji.mimeType.ifBlank { "image/*" },
                title = emoji.name
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare share payload", e)
            null
        }
    }

    private fun getContentUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun getEmojiUri(emoji: EmojiEntity): Uri? {
        val file = resourceManager.getFile(emoji.filePath)
        return if (file.exists()) getContentUri(file) else null
    }

    fun getEmojiFile(emoji: EmojiEntity): File? {
        val file = resourceManager.getFile(emoji.filePath)
        return if (file.exists()) file else null
    }

    fun emojiFileExists(emoji: EmojiEntity): Boolean {
        return resourceManager.fileExists(emoji.filePath)
    }
}
