package com.emoji.overlay.browser.ui

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.R
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.util.ResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Long press preview dialog.
 *
 * Shows:
 * - Full size image preview
 * - Pinch to zoom
 * - Favorite toggle
 * - Emoji info
 * - Animated GIF/WebP support
 */
@Composable
fun LongPressPreview(
    emoji: EmojiEntity,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelect: () -> Unit,
    onAddToCategory: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val resourceManager = remember(context) { ResourceManager.getInstance(context.applicationContext) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = emoji.id,
        key2 = emoji.filePath,
        key3 = emoji.thumbPath
    ) {
        value = withContext(Dispatchers.IO) {
            val previewPath = if (emoji.mimeType == "image/gif") {
                emoji.filePath
            } else {
                emoji.thumbPath ?: emoji.filePath
            }
            val previewFile = resourceManager.getFile(previewPath)
            if (!previewFile.exists()) {
                null
            } else {
                decodeSampledBitmap(previewFile.absolutePath, 1080)?.asImageBitmap()
            }
        }
    }
    val gifFilePath = remember(emoji.filePath) {
        resourceManager.getFile(emoji.filePath).absolutePath
    }
    val supportsAnimatedGif = emoji.mimeType == "image/gif" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val gifDrawable by produceState<Drawable?>(
        initialValue = null,
        key1 = emoji.id,
        key2 = gifFilePath,
        key3 = emoji.mimeType
    ) {
        if (!supportsAnimatedGif) {
            value = null
        } else {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val source = ImageDecoder.createSource(java.io.File(gifFilePath))
                    ImageDecoder.decodeDrawable(source)
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Image preview
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 3f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (supportsAnimatedGif) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    adjustViewBounds = true
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { imageView ->
                                imageView.setImageDrawable(gifDrawable)
                                (gifDrawable as? AnimatedImageDrawable)?.start()
                            }
                        )
                    } else if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = emoji.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = emoji.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top bar with favorite and close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.preview_close),
                            tint = Color.White
                        )
                    }

                    // Favorite button
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.preview_unfavorite else R.string.preview_favorite
                            ),
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White
                        )
                    }
                }

                // Bottom info bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = emoji.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (emoji.keywords.isNotEmpty()) {
                        Text(
                            text = emoji.keywords,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${emoji.width}×${emoji.height}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatFileSize(emoji.fileSize),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = emoji.mimeType.substringAfter("image/").uppercase(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (onAddToCategory == null) {
                        Button(
                            onClick = onSelect,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.select_this_emoji))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAddToCategory,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.add_to_category))
                            }
                            Button(
                                onClick = onSelect,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.select_this_emoji))
                            }
                        }
                    }
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.delete_emoji))
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_emoji_confirm_title)) },
            text = {
                Text(stringResource(R.string.delete_emoji_confirm_message, emoji.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    }
                ) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun decodeSampledBitmap(path: String, targetMaxPx: Int): android.graphics.Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, boundsOptions)
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return null

    var sampleSize = 1
    val halfWidth = width / 2
    val halfHeight = height / 2
    while (halfWidth / sampleSize >= targetMaxPx && halfHeight / sampleSize >= targetMaxPx) {
        sampleSize *= 2
    }
    sampleSize = max(1, sampleSize)

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeFile(path, decodeOptions)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${bytes / (1024 * 1024)}MB"
    }
}
