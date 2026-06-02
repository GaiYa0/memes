package com.emoji.overlay.import.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.import.model.*

/**
 * Import preview screen.
 *
 * Shows scanned files in a grid with selection controls.
 * User must explicitly select files before import begins.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    session: ImportSession,
    filter: ImportFilter,
    onToggleSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSelectGifOnly: () -> Unit,
    onSelectImagesOnly: () -> Unit,
    onExcludeDuplicates: () -> Unit,
    onStartImport: () -> Unit,
    onCancel: () -> Unit,
    onFilterChanged: (ImportFilter) -> Unit
) {
    val filteredItems = remember(session.items, filter) {
        session.items.filter { filter.matches(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入预览") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Filter menu
                    var showFilterMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, "筛选")
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("仅显示 GIF") },
                            onClick = {
                                onFilterChanged(filter.copy(showGifOnly = !filter.showGifOnly))
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (filter.showGifOnly) Icon(Icons.Default.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("仅显示图片") },
                            onClick = {
                                onFilterChanged(filter.copy(showImagesOnly = !filter.showImagesOnly))
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (filter.showImagesOnly) Icon(Icons.Default.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("隐藏重复") },
                            onClick = {
                                onFilterChanged(filter.copy(excludeDuplicates = !filter.excludeDuplicates))
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (filter.excludeDuplicates) Icon(Icons.Default.Check, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("隐藏损坏") },
                            onClick = {
                                onFilterChanged(filter.copy(excludeCorrupted = !filter.excludeCorrupted))
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (filter.excludeCorrupted) Icon(Icons.Default.Check, null)
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            ImportBottomBar(
                session = session,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                onSelectGifOnly = onSelectGifOnly,
                onSelectImagesOnly = onSelectImagesOnly,
                onExcludeDuplicates = onExcludeDuplicates,
                onStartImport = onStartImport
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Statistics bar
            ImportStatisticsBar(session = session)

            // Item grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ImportPreviewItem(
                        item = item,
                        onToggle = { onToggleSelection(item.id) }
                    )
                }
            }
        }
    }
}

/**
 * Statistics bar showing import counts.
 */
@Composable
private fun ImportStatisticsBar(session: ImportSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "发现", value = session.totalCount, color = MaterialTheme.colorScheme.primary)
            StatItem(label = "已选", value = session.selectedCount, color = MaterialTheme.colorScheme.tertiary)
            StatItem(label = "重复", value = session.duplicateCount, color = MaterialTheme.colorScheme.error)
            StatItem(label = "损坏", value = session.corruptedCount, color = MaterialTheme.colorScheme.error)
            StatItem(label = "预计", value = session.formattedSelectedSize, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun StatItem(label: String, value: Any, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Single preview item in the grid.
 */
@Composable
private fun ImportPreviewItem(
    item: ImportPreviewItem,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (item.isSelected) 3.dp else 1.dp,
                color = when {
                    item.isSelected -> MaterialTheme.colorScheme.primary
                    item.isDuplicate -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    item.isCorrupted -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !item.isDuplicate && !item.isCorrupted) { onToggle() }
    ) {
        // Thumbnail placeholder (in production, load actual thumbnail)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    item.isGif -> Icons.Default.PlayCircle
                    item.isWebp -> Icons.Default.Image
                    else -> Icons.Default.Image
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        // Format badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .background(
                    color = if (item.isGif) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = item.extension.uppercase(),
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Status badges
        if (item.isDuplicate) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
                    .size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "重复",
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }

        if (item.isCorrupted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
                    .size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "损坏",
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }

        // Selection checkbox
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp),
            enabled = !item.isDuplicate && !item.isCorrupted
        )

        // File info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(4.dp)
        ) {
            Text(
                text = item.fileName,
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.formattedSize,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Bottom action bar with import controls.
 */
@Composable
private fun ImportBottomBar(
    session: ImportSession,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSelectGifOnly: () -> Unit,
    onSelectImagesOnly: () -> Unit,
    onExcludeDuplicates: () -> Unit,
    onStartImport: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Quick action buttons - Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                    Text("全选", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onDeselectAll, modifier = Modifier.weight(1f)) {
                    Text("取消全选", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onExcludeDuplicates, modifier = Modifier.weight(1f)) {
                    Text("排除重复", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick action buttons - Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onSelectGifOnly, modifier = Modifier.weight(1f)) {
                    Text("仅GIF", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onSelectImagesOnly, modifier = Modifier.weight(1f)) {
                    Text("仅图片", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Import button
            Button(
                onClick = onStartImport,
                modifier = Modifier.fillMaxWidth(),
                enabled = session.selectedCount > 0 && session.state == ImportState.PREVIEW
            ) {
                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始导入 (${session.selectedCount})")
            }
        }
    }
}
