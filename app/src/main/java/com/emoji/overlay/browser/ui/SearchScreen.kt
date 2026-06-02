package com.emoji.overlay.browser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.entity.TagEntity

/**
 * Search screen with real-time results.
 *
 * Supports:
 * - Name/keyword search
 * - Tag search
 * - Category filtering
 * - Real-time debounced search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: List<EmojiEntity>,
    tags: List<TagEntity>,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onEmojiClick: (EmojiEntity) -> Unit,
    onEmojiLongPress: (EmojiEntity) -> Unit,
    onTagClick: (TagEntity) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SearchBar(
                        query = query,
                        onQueryChange = onQueryChange,
                        placeholder = { Text("搜索表情、标签...") },
                        onSearch = {},
                        active = false,
                        onActiveChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, "清除")
                                }
                            }
                        }
                    ) {}
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tags section
            if (tags.isNotEmpty() && query.isBlank()) {
                Text(
                    text = "热门标签",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags.take(20)) { tag ->
                        AssistChip(
                            onClick = { onTagClick(tag) },
                            label = { Text(tag.displayName) }
                        )
                    }
                }
            }

            // Results
            if (query.isNotBlank()) {
                Text(
                    text = "搜索结果 (${results.size})",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "没有找到匹配的表情",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(80.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(results, key = { it.id }) { emoji ->
                            SearchResultItem(
                                emoji = emoji,
                                onClick = { onEmojiClick(emoji) },
                                onLongClick = { onEmojiLongPress(emoji) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    emoji: EmojiEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji.name.take(3),
                fontSize = 16.sp
            )

            if (emoji.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "收藏",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .padding(1.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
