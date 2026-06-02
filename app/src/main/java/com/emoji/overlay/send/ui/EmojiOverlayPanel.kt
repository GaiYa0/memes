package com.emoji.overlay.send.ui

import androidx.compose.animation.*
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
import com.emoji.overlay.browser.ui.LongPressPreview
import com.emoji.overlay.data.entity.EmojiEntity

/**
 * Emoji overlay panel composable.
 *
 * Shows:
 * - Quick access tabs (Recent, Favorites, Categories, Search)
 * - Emoji grid
 * - Long press preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiOverlayPanel(
    isVisible: Boolean,
    recentEmojis: List<EmojiEntity>,
    favoriteEmojis: List<EmojiEntity>,
    selectedEmoji: EmojiEntity?,
    isFavorite: Boolean,
    onEmojiClick: (EmojiEntity) -> Unit,
    onEmojiLongPress: (EmojiEntity) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onDismissPreview: () -> Unit,
    onSelect: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar with tabs
                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("最近", "收藏", "分类", "搜索")

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Emoji grid
                val emojis = when (selectedTab) {
                    0 -> recentEmojis
                    1 -> favoriteEmojis
                    2 -> recentEmojis // Placeholder for categories
                    3 -> recentEmojis // Placeholder for search
                    else -> recentEmojis
                }

                if (emojis.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EmojiEmotions,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "还没有使用记录"
                                    1 -> "还没有收藏表情"
                                    2 -> "请从导入中心添加表情"
                                    3 -> "输入关键词搜索"
                                    else -> ""
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(64.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(emojis, key = { it.id }) { emoji ->
                            EmojiGridItem(
                                emoji = emoji,
                                onClick = { onEmojiClick(emoji) },
                                onLongClick = { onEmojiLongPress(emoji) }
                            )
                        }
                    }
                }
            }
        }

        // Long press preview overlay
        if (selectedEmoji != null) {
            LongPressPreview(
                emoji = selectedEmoji,
                isFavorite = isFavorite,
                onDismiss = onDismissPreview,
                onToggleFavorite = { onToggleFavorite(selectedEmoji.id) },
                onSelect = onSelect
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiGridItem(
    emoji: EmojiEntity,
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
        shape = RoundedCornerShape(8.dp)
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

            // Favorite indicator
            if (emoji.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "收藏",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .padding(1.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
