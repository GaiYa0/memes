package com.emoji.overlay

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.R
import com.emoji.overlay.browser.ui.LongPressPreview
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity

@Composable
fun BrowseScreen(
    viewModel: MainViewModel,
    onEmojiClick: (EmojiEntity) -> Unit
) {
    val emojis by viewModel.getAllEmojis().collectAsState(initial = emptyList())
    EmojiGridWithPreview(
        viewModel = viewModel,
        emojis = emojis,
        emptyMessage = "还没有表情\n点击底部「导入」添加",
        onEmojiClick = onEmojiClick
    )
}

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onEmojiClick: (EmojiEntity) -> Unit
) {
    val emojis by viewModel.getFavoriteEmojis().collectAsState(initial = emptyList())
    EmojiGridWithPreview(
        viewModel = viewModel,
        emojis = emojis,
        emptyMessage = "还没有收藏表情\n浏览表情时点击 ♥ 收藏",
        onEmojiClick = onEmojiClick
    )
}

@Composable
fun RecentScreen(
    viewModel: MainViewModel,
    onEmojiClick: (EmojiEntity) -> Unit
) {
    val context = LocalContext.current
    val emojis by viewModel.getRecentEmojis().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
    ) {
        if (emojis.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    try {
                        viewModel.clearHistory()
                        Toast.makeText(context, "已清除使用记录", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Clear history failed", e)
                    }
                }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清除记录")
                }
            }
        }

        EmojiGridWithPreview(
            viewModel = viewModel,
            modifier = Modifier.weight(1f),
            emojis = emojis,
            emptyMessage = "还没有使用记录\n选择表情后会自动记录",
            onEmojiClick = onEmojiClick
        )
    }
}

@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onCategoryClick: (CategoryEntity) -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.getCategories().collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CategoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("新增分类")
            }
        }

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📁", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("还没有分类", fontSize = 16.sp, color = Color(0xFF888888))
                    Text("点击右上角创建分类", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onCategoryClick(category) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = category.icon, fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(category.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text(
                                        "${category.emojiCount} 个表情",
                                        fontSize = 13.sp,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }

                            IconButton(onClick = { onCategoryClick(category) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "进入分类", tint = Color(0xFFCCCCCC))
                            }

                            if (!category.isSystem) {
                                IconButton(onClick = { renameTarget = category }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.rename_category),
                                        tint = Color(0xFF666666)
                                    )
                                }
                            }
                            IconButton(onClick = { deleteTarget = category }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_category),
                                    tint = Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CategoryEditorDialog(
            title = "新增分类",
            initialName = "",
            initialIcon = "📁",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, icon ->
                viewModel.createCategory(context, name, icon)
                showCreateDialog = false
            }
        )
    }

    renameTarget?.let { category ->
        CategoryEditorDialog(
            title = "重命名分类",
            initialName = category.name,
            initialIcon = category.icon,
            onDismiss = { renameTarget = null },
            onConfirm = { name, icon ->
                viewModel.renameCategory(context, category, name, icon)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { category ->
        val message = if (category.isSystem) {
            stringResource(
                R.string.delete_category_system_confirm,
                category.name,
                category.emojiCount
            )
        } else {
            stringResource(
                R.string.delete_category_confirm,
                category.name,
                category.emojiCount
            )
        }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_category_confirm_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(context, category.id)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.delete_category), color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var icon by remember(initialIcon) { mutableStateOf(initialIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("图标（emoji）") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), icon.trim().ifBlank { "📁" }) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun CategoryDetailScreen(
    viewModel: MainViewModel,
    categoryId: Long,
    onEmojiClick: (EmojiEntity) -> Unit
) {
    val categories by viewModel.getCategories().collectAsState(initial = emptyList())
    val categoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "未分类"
    val emojis by viewModel.getEmojisByCategory(categoryId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
    ) {
        Text(
            text = "分类：$categoryName",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF5E5873)
        )

        EmojiGridWithPreview(
            viewModel = viewModel,
            modifier = Modifier.weight(1f),
            emojis = emojis,
            emptyMessage = "该分类下还没有表情",
            onEmojiClick = onEmojiClick
        )
    }
}

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onEmojiClick: (EmojiEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchEmojis(query).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("搜索表情名称或关键词...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (query.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("输入关键词搜索表情", fontSize = 16.sp, color = Color(0xFF888888))
                }
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😅", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("没有找到匹配的表情", fontSize = 16.sp, color = Color(0xFF888888))
                }
            }
        } else {
            EmojiGridWithPreview(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                emojis = results,
                emptyMessage = "没有找到匹配的表情",
                onEmojiClick = onEmojiClick
            )
        }
    }
}

@Composable
private fun EmojiGridWithPreview(
    viewModel: MainViewModel,
    emojis: List<EmojiEntity>,
    emptyMessage: String,
    onEmojiClick: (EmojiEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.getCategories().collectAsState(initial = emptyList())
    var previewEmoji by remember { mutableStateOf<EmojiEntity?>(null) }
    var categoryTarget by remember { mutableStateOf<EmojiEntity?>(null) }

    Box(modifier = modifier) {
        EmojiGridScreen(
            modifier = Modifier.fillMaxSize(),
            emojis = emojis,
            emptyMessage = emptyMessage,
            onEmojiClick = onEmojiClick,
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onEmojiLongPress = { previewEmoji = it }
        )
    }

    previewEmoji?.let { emoji ->
        LongPressPreview(
            emoji = emoji,
            isFavorite = emoji.isFavorite,
            onDismiss = { previewEmoji = null },
            onToggleFavorite = { viewModel.toggleFavorite(emoji.id) },
            onSelect = {
                onEmojiClick(emoji)
                previewEmoji = null
            },
            onAddToCategory = {
                categoryTarget = emoji
                previewEmoji = null
            },
            onDelete = {
                viewModel.deleteEmoji(emoji.id)
                previewEmoji = null
            }
        )
    }

    categoryTarget?.let { emoji ->
        CategoryPickerDialog(
            categories = categories,
            onDismiss = { categoryTarget = null },
            onSelectCategory = { categoryId ->
                viewModel.moveEmojiToCategory(context, emoji.id, categoryId)
                categoryTarget = null
            }
        )
    }
}

@Composable
private fun CategoryPickerDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSelectCategory: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.category_picker_hint))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        TextButton(
                            onClick = { onSelectCategory(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.move_to_uncategorized))
                        }
                    }
                    items(categories, key = { it.id }) { category ->
                        TextButton(
                            onClick = { onSelectCategory(category.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${category.icon} ${category.name}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    )
}
