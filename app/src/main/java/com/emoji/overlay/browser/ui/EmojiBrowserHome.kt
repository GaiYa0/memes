package com.emoji.overlay.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity

/**
 * Home screen for the emoji browser.
 *
 * Shows:
 * - Search bar
 * - Recent emojis (horizontal scroll)
 * - Favorites (horizontal scroll)
 * - Category grid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiBrowserHome(
    recentEmojis: List<EmojiEntity>,
    favoriteEmojis: List<EmojiEntity>,
    categories: List<CategoryEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryClick: (CategoryEntity) -> Unit,
    onEmojiClick: (EmojiEntity) -> Unit,
    onEmojiLongPress: (EmojiEntity) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToRecent: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        item {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = { Text("搜索表情...") },
                onSearch = {},
                active = false,
                onActiveChange = {},
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, "清除")
                        }
                    }
                }
            ) {}
        }

        // Recent emojis section
        if (recentEmojis.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "最近使用",
                    actionText = "查看全部",
                    onAction = onNavigateToRecent
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentEmojis.take(20), key = { it.id }) { emoji ->
                        EmojiChip(
                            emoji = emoji,
                            onClick = { onEmojiClick(emoji) },
                            onLongClick = { onEmojiLongPress(emoji) }
                        )
                    }
                }
            }
        }

        // Favorites section
        if (favoriteEmojis.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "收藏表情",
                    actionText = "查看全部",
                    onAction = onNavigateToFavorites
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favoriteEmojis.take(20), key = { it.id }) { emoji ->
                        EmojiChip(
                            emoji = emoji,
                            onClick = { onEmojiClick(emoji) },
                            onLongClick = { onEmojiLongPress(emoji) }
                        )
                    }
                }
            }
        }

        // Categories section
        item {
            SectionHeader(title = "分类")
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun EmojiChip(
    emoji: EmojiEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder - in production, load thumbnail
            Text(
                text = emoji.name.take(2),
                fontSize = 20.sp
            )

            // Favorite indicator
            if (emoji.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "收藏",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .padding(2.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.icon,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = category.name,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${category.emojiCount} 个表情",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
