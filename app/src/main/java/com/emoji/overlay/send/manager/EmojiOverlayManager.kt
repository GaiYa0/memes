package com.emoji.overlay.send.manager

import android.content.Context
import android.util.Log
import com.emoji.overlay.data.database.EmojiDatabase
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.browser.manager.LongPressPreviewManager
import com.emoji.overlay.browser.manager.ThumbnailCache
import com.emoji.overlay.browser.viewmodel.EmojiBrowserViewModel
import com.emoji.overlay.browser.usecase.*
import com.emoji.overlay.send.SharePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the emoji overlay panel integration.
 *
 * Responsibilities:
 * - Initialize and manage the overlay panel lifecycle
 * - Coordinate between overlay UI and send operations
 * - Handle emoji selection and preview
 * - Manage state for the overlay panel
 */
class EmojiOverlayManager(private val context: Context) {
    companion object {
        private const val TAG = "EmojiOverlayManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Dependencies
    private val database = EmojiDatabase.getInstance(context)
    private val resourceManager = ResourceManager(context)
    private val emojiRepository = EmojiRepository(
        database.emojiDao(),
        database.categoryDao(),
        database.tagDao(),
        database.recentHistoryDao(),
        resourceManager
    )
    private val thumbnailCache = ThumbnailCache(context)
    private val longPressPreviewManager = LongPressPreviewManager(context, resourceManager, thumbnailCache)
    private val sendManager = EmojiSendManager(context, emojiRepository, resourceManager)

    // Use cases
    private val categoryUseCase = EmojiCategoryUseCase(emojiRepository)
    private val recentUseCase = RecentUseCase(emojiRepository)
    private val favoriteUseCase = FavoriteUseCase(emojiRepository)
    private val searchUseCase = SearchUseCase(emojiRepository)

    // ViewModel
    val viewModel = EmojiBrowserViewModel(
        categoryUseCase,
        recentUseCase,
        favoriteUseCase,
        searchUseCase
    )

    // State
    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    private val _selectedEmoji = MutableStateFlow<EmojiEntity?>(null)
    val selectedEmoji: StateFlow<EmojiEntity?> = _selectedEmoji.asStateFlow()

    private val _pendingShare = MutableStateFlow<SharePayload?>(null)
    val pendingShare: StateFlow<SharePayload?> = _pendingShare.asStateFlow()

    private val _sendResult = MutableStateFlow<SendResult?>(null)
    val sendResult: StateFlow<SendResult?> = _sendResult.asStateFlow()

    /**
     * Show the overlay panel.
     */
    fun showOverlay() {
        _isOverlayVisible.value = true
        Log.d(TAG, "Overlay shown")
    }

    /**
     * Hide the overlay panel.
     */
    fun hideOverlay() {
        _isOverlayVisible.value = false
        _selectedEmoji.value = null
        Log.d(TAG, "Overlay hidden")
    }

    /**
     * Toggle overlay visibility.
     */
    fun toggleOverlay() {
        if (_isOverlayVisible.value) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    /**
     * Select an emoji for preview or sending.
     */
    fun selectEmoji(emoji: EmojiEntity) {
        _selectedEmoji.value = emoji
        viewModel.selectEmoji(emoji)
    }

    /**
     * Send the selected emoji via clipboard.
     */
    fun sendSelectedEmoji() {
        val emoji = _selectedEmoji.value ?: return
        scope.launch {
            val success = sendManager.sendToClipboard(emoji)
            _sendResult.value = if (success) {
                SendResult.Success(emoji)
            } else {
                SendResult.Failure(emoji, "Failed to copy to clipboard")
            }

            if (success) {
                // Update recent usage
                viewModel.recordUsage(emoji.id)
                // Hide overlay after sending
                hideOverlay()
            }
        }
    }

    /**
     * Send a specific emoji via clipboard.
     */
    fun sendEmoji(emoji: EmojiEntity) {
        scope.launch {
            val success = sendManager.sendToClipboard(emoji)
            _sendResult.value = if (success) {
                SendResult.Success(emoji)
            } else {
                SendResult.Failure(emoji, "Failed to copy to clipboard")
            }

            if (success) {
                viewModel.recordUsage(emoji.id)
            }
        }
    }

    /**
     * Share a specific emoji via share intent.
     */
    fun shareEmoji(emoji: EmojiEntity) {
        scope.launch {
            val payload = kotlinx.coroutines.withContext(Dispatchers.IO) {
                sendManager.prepareShare(emoji)
            }
            if (payload != null) {
                _pendingShare.value = payload
                _sendResult.value = SendResult.Shared(emoji)
                viewModel.recordUsage(emoji.id)
            } else {
                _sendResult.value = SendResult.Failure(emoji, "Failed to share")
            }
        }
    }

    fun clearPendingShare() {
        _pendingShare.value = null
    }

    /**
     * Toggle favorite status for an emoji.
     */
    fun toggleFavorite(emojiId: Long) {
        viewModel.toggleFavorite(emojiId)
    }

    /**
     * Show long press preview for an emoji.
     */
    fun showPreview(emoji: EmojiEntity) {
        viewModel.showPreview(emoji)
    }

    /**
     * Dismiss the preview.
     */
    fun dismissPreview() {
        viewModel.dismissPreview()
    }

    /**
     * Get the long press preview manager.
     */
    fun getLongPressPreviewManager(): LongPressPreviewManager = longPressPreviewManager

    /**
     * Get the send manager.
     */
    fun getSendManager(): EmojiSendManager = sendManager

    /**
     * Get the thumbnail cache.
     */
    fun getThumbnailCache(): ThumbnailCache = thumbnailCache

    /**
     * Clear the send result.
     */
    fun clearSendResult() {
        _sendResult.value = null
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        thumbnailCache.clear()
    }
}

/**
 * Result of a send operation.
 */
sealed class SendResult {
    data class Success(val emoji: EmojiEntity) : SendResult()
    data class Shared(val emoji: EmojiEntity) : SendResult()
    data class Failure(val emoji: EmojiEntity, val error: String) : SendResult()
}
