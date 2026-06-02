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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity

/**
 * Category browsing screen.
 *
 * Shows emojis in a grid with:
 * - Lazy loading
 * - Long press preview
 * - Favorite toggle
 * - Multi-select support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    category: CategoryEntity,
    emojis: List<EmojiEntity>,
    onBack: () -> Unit,
    onEmojiClick: (EmojiEntity) -> Unit,
    onEmojiLongPress: (EmojiEntity) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(category.name)
                        Text(
                            text = "${emojis.size} 个表情",
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
                            selectedIds.forEach { onToggleFavorite(it) }
                            isMultiSelectMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Favorite, "收藏选中")
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(emojis, key = { it.id }) { emoji ->
                EmojiGridItem(
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
                    onLongClick = {
                        if (!isMultiSelectMode) {
                            onEmojiLongPress(emoji)
                        }
                    },
                    onToggleFavorite = { onToggleFavorite(emoji.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiGridItem(
    emoji: EmojiEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
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
            // Thumbnail placeholder
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

            // Format badge
            if (emoji.isGif) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GIF", fontSize = 6.sp)
                }
            }

            // Favorite indicator
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

            // Selection checkbox
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

private val EmojiEntity.isGif: Boolean get() = mimeType == "image/gif"
