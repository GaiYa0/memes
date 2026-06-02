package com.emoji.overlay.import

import com.emoji.overlay.import.manager.ImportQueue
import com.emoji.overlay.import.model.ImportItemStatus
import com.emoji.overlay.import.model.ImportPreviewItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ImportQueue.
 */
class ImportQueueTest {

    private lateinit var queue: ImportQueue

    @Before
    fun setup() {
        queue = ImportQueue()
    }

    @Test
    fun `enqueue and dequeue items`() = runBlocking {
        val items = (1..10).map { createTestItem(it) }
        queue.enqueue(items)

        assertEquals(10, queue.remaining())

        val item = queue.dequeue()
        assertNotNull(item)
        assertEquals("file_1.png", item?.fileName)
        assertEquals(9, queue.remaining())
    }

    @Test
    fun `dequeue returns null when paused`() = runBlocking {
        val items = (1..5).map { createTestItem(it) }
        queue.enqueue(items)

        queue.pause()
        assertTrue(queue.isPaused.value)

        val item = queue.dequeue()
        assertNull(item)
    }

    @Test
    fun `dequeue returns item after resume`() = runBlocking {
        val items = (1..5).map { createTestItem(it) }
        queue.enqueue(items)

        queue.pause()
        queue.resume()
        assertFalse(queue.isPaused.value)

        val item = queue.dequeue()
        assertNotNull(item)
    }

    @Test
    fun `dequeue returns null when cancelled`() = runBlocking {
        val items = (1..5).map { createTestItem(it) }
        queue.enqueue(items)

        queue.cancel()
        assertTrue(queue.isCancelled.value)

        val item = queue.dequeue()
        assertNull(item)
    }

    @Test
    fun `mark completed tracks items`() = runBlocking {
        val items = (1..5).map { createTestItem(it) }
        queue.enqueue(items)

        val item = queue.dequeue()!!
        queue.markCompleted(item)

        assertEquals(1, queue.getCompleted().size)
        assertEquals("file_1.png", queue.getCompleted().first().fileName)
    }

    @Test
    fun `mark failed tracks items`() = runBlocking {
        val items = (1..5).map { createTestItem(it) }
        queue.enqueue(items)

        val item = queue.dequeue()!!
        queue.markFailed(item)

        assertEquals(1, queue.getFailed().size)
    }

    @Test
    fun `checkpoint and restore`() = runBlocking {
        val items = (1..10).map { createTestItem(it) }
        queue.enqueue(items)

        // Complete first 3 items
        repeat(3) {
            val item = queue.dequeue()!!
            queue.markCompleted(item)
        }

        val checkpoint = queue.getCheckpoint()
        assertEquals(3, checkpoint.size)

        // Simulate restart
        val newQueue = ImportQueue()
        newQueue.restoreFromCheckpoint(items, checkpoint)

        assertEquals(7, newQueue.remaining())
        assertEquals(3, newQueue.getCompleted().size)
    }

    @Test
    fun `clear resets everything`() = runBlocking {
        val items = (1..5).map { createTestItem(it) }
        queue.enqueue(items)

        val item = queue.dequeue()!!
        queue.markCompleted(item)

        queue.clear()

        assertEquals(0, queue.remaining())
        assertEquals(0, queue.getCompleted().size)
        assertEquals(0, queue.getFailed().size)
    }

    @Test
    fun `dequeue returns null when empty`() = runBlocking {
        queue.enqueue(emptyList())
        val item = queue.dequeue()
        assertNull(item)
    }

    // ==================== HELPERS ====================

    private fun createTestItem(id: Int) = ImportPreviewItem(
        id = "item_$id",
        sourceUri = "/test/file_$id.png",
        fileName = "file_$id.png",
        mimeType = "image/png",
        fileSize = 1024L * id,
        status = ImportItemStatus.VALID
    )
}
