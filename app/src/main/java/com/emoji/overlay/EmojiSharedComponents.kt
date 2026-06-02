package com.emoji.overlay

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.performance.ThumbnailBitmapCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun EmojiGridScreen(
    modifier: Modifier = Modifier,
    emojis: List<EmojiEntity>,
    emptyMessage: String,
    onEmojiClick: (EmojiEntity) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onEmojiLongPress: (EmojiEntity) -> Unit = {}
) {
    if (emojis.isEmpty()) {
        Box(
            modifier = Modifier
                .then(modifier)
                .fillMaxSize()
                .background(Color(0xFFF8F5FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                fontSize = 16.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            modifier = modifier.fillMaxSize(),
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            items(emojis, key = { "${it.id}_${it.filePath}" }) { emoji ->
                EmojiCard(
                    emoji = emoji,
                    onClick = { onEmojiClick(emoji) },
                    onToggleFavorite = { onToggleFavorite(emoji.id) },
                    onLongClick = { onEmojiLongPress(emoji) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EmojiCard(
    emoji: EmojiEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val cacheKey = "${emoji.id}_${emoji.thumbPath ?: emoji.filePath}"
    val imageBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = cacheKey
    ) {
        value = withContext(Dispatchers.IO) {
            val manager = ResourceManager.getInstance(context.applicationContext)
            val previewPath = emoji.thumbPath ?: emoji.filePath
            val previewFile = manager.getFile(previewPath)
            if (!previewFile.exists()) {
                null
            } else {
                ThumbnailBitmapCache.loadGridThumbnail(cacheKey, previewFile.absolutePath, targetMaxPx = 320)
            }
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = emoji.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = emoji.name.take(6),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(6.dp),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                Icon(
                    if (emoji.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (emoji.isFavorite) Color(0xFFE53935) else Color(0xFFCCCCCC)
                )
            }
        }
    }
}
