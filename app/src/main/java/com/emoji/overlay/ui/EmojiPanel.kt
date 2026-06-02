package com.emoji.overlay.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.util.Log
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.R
import com.emoji.overlay.browser.ui.LongPressPreview
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.repository.EmojiRepositoryHolder
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.performance.ThumbnailBitmapCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EmojiPanel(
    visible: Boolean,
    panelHeightPx: Int,
    onPanelDragStart: () -> Float,
    onPanelOffsetChange: (Float) -> Unit,
    onDragEnd: (velocityY: Float) -> Unit,
    onEmojiSelected: (EmojiEntity) -> Unit = {}
) {
    if (!visible) return

    EmojiPanelContent(
        panelHeightPx = panelHeightPx,
        onPanelDragStart = onPanelDragStart,
        onPanelOffsetChange = onPanelOffsetChange,
        onDragEnd = onDragEnd,
        onEmojiSelected = onEmojiSelected
    )
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalFoundationApi::class)
@Composable
private fun EmojiPanelContent(
    panelHeightPx: Int,
    onPanelDragStart: () -> Float,
    onPanelOffsetChange: (Float) -> Unit,
    onDragEnd: (velocityY: Float) -> Unit,
    onEmojiSelected: (EmojiEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val resourceManager = remember(context) { ResourceManager.getInstance(context.applicationContext) }
    val density = LocalDensity.current
    val panelHeightDp = with(density) { panelHeightPx.toDp() }

    val repositoryState = produceState<EmojiRepository?>(initialValue = null, context) {
        value = try {
            withContext(Dispatchers.IO) {
                EmojiRepositoryHolder.getRepository(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e("EmojiPanel", "Failed to initialize repository", e)
            null
        }
    }
    val repository = repositoryState.value

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var previewEmoji by remember { mutableStateOf<EmojiEntity?>(null) }

    val categoriesFlow = remember(repository, selectedTab) {
        if (repository != null && selectedTab == 3) {
            repository.getAllCategoriesFlow()
        } else {
            flowOf(emptyList())
        }
    }
    val categories by categoriesFlow.collectAsState(initial = emptyList())

    val activeEmojisFlow: Flow<List<EmojiEntity>> = remember(repository, selectedTab, selectedCategoryId) {
        val repo = repository ?: return@remember flowOf(emptyList())
        when (selectedTab) {
            0 -> repo.getAllEmojisFlow()
            1 -> repo.getRecentEmojisFlow(limit = 32)
            2 -> repo.getFavoritesFlow()
            3 -> selectedCategoryId?.let { repo.getEmojisByCategoryFlow(it) } ?: flowOf(emptyList())
            else -> flowOf(emptyList())
        }
    }
    val emojis by activeEmojisFlow.collectAsState(initial = emptyList())

    val emojiCountFlow = remember(repository, selectedTab, selectedCategoryId) {
        if (repository != null && selectedTab != 3 && selectedCategoryId == null) {
            repository.getEmojiCountFlow()
        } else {
            flowOf(0)
        }
    }
    val emojiCount by emojiCountFlow.collectAsState(initial = 0)

    val selectedCategory = categories.find { it.id == selectedCategoryId }

    val tabAll = stringResource(R.string.overlay_tab_all)
    val tabRecent = stringResource(R.string.overlay_tab_recent)
    val tabFavorites = stringResource(R.string.overlay_tab_favorites)
    val tabCategories = stringResource(R.string.overlay_tab_categories)
    val tabs = listOf(tabAll, tabRecent, tabFavorites, tabCategories)

    val footerText = when {
        selectedTab == 3 && selectedCategoryId == null -> stringResource(R.string.overlay_pick_category)
        selectedTab == 3 && selectedCategory != null -> stringResource(
            R.string.overlay_filtering_category,
            selectedCategory.name
        )
        else -> stringResource(R.string.overlay_emoji_total, emojiCount)
    }

    val emptyMessage = when {
        selectedTab == 3 && selectedCategoryId == null -> stringResource(R.string.overlay_pick_category)
        selectedTab == 3 && selectedCategoryId != null -> stringResource(R.string.overlay_category_empty)
        selectedTab == 0 -> stringResource(R.string.overlay_empty_all)
        selectedTab == 1 -> stringResource(R.string.overlay_empty_recent)
        selectedTab == 2 -> stringResource(R.string.overlay_empty_favorites)
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeightDp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        PanelDragHandle(
            panelHeightPx = panelHeightPx,
            onPanelDragStart = onPanelDragStart,
            onPanelOffsetChange = onPanelOffsetChange,
            onDragEnd = onDragEnd
        )

        if (selectedTab == 3 && selectedCategoryId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedCategoryId = null }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.overlay_back_categories)
                    )
                }
                Text(
                    text = "${selectedCategory?.icon.orEmpty()} ${selectedCategory?.name.orEmpty()}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF5F5F5),
                contentColor = Color(0xFF6750A4),
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            selectedCategoryId = null
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when {
            selectedTab == 3 && selectedCategoryId == null -> {
                CategoryListPane(
                    categories = categories,
                    modifier = Modifier.weight(1f),
                    onCategoryClick = { category ->
                        if (category.emojiCount > 0) {
                            selectedCategoryId = category.id
                        }
                    }
                )
            }
            emojis.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📦", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = emptyMessage,
                            color = Color(0xFF999999),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 58.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(emojis, key = { "${it.id}_${it.filePath}" }) { emoji ->
                        EmojiGridItem(
                            emoji = emoji,
                            resourceManager = resourceManager,
                            onClick = { onEmojiSelected(emoji) },
                            onLongClick = { previewEmoji = emoji }
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFE8E8E8)
        ) {
            Text(
                text = footerText,
                fontSize = 12.sp,
                color = Color(0xFF888888),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }

    previewEmoji?.let { emoji ->
        LongPressPreview(
            emoji = emoji,
            isFavorite = emoji.isFavorite,
            onDismiss = { previewEmoji = null },
            onToggleFavorite = {
                repository?.let { repo ->
                    coroutineScope.launch(Dispatchers.IO) {
                        repo.toggleFavorite(emoji.id)
                    }
                }
            },
            onSelect = {
                onEmojiSelected(emoji)
                previewEmoji = null
            }
        )
    }
}

@Composable
private fun PanelDragHandle(
    panelHeightPx: Int,
    onPanelDragStart: () -> Float,
    onPanelOffsetChange: (Float) -> Unit,
    onDragEnd: (velocityY: Float) -> Unit
) {
    val velocityTracker = remember { VelocityTracker() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(panelHeightPx) {
                var dragStartOffset = 0f
                var totalDragDelta = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        dragStartOffset = onPanelDragStart()
                        totalDragDelta = 0f
                        velocityTracker.resetTracking()
                    },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().y
                        onDragEnd(velocity)
                    },
                    onDragCancel = {
                        onDragEnd(0f)
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        totalDragDelta += dragAmount
                        onPanelOffsetChange(
                            (dragStartOffset + totalDragDelta).coerceIn(0f, panelHeightPx.toFloat())
                        )
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFCCCCCC))
        )
    }
}

@Composable
private fun CategoryListPane(
    categories: List<CategoryEntity>,
    modifier: Modifier = Modifier,
    onCategoryClick: (CategoryEntity) -> Unit
) {
    val visibleCategories = categories.filter { it.isVisible }
    if (visibleCategories.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.overlay_pick_category),
                color = Color(0xFF999999),
                fontSize = 14.sp
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(visibleCategories, key = { it.id }) { category ->
                val enabled = category.emojiCount > 0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { onCategoryClick(category) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (enabled) Color.White else Color(0xFFEEEEEE)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = category.icon, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = if (enabled) Color(0xFF333333) else Color(0xFFAAAAAA)
                            )
                            Text(
                                text = stringResource(
                                    R.string.emoji_count_in_category,
                                    category.emojiCount
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }
                        if (enabled) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFCCCCCC)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiGridItem(
    emoji: EmojiEntity,
    resourceManager: ResourceManager,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cacheKey = "${emoji.id}_${emoji.thumbPath ?: emoji.filePath}"
    val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = cacheKey
    ) {
        value = withContext(Dispatchers.IO) {
            val previewPath = emoji.thumbPath ?: emoji.filePath
            val previewFile = resourceManager.getFile(previewPath)
            if (!previewFile.exists()) {
                null
            } else {
                ThumbnailBitmapCache.loadGridThumbnail(cacheKey, previewFile.absolutePath, targetMaxPx = 240)
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = emoji.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = emoji.name.take(4),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (emoji.isFavorite) {
            Text(
                text = "♥",
                fontSize = 9.sp,
                color = Color(0xFFE53935),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            )
        }
    }
}
