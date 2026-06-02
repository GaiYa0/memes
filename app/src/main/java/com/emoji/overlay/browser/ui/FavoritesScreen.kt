package com.emoji.overlay.browser.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.data.entity.EmojiEntity

/**
 * Favorites screen showing all favorited emojis.
 *
 * Features:
 * - Grid layout
 * - Long press preview
 * - Remove from favorites
 * - Multi-select support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<EmojiEntity>,
    onBack: () -> Unit,
    onEmojiClick: (EmojiEntity) -> Unit,
    onEmojiLongPress: (EmojiEntity) -> Unit,
    onRemoveFromFavorites: (Long) -> Unit
) {
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("收藏表情")
                        Text(
                            text = "${favorites.size} 个表情",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        Text("${selectedIds.size} 已选")
                        IconButton(onClick = {
                            selectedIds.forEach { onRemoveFromFavorites(it) }
                            isMultiSelectMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.FavoriteBorder, "取消收藏")
                        }
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Close, "取消")
                        }
                    } else {
                        IconButton(onClick = { isMultiSelectMode = true }) {
                            Icon(Icons.Default.CheckCircle, "多选")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有收藏表情",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "长按表情可以添加收藏",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(80.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(favorites, key = { it.id }) { emoji ->
                    FavoriteGridItem(
                        emoji = emoji,
                        isSelected = emoji.id in selectedIds,
                        isMultiSelectMode = isMultiSelectMode,
                        onClick = {
                            if (isMultiSelectMode) {
                                if (emoji.id in selectedIds) selectedIds.remove(emoji.id)
                                else selectedIds.add(emoji.id)
                            } else {
                                onEmojiClick(emoji)
                            }
                        },
                        onLongClick = { onEmojiLongPress(emoji) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteGridItem(
    emoji: EmojiEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji.name.take(3),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite indicator (always shown in favorites)
            Icon(
                Icons.Default.Favorite,
                contentDescription = "收藏",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(14.dp)
                    .padding(1.dp),
                tint = MaterialTheme.colorScheme.error
            )

            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                )
            }
        }
    }
}
