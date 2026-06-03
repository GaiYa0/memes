package com.emoji.overlay.import.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.emoji.overlay.performance.loadMediaThumbnail
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.emoji.overlay.import.media.MediaAlbum
import com.emoji.overlay.import.media.MediaAlbumReader
import com.emoji.overlay.import.media.MediaImagePermissions
import com.emoji.overlay.import.media.MediaPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_SELECTION = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumImportScreen(
    onImportSelected: (List<Uri>) -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onUseFallbackPicker: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember { MediaAlbumReader(context.contentResolver) }

    var hasPermission by remember { mutableStateOf(MediaImagePermissions.hasPermission(context)) }
    var isLoading by remember { mutableStateOf(false) }
    var albums by remember { mutableStateOf<List<MediaAlbum>>(emptyList()) }
    var selectedAlbum by remember { mutableStateOf<MediaAlbum?>(null) }
    var photos by remember { mutableStateOf<List<MediaPhoto>>(emptyList()) }
    val selectedUris = remember { mutableStateListOf<Uri>() }

    fun reloadAlbums() {
        if (!MediaImagePermissions.hasPermission(context)) {
            hasPermission = false
            albums = emptyList()
            return
        }
        hasPermission = true
        isLoading = true
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { reader.loadAlbums() }
            albums = loaded
            isLoading = false
        }
    }

    fun reloadPhotos(album: MediaAlbum) {
        isLoading = true
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { reader.loadPhotos(album.bucketId) }
            photos = loaded
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (MediaImagePermissions.hasPermission(context)) {
            reloadAlbums()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = MediaImagePermissions.hasPermission(context)
                hasPermission = granted
                if (granted && albums.isEmpty() && selectedAlbum == null) {
                    reloadAlbums()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedAlbum?.name ?: "选择相册",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        },
        bottomBar = {
            if (selectedAlbum != null && selectedUris.isNotEmpty()) {
                Button(
                    onClick = { onImportSelected(selectedUris.toList()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("确认导入 (${selectedUris.size}/$MAX_SELECTION)")
                }
            }
        }
    ) { padding ->
        when {
            !hasPermission -> {
                PermissionGateContent(
                    modifier = Modifier.padding(padding),
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                    onUseFallbackPicker = onUseFallbackPicker
                )
            }
            isLoading && albums.isEmpty() && selectedAlbum == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            selectedAlbum == null -> {
                AlbumGrid(
                    modifier = Modifier.padding(padding),
                    albums = albums,
                    isLoading = isLoading,
                    onAlbumClick = { album ->
                        selectedAlbum = album
                        selectedUris.clear()
                        reloadPhotos(album)
                    }
                )
            }
            else -> {
                PhotoGrid(
                    modifier = Modifier.padding(padding),
                    photos = photos,
                    selectedUris = selectedUris,
                    isLoading = isLoading,
                    onPhotoToggle = { uri ->
                        if (selectedUris.contains(uri)) {
                            selectedUris.remove(uri)
                        } else if (selectedUris.size < MAX_SELECTION) {
                            selectedUris.add(uri)
                        }
                    },
                    onBackToAlbums = {
                        selectedAlbum = null
                        photos = emptyList()
                        selectedUris.clear()
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionGateContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onUseFallbackPicker: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 8.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "需要照片访问权限",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "授权后可浏览微信、QQ、DCIM 等全部相册。可随时在设置中重新开启。",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
            Text("授予权限")
        }
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text("去设置开启")
        }
        Button(onClick = onUseFallbackPicker, modifier = Modifier.fillMaxWidth()) {
            Text("使用系统选择器（相册较少）")
        }
    }
}

@Composable
private fun AlbumGrid(
    modifier: Modifier = Modifier,
    albums: List<MediaAlbum>,
    isLoading: Boolean,
    onAlbumClick: (MediaAlbum) -> Unit
) {
    if (albums.isEmpty() && !isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("未找到相册，请确认已授予完整照片访问权限", color = Color.Gray)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums, key = { it.bucketId }) { album ->
            AlbumItem(album = album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun AlbumItem(album: MediaAlbum, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        MediaThumbnail(
            uri = album.coverUri,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE0E0E0))
        )
        Text(
            text = album.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "${album.count}",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun PhotoGrid(
    modifier: Modifier = Modifier,
    photos: List<MediaPhoto>,
    selectedUris: List<Uri>,
    isLoading: Boolean,
    onPhotoToggle: (Uri) -> Unit,
    onBackToAlbums: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "← 返回相册列表",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(onClick = onBackToAlbums)
        )
        if (photos.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该相册暂无图片", color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    val selected = selectedUris.contains(photo.uri)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onPhotoToggle(photo.uri) }
                    ) {
                        MediaThumbnail(
                            uri = photo.uri,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (selected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnail(uri: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
        value = uri?.let {
            withContext(Dispatchers.IO) {
                runCatching {
                    loadMediaThumbnail(context.contentResolver, it, targetMaxPx = 320)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(Color(0xFFE0E0E0)))
    }
}

@Composable
fun ImportPermissionBanner(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("开启完整相册访问", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "授权后可浏览全部相册分类（微信、QQ、DCIM 等）",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequestPermission) { Text("授予权限", fontSize = 12.sp) }
                Button(onClick = onOpenSettings) { Text("去设置", fontSize = 12.sp) }
            }
        }
    }
}
