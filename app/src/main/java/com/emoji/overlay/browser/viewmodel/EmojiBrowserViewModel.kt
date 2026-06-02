package com.emoji.overlay.browser.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emoji.overlay.browser.usecase.*
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.TagEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the emoji browser.
 *
 * Manages UI state for:
 * - Home screen (recent + favorites + categories)
 * - Category browsing
 * - Favorites
 * - Recent usage
 * - Search
 * - Long press preview
 */
class EmojiBrowserViewModel(
    private val categoryUseCase: EmojiCategoryUseCase,
    private val recentUseCase: RecentUseCase,
    private val favoriteUseCase: FavoriteUseCase,
    private val searchUseCase: SearchUseCase
) : ViewModel() {

    // ==================== UI STATE ====================

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedCategory: StateFlow<CategoryEntity?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _previewEmoji = MutableStateFlow<EmojiEntity?>(null)
    val previewEmoji: StateFlow<EmojiEntity?> = _previewEmoji.asStateFlow()

    // ==================== DATA FLOWS ====================

    val categories: StateFlow<List<CategoryEntity>> = categoryUseCase.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentEmojis: StateFlow<List<EmojiEntity>> = recentUseCase.getRecentEmojisFlow(limit = 50)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteEmojis: StateFlow<List<EmojiEntity>> = favoriteUseCase.getFavoritesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val categoryEmojis: StateFlow<List<EmojiEntity>> = _selectedCategory
        .filterNotNull()
        .flatMapLatest { category ->
            categoryUseCase.getEmojisByCategoryFlow(category.id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val searchResults: StateFlow<List<EmojiEntity>> = _searchQuery
        .debounce(300)
        .filter { it.isNotBlank() }
        .flatMapLatest { query ->
            searchUseCase.searchFlow(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ==================== ACTIONS ====================

    fun selectCategory(category: CategoryEntity) {
        _selectedCategory.value = category
        _uiState.update { it.copy(currentScreen = BrowserScreen.CATEGORY) }
    }

    fun clearCategory() {
        _selectedCategory.value = null
        _uiState.update { it.copy(currentScreen = BrowserScreen.HOME) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _uiState.update { it.copy(currentScreen = BrowserScreen.SEARCH) }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { it.copy(currentScreen = BrowserScreen.HOME) }
    }

    fun navigateTo(screen: BrowserScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun toggleFavorite(emojiId: Long) {
        viewModelScope.launch {
            favoriteUseCase.toggleFavorite(emojiId)
        }
    }

    fun recordUsage(emojiId: Long) {
        viewModelScope.launch {
            recentUseCase.recordUsage(emojiId)
        }
    }

    fun showPreview(emoji: EmojiEntity) {
        _previewEmoji.value = emoji
    }

    fun dismissPreview() {
        _previewEmoji.value = null
    }

    fun selectEmoji(emoji: EmojiEntity) {
        recordUsage(emoji.id)
        _uiState.update { it.copy(selectedEmoji = emoji) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedEmoji = null) }
    }
}

/**
 * UI state for the browser.
 */
data class BrowserUiState(
    val currentScreen: BrowserScreen = BrowserScreen.HOME,
    val selectedEmoji: EmojiEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Navigation screens.
 */
enum class BrowserScreen {
    HOME,
    CATEGORY,
    FAVORITES,
    RECENT,
    SEARCH
}
