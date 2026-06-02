package com.emoji.overlay.import

import com.emoji.overlay.import.model.ImportItemStatus
import com.emoji.overlay.import.model.ImportPreviewItem
import com.emoji.overlay.import.model.ImportSession
import com.emoji.overlay.import.model.ImportSource
import com.emoji.overlay.import.model.ImportState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ImportManager selection logic.
 */
class ImportManagerTest {

    @Test
    fun `select all selects valid items`() {
        val session = createTestSession(
            validCount = 5,
            duplicateCount = 2,
            corruptedCount = 1
        )

        val updated = selectAll(session)

        assertEquals(5, updated.selectedCount)
        assertTrue(updated.items.filter { it.status == ImportItemStatus.VALID }.all { it.isSelected })
        assertFalse(updated.items.filter { it.isDuplicate }.any { it.isSelected })
        assertFalse(updated.items.filter { it.isCorrupted }.any { it.isSelected })
    }

    @Test
    fun `deselect all deselects all items`() {
        val session = createTestSession(
            validCount = 5,
            duplicateCount = 2,
            corruptedCount = 1
        ).let { selectAll(it) }

        val updated = deselectAll(session)

        assertEquals(0, updated.selectedCount)
        assertTrue(updated.items.none { it.isSelected })
    }

    @Test
    fun `select gif only`() {
        val items = listOf(
            createTestItem("1", mimeType = "image/gif"),
            createTestItem("2", mimeType = "image/png"),
            createTestItem("3", mimeType = "image/gif"),
            createTestItem("4", mimeType = "image/jpeg"),
            createTestItem("5", mimeType = "image/webp")
        )
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = items
        )

        val updated = selectGifOnly(session)

        assertEquals(2, updated.selectedCount)
        assertTrue(updated.items.filter { it.isSelected }.all { it.isGif })
    }

    @Test
    fun `select images only`() {
        val items = listOf(
            createTestItem("1", mimeType = "image/gif"),
            createTestItem("2", mimeType = "image/png"),
            createTestItem("3", mimeType = "image/gif"),
            createTestItem("4", mimeType = "image/jpeg"),
            createTestItem("5", mimeType = "image/webp")
        )
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = items
        )

        val updated = selectImagesOnly(session)

        assertEquals(3, updated.selectedCount)
        assertFalse(updated.items.filter { it.isSelected }.any { it.isGif })
    }

    @Test
    fun `exclude duplicates from selection`() {
        val items = listOf(
            createTestItem("1", isSelected = true, isDuplicate = false),
            createTestItem("2", isSelected = true, isDuplicate = true),
            createTestItem("3", isSelected = true, isDuplicate = false),
            createTestItem("4", isSelected = false, isDuplicate = true)
        )
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = items
        )

        val updated = excludeDuplicates(session)

        assertEquals(2, updated.selectedCount)
        assertFalse(updated.items.filter { it.isDuplicate }.any { it.isSelected })
    }

    @Test
    fun `toggle selection`() {
        val items = listOf(
            createTestItem("1", isSelected = false),
            createTestItem("2", isSelected = true)
        )
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = items
        )

        val toggled1 = toggleSelection(session, "1")
        assertTrue(toggled1.items.first { it.id == "1" }.isSelected)

        val toggled2 = toggleSelection(toggled1, "2")
        assertFalse(toggled2.items.first { it.id == "2" }.isSelected)
    }

    @Test
    fun `cannot select duplicate or corrupted`() {
        val items = listOf(
            createTestItem("1", isDuplicate = true),
            createTestItem("2", isCorrupted = true)
        )
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = items
        )

        val toggled1 = toggleSelection(session, "1")
        assertFalse(toggled1.items.first { it.id == "1" }.isSelected)

        val toggled2 = toggleSelection(session, "2")
        assertFalse(toggled2.items.first { it.id == "2" }.isSelected)
    }

    @Test
    fun `session counts are correct`() {
        val items = listOf(
            createTestItem("1", status = ImportItemStatus.SELECTED, isSelected = true),
            createTestItem("2", status = ImportItemStatus.SELECTED, isSelected = true),
            createTestItem("3", status = ImportItemStatus.DUPLICATE, isDuplicate = true),
            createTestItem("4", status = ImportItemStatus.CORRUPTED, isCorrupted = true),
            createTestItem("5", status = ImportItemStatus.IMPORTED),
            createTestItem("6", status = ImportItemStatus.FAILED, error = "test error"),
            createTestItem("7", status = ImportItemStatus.VALID)
        )
        val session = ImportSession(
            id = "test",
            source = ImportSource.ALBUM,
            items = items
        )

        assertEquals(7, session.totalCount)
        assertEquals(2, session.selectedCount)
        assertEquals(1, session.duplicateCount)
        assertEquals(1, session.corruptedCount)
        assertEquals(1, session.importedCount)
        assertEquals(1, session.failedCount)
        assertEquals(3, session.validCount) // 2 SELECTED + 1 VALID
    }

    // ==================== HELPERS ====================

    private fun createTestItem(
        id: String,
        mimeType: String = "image/png",
        isSelected: Boolean = false,
        isDuplicate: Boolean = false,
        isCorrupted: Boolean = false,
        status: ImportItemStatus = ImportItemStatus.VALID,
        error: String? = null
    ) = ImportPreviewItem(
        id = id,
        sourceUri = "/test/file_$id.png",
        fileName = "file_$id.png",
        mimeType = mimeType,
        fileSize = 1024L,
        status = status,
        isSelected = isSelected,
        isDuplicate = isDuplicate,
        isCorrupted = isCorrupted,
        error = error
    )

    private fun createTestSession(
        validCount: Int,
        duplicateCount: Int,
        corruptedCount: Int
    ): ImportSession {
        val items = mutableListOf<ImportPreviewItem>()
        repeat(validCount) { items.add(createTestItem("valid_$it", status = ImportItemStatus.VALID)) }
        repeat(duplicateCount) { items.add(createTestItem("dup_$it", isDuplicate = true, status = ImportItemStatus.DUPLICATE)) }
        repeat(corruptedCount) { items.add(createTestItem("corrupt_$it", isCorrupted = true, status = ImportItemStatus.CORRUPTED)) }
        return ImportSession(id = "test", source = ImportSource.ALBUM, items = items)
    }

    private fun selectAll(session: ImportSession): ImportSession {
        return session.copy(items = session.items.map { item ->
            if (item.status == ImportItemStatus.VALID || item.status == ImportItemStatus.SELECTED) {
                item.copy(isSelected = true, status = ImportItemStatus.SELECTED)
            } else item
        })
    }

    private fun deselectAll(session: ImportSession): ImportSession {
        return session.copy(items = session.items.map { item ->
            if (item.isSelected) item.copy(isSelected = false, status = ImportItemStatus.VALID) else item
        })
    }

    private fun selectGifOnly(session: ImportSession): ImportSession {
        return session.copy(items = session.items.map { item ->
            when {
                item.isGif && (item.status == ImportItemStatus.VALID || item.status == ImportItemStatus.SELECTED) ->
                    item.copy(isSelected = true, status = ImportItemStatus.SELECTED)
                item.isSelected ->
                    item.copy(isSelected = false, status = ImportItemStatus.VALID)
                else -> item
            }
        })
    }

    private fun selectImagesOnly(session: ImportSession): ImportSession {
        return session.copy(items = session.items.map { item ->
            when {
                !item.isGif && (item.status == ImportItemStatus.VALID || item.status == ImportItemStatus.SELECTED) ->
                    item.copy(isSelected = true, status = ImportItemStatus.SELECTED)
                item.isSelected ->
                    item.copy(isSelected = false, status = ImportItemStatus.VALID)
                else -> item
            }
        })
    }

    private fun excludeDuplicates(session: ImportSession): ImportSession {
        return session.copy(items = session.items.map { item ->
            if (item.isDuplicate && item.isSelected) {
                item.copy(isSelected = false, status = ImportItemStatus.DUPLICATE)
            } else item
        })
    }

    private fun toggleSelection(session: ImportSession, itemId: String): ImportSession {
        return session.copy(items = session.items.map { item ->
            if (item.id == itemId && !item.isDuplicate && !item.isCorrupted) {
                item.copy(isSelected = !item.isSelected, status = if (!item.isSelected) ImportItemStatus.SELECTED else ImportItemStatus.VALID)
            } else item
        })
    }
}
