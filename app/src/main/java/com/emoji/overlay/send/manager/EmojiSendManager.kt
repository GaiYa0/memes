package com.emoji.overlay.send.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ResourceManager
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

            // Record usage
            repository.recordUsage(emoji.id)

            // Copy to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(context.contentResolver, emoji.name, Uri.fromFile(file))
            clipboard.setPrimaryClip(clip)

            Log.d(TAG, "Emoji copied to clipboard: ${emoji.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emoji to clipboard", e)
            false
        }
    }

    /**
     * Send an emoji via share intent.
     * Opens the share sheet for the user to choose a target app.
     */
    suspend fun sendViaShare(emoji: EmojiEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = resourceManager.getFile(emoji.filePath)
            if (!file.exists()) {
                Log.w(TAG, "Emoji file not found: ${emoji.filePath}")
                return@withContext false
            }

            // Record usage
            repository.recordUsage(emoji.id)

            // Create share intent
            val uri = Uri.fromFile(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = emoji.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "发送表情")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Log.d(TAG, "Emoji share intent launched: ${emoji.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emoji via share", e)
            false
        }
    }

    /**
     * Get the file URI for an emoji.
     * Useful for direct URI-based operations.
     */
    fun getEmojiUri(emoji: EmojiEntity): Uri? {
        val file = resourceManager.getFile(emoji.filePath)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    /**
     * Get the file for an emoji.
     */
    fun getEmojiFile(emoji: EmojiEntity): File? {
        val file = resourceManager.getFile(emoji.filePath)
        return if (file.exists()) file else null
    }

    /**
     * Check if an emoji file exists.
     */
    fun emojiFileExists(emoji: EmojiEntity): Boolean {
        return resourceManager.fileExists(emoji.filePath)
    }
}
