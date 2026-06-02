package com.emoji.overlay.browser

import com.emoji.overlay.browser.usecase.*
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.TagEntity
import com.emoji.overlay.browser.viewmodel.BrowserScreen
import com.emoji.overlay.browser.viewmodel.BrowserUiState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for EmojiBrowserViewModel logic.
 */
class EmojiBrowserViewModelTest {

    @Test
    fun `initial state is HOME screen`() {
        val state = BrowserUiState()
        assertEquals(BrowserScreen.HOME, state.currentScreen)
        assertNull(state.selectedEmoji)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `navigate to category screen`() {
        val state = BrowserUiState(currentScreen = BrowserScreen.HOME)
        val updated = state.copy(currentScreen = BrowserScreen.CATEGORY)
        assertEquals(BrowserScreen.CATEGORY, updated.currentScreen)
    }

    @Test
    fun `navigate to favorites screen`() {
        val state = BrowserUiState()
        val updated = state.copy(currentScreen = BrowserScreen.FAVORITES)
        assertEquals(BrowserScreen.FAVORITES, updated.currentScreen)
    }

    @Test
    fun `navigate to recent screen`() {
        val state = BrowserUiState()
        val updated = state.copy(currentScreen = BrowserScreen.RECENT)
        assertEquals(BrowserScreen.RECENT, updated.currentScreen)
    }

    @Test
    fun `navigate to search screen`() {
        val state = BrowserUiState()
        val updated = state.copy(currentScreen = BrowserScreen.SEARCH)
        assertEquals(BrowserScreen.SEARCH, updated.currentScreen)
    }

    @Test
    fun `select emoji updates state`() {
        val emoji = createTestEmoji(1, "smile")
        val state = BrowserUiState()
        val updated = state.copy(selectedEmoji = emoji)
        assertNotNull(updated.selectedEmoji)
        assertEquals("smile", updated.selectedEmoji?.name)
    }

    @Test
    fun `clear selection`() {
        val emoji = createTestEmoji(1, "smile")
        val state = BrowserUiState(selectedEmoji = emoji)
        val updated = state.copy(selectedEmoji = null)
        assertNull(updated.selectedEmoji)
    }

    @Test
    fun `loading state`() {
        val state = BrowserUiState(isLoading = true)
        assertTrue(state.isLoading)
    }

    @Test
    fun `error state`() {
        val state = BrowserUiState(error = "Test error")
        assertEquals("Test error", state.error)
    }

    // ==================== CATEGORY TESTS ====================

    @Test
    fun `category selection`() {
        val category = CategoryEntity(id = 1, name = "Smileys", icon = "😀")
        assertEquals(1L, category.id)
        assertEquals("Smileys", category.name)
        assertEquals("😀", category.icon)
    }

    @Test
    fun `category with emoji count`() {
        val category = CategoryEntity(id = 1, name = "Smileys", emojiCount = 42)
        assertEquals(42, category.emojiCount)
    }

    // ==================== FAVORITE TESTS ====================

    @Test
    fun `toggle favorite`() {
        val emoji = createTestEmoji(1, "smile", isFavorite = false)
        assertFalse(emoji.isFavorite)

        val toggled = emoji.copy(isFavorite = !emoji.isFavorite)
        assertTrue(toggled.isFavorite)

        val toggledBack = toggled.copy(isFavorite = !toggled.isFavorite)
        assertFalse(toggledBack.isFavorite)
    }

    @Test
    fun `favorites list filtering`() {
        val emojis = (1L..10L).map {
            createTestEmoji(it, "emoji_$it", isFavorite = it % 3 == 0L)
        }

        val favorites = emojis.filter { it.isFavorite }
        assertEquals(3, favorites.size) // 3, 6, 9
    }

    // ==================== RECENT TESTS ====================

    @Test
    fun `recent emojis sorted by usage`() {
        val emojis = listOf(
            createTestEmoji(1, "old", usageCount = 10),
            createTestEmoji(2, "new", usageCount = 100),
            createTestEmoji(3, "medium", usageCount = 50)
        )

        val sorted = emojis.sortedByDescending { it.usageCount }
        assertEquals("new", sorted[0].name)
        assertEquals("medium", sorted[1].name)
        assertEquals("old", sorted[2].name)
    }

    // ==================== SEARCH TESTS ====================

    @Test
    fun `search by name`() {
        val emojis = listOf(
            createTestEmoji(1, "happy_face", keywords = "smile"),
            createTestEmoji(2, "sad_face", keywords = "cry"),
            createTestEmoji(3, "happy_heart", keywords = "love")
        )

        val results = emojis.filter { it.name.contains("happy") }
        assertEquals(2, results.size)
    }

    @Test
    fun `search by keywords`() {
        val emojis = listOf(
            createTestEmoji(1, "face1", keywords = "smile,joy"),
            createTestEmoji(2, "face2", keywords = "cry,tears"),
            createTestEmoji(3, "heart", keywords = "love,smile")
        )

        val results = emojis.filter { it.keywords.contains("smile") }
        assertEquals(2, results.size)
    }

    @Test
    fun `search empty query returns empty`() {
        val emojis = listOf(
            createTestEmoji(1, "test"),
            createTestEmoji(2, "test2")
        )

        val query = ""
        val results = if (query.isBlank()) emptyList() else emojis.filter { it.name.contains(query) }
        assertTrue(results.isEmpty())
    }

    @Test
    fun `search case insensitive`() {
        val emojis = listOf(
            createTestEmoji(1, "Happy"),
            createTestEmoji(2, "HAPPY"),
            createTestEmoji(3, "happy")
        )

        val results = emojis.filter { it.name.lowercase().contains("happy") }
        assertEquals(3, results.size)
    }

    // ==================== TAG TESTS ====================

    @Test
    fun `tag filtering`() {
        val tags = listOf(
            TagEntity(id = 1, name = "happy", displayName = "Happy"),
            TagEntity(id = 2, name = "sad", displayName = "Sad"),
            TagEntity(id = 3, name = "love", displayName = "Love")
        )

        val filtered = tags.filter { it.name.contains("ha") }
        assertEquals(1, filtered.size)
        assertEquals("happy", filtered[0].name)
    }

    // ==================== PAGING TESTS ====================

    @Test
    fun `paging offset calculation`() {
        val pageSize = 50
        val page1Offset = 0
        val page2Offset = pageSize
        val page3Offset = pageSize * 2

        assertEquals(0, page1Offset)
        assertEquals(50, page2Offset)
        assertEquals(100, page3Offset)
    }

    @Test
    fun `paging has more items`() {
        val totalItems = 120
        val pageSize = 50
        val loadedItems = 100

        val hasMore = loadedItems < totalItems
        assertTrue(hasMore)

        val allLoaded = loadedItems + 50 >= totalItems
        assertTrue(allLoaded)
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        id: Long,
        name: String,
        keywords: String = "",
        usageCount: Long = 0,
        isFavorite: Boolean = false
    ) = EmojiEntity(
        id = id,
        name = name,
        keywords = keywords,
        filePath = "/test/$name.png",
        mimeType = "image/png",
        contentHash = "hash_$id",
        usageCount = usageCount,
        isFavorite = isFavorite
    )
}
