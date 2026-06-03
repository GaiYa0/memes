package com.emoji.overlay.send.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import com.emoji.overlay.send.ResolvedShareTarget
import com.emoji.overlay.send.ShareTargetStore
import com.emoji.overlay.send.ShareTargetResolver

enum class ShareSheetPresentation {
    Modal,
    OverlayEmbedded
}

@Composable
fun rememberResolvedShareTargets(
    visible: Boolean,
    mimeType: String,
    shareUri: Uri
): List<ResolvedShareTarget> {
    val context = LocalContext.current
    var targets by remember { mutableStateOf(emptyList<ResolvedShareTarget>()) }
    LaunchedEffect(visible, mimeType, shareUri) {
        if (visible) {
            val store = ShareTargetStore.getInstance(context)
            targets = ShareTargetResolver.resolveDisplayTargetsForContext(
                context = context,
                mimeType = mimeType,
                customSlot4Package = store.loadCustomSlot4Package(),
                streamUri = shareUri
            )
        }
    }
    return targets
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTargetBottomSheet(
    visible: Boolean,
    mimeType: String,
    shareUri: Uri,
    title: String,
    onTargetClick: (ResolvedShareTarget) -> Unit,
    onMoreClick: () -> Unit,
    onDismiss: () -> Unit,
    presentation: ShareSheetPresentation = ShareSheetPresentation.Modal
) {
    if (!visible) return

    val targets = rememberResolvedShareTargets(visible = true, mimeType = mimeType, shareUri = shareUri)

    when (presentation) {
        ShareSheetPresentation.Modal -> {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ShareTargetSheetContent(
                    title = title,
                    targets = targets,
                    onTargetClick = onTargetClick,
                    onMoreClick = onMoreClick,
                    onDismiss = onDismiss,
                    showDragHandle = false
                )
            }
        }
        ShareSheetPresentation.OverlayEmbedded -> {
            ShareTargetOverlayEmbeddedSheet(
                title = title,
                targets = targets,
                onTargetClick = onTargetClick,
                onMoreClick = onMoreClick,
                onDismiss = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareTargetOverlayEmbeddedSheet(
    title: String,
    targets: List<ResolvedShareTarget>,
    onTargetClick: (ResolvedShareTarget) -> Unit,
    onMoreClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            ShareTargetSheetContent(
                title = title,
                targets = targets,
                onTargetClick = onTargetClick,
                onMoreClick = onMoreClick,
                onDismiss = onDismiss,
                showDragHandle = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareTargetSheetContent(
    title: String,
    targets: List<ResolvedShareTarget>,
    onTargetClick: (ResolvedShareTarget) -> Unit,
    onMoreClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showDragHandle) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        Text(
            text = "分享",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        if (title.isNotBlank()) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        if (targets.isEmpty()) {
            Text(
                text = "暂无快捷分享，请使用更多应用",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(targets, key = { it.packageName }) { target ->
                    ShareTargetItem(
                        target = target,
                        onClick = { onTargetClick(target) }
                    )
                }
            }
        }

        Button(onClick = onMoreClick, modifier = Modifier.fillMaxWidth()) {
            Text("更多应用…")
        }
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("取消")
        }
    }
}

@Composable
private fun ShareTargetItem(
    target: ResolvedShareTarget,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ShareAppIcon(packageName = target.packageName)
        Text(
            text = target.label,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
