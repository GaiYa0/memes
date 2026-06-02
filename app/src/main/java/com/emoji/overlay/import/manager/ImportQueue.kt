package com.emoji.overlay.import.manager

import com.emoji.overlay.import.model.ImportPreviewItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe import queue with pause/resume/cancel support.
 *
 * Supports checkpoint-based resume for interrupted imports.
 */
class ImportQueue {
    private val queue = ConcurrentLinkedQueue<ImportPreviewItem>()
    private val completed = mutableListOf<ImportPreviewItem>()
    private val failed = mutableListOf<ImportPreviewItem>()
    private val mutex = Mutex()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _isCancelled = MutableStateFlow(false)
    val isCancelled: StateFlow<Boolean> = _isCancelled

    private var checkpoint: List<String> = emptyList()

    /**
     * Enqueue items for import.
     */
    fun enqueue(items: List<ImportPreviewItem>) {
        queue.clear()
        queue.addAll(items)
        completed.clear()
        failed.clear()
        _isPaused.value = false
        _isCancelled.value = false
        checkpoint = emptyList()
    }

    /**
     * Dequeue next item. Returns null if paused, cancelled, or empty.
     */
    suspend fun dequeue(): ImportPreviewItem? = mutex.withLock {
        if (_isPaused.value || _isCancelled.value) return@withLock null
        return@withLock queue.poll()
    }

    /**
     * Mark an item as completed.
     */
    suspend fun markCompleted(item: ImportPreviewItem) = mutex.withLock {
        completed.add(item)
        updateCheckpoint()
    }

    /**
     * Mark an item as failed.
     */
    suspend fun markFailed(item: ImportPreviewItem) = mutex.withLock {
        failed.add(item)
    }

    /**
     * Pause the import queue.
     */
    fun pause() {
        _isPaused.value = true
    }

    /**
     * Resume the import queue.
     */
    fun resume() {
        _isPaused.value = false
    }

    /**
     * Cancel the import queue.
     */
    fun cancel() {
        _isCancelled.value = true
    }

    /**
     * Get remaining items count.
     */
    fun remaining(): Int = queue.size

    /**
     * Get completed items.
     */
    fun getCompleted(): List<ImportPreviewItem> = completed.toList()

    /**
     * Get failed items.
     */
    fun getFailed(): List<ImportPreviewItem> = failed.toList()

    /**
     * Save checkpoint for resume capability.
     */
    private fun updateCheckpoint() {
        checkpoint = completed.map { it.id }
    }

    /**
     * Get checkpoint for persistence.
     */
    fun getCheckpoint(): List<String> = checkpoint

    /**
     * Restore from checkpoint.
     */
    fun restoreFromCheckpoint(allItems: List<ImportPreviewItem>, completedIds: List<String>) {
        val remaining = allItems.filter { it.id !in completedIds }
        queue.clear()
        queue.addAll(remaining)
        completed.clear()
        completed.addAll(allItems.filter { it.id in completedIds })
        failed.clear()
        _isPaused.value = false
        _isCancelled.value = false
    }

    /**
     * Clear the queue.
     */
    fun clear() {
        queue.clear()
        completed.clear()
        failed.clear()
        checkpoint = emptyList()
        _isPaused.value = false
        _isCancelled.value = false
    }
}
