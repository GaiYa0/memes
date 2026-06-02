package com.emoji.overlay.import

import com.emoji.overlay.import.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for import data models and logic.
 */
class ImportModelsTest {

    @Test
    fun `ImportPreviewItem formatted size for bytes`() {
        val item = createTestItem(fileSize = 500)
        assertEquals("500B", item.formattedSize)
    }

    @Test
    fun `ImportPreviewItem formatted size for kilobytes`() {
        val item = createTestItem(fileSize = 1024)
        assertEquals("1KB", item.formattedSize)
    }

    @Test
    fun `ImportPreviewItem formatted size for megabytes`() {
        val item = createTestItem(fileSize = 1024 * 1024)
        assertEquals("1MB", item.formattedSize)
    }

    @Test
    fun `ImportPreviewItem isGif detection`() {
        val gif = createTestItem(mimeType = "image/gif")
        val png = createTestItem(mimeType = "image/png")
        assertTrue(gif.isGif)
        assertFalse(png.isGif)
    }

    @Test
    fun `ImportPreviewItem extension extraction`() {
        val item = createTestItem(fileName = "smile.gif")
        assertEquals("gif", item.extension)
    }

    @Test
    fun `ImportSession counts`() {
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = listOf(
                createTestItem(status = ImportItemStatus.SELECTED, isSelected = true),
                createTestItem(status = ImportItemStatus.SELECTED, isSelected = true),
                createTestItem(status = ImportItemStatus.DUPLICATE, isDuplicate = true),
                createTestItem(status = ImportItemStatus.CORRUPTED, isCorrupted = true),
                createTestItem(status = ImportItemStatus.VALID)
            )
        )

        assertEquals(5, session.totalCount)
        assertEquals(2, session.selectedCount)
        assertEquals(1, session.duplicateCount)
        assertEquals(1, session.corruptedCount)
        assertEquals(3, session.validCount) // 2 SELECTED + 1 VALID
    }

    @Test
    fun `ImportSession selected size`() {
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = listOf(
                createTestItem(fileSize = 1024, isSelected = true, status = ImportItemStatus.SELECTED),
                createTestItem(fileSize = 2048, isSelected = true, status = ImportItemStatus.SELECTED),
                createTestItem(fileSize = 4096, isSelected = false, status = ImportItemStatus.VALID)
            )
        )

        assertEquals(3072L, session.selectedSize)
    }

    @Test
    fun `ImportProgress percentage`() {
        val progress = ImportProgress(total = 100, completed = 50)
        assertEquals(0.5f, progress.percentage, 0.01f)
    }

    @Test
    fun `ImportProgress is complete`() {
        val incomplete = ImportProgress(total = 100, completed = 50, failed = 0)
        val complete = ImportProgress(total = 100, completed = 90, failed = 10)
        assertFalse(incomplete.isComplete)
        assertTrue(complete.isComplete)
    }

    @Test
    fun `ImportFilter show gif only`() {
        val filter = ImportFilter(showGifOnly = true)
        val gif = createTestItem(mimeType = "image/gif")
        val png = createTestItem(mimeType = "image/png")

        assertTrue(filter.matches(gif))
        assertFalse(filter.matches(png))
    }

    @Test
    fun `ImportFilter show images only`() {
        val filter = ImportFilter(showImagesOnly = true)
        val gif = createTestItem(mimeType = "image/gif")
        val png = createTestItem(mimeType = "image/png")

        assertFalse(filter.matches(gif))
        assertTrue(filter.matches(png))
    }

    @Test
    fun `ImportFilter exclude duplicates`() {
        val filter = ImportFilter(excludeDuplicates = true)
        val normal = createTestItem(isDuplicate = false)
        val duplicate = createTestItem(isDuplicate = true)

        assertTrue(filter.matches(normal))
        assertFalse(filter.matches(duplicate))
    }

    @Test
    fun `ImportFilter exclude corrupted`() {
        val filter = ImportFilter(excludeCorrupted = true)
        val normal = createTestItem(isCorrupted = false)
        val corrupted = createTestItem(isCorrupted = true)

        assertTrue(filter.matches(normal))
        assertFalse(filter.matches(corrupted))
    }

    @Test
    fun `ImportFilter combined filters`() {
        val filter = ImportFilter(showGifOnly = true, excludeDuplicates = true)
        val gifNormal = createTestItem(mimeType = "image/gif", isDuplicate = false)
        val gifDuplicate = createTestItem(mimeType = "image/gif", isDuplicate = true)
        val pngNormal = createTestItem(mimeType = "image/png", isDuplicate = false)

        assertTrue(filter.matches(gifNormal))
        assertFalse(filter.matches(gifDuplicate))
        assertFalse(filter.matches(pngNormal))
    }

    // ==================== HELPERS ====================

    private fun createTestItem(
        id: String = "test_${System.nanoTime()}",
        fileName: String = "test.png",
        mimeType: String = "image/png",
        fileSize: Long = 1024,
        status: ImportItemStatus = ImportItemStatus.VALID,
        isSelected: Boolean = false,
        isDuplicate: Boolean = false,
        isCorrupted: Boolean = false
    ) = ImportPreviewItem(
        id = id,
        sourceUri = "/test/$fileName",
        fileName = fileName,
        mimeType = mimeType,
        fileSize = fileSize,
        status = status,
        isSelected = isSelected,
        isDuplicate = isDuplicate,
        isCorrupted = isCorrupted
    )
}
