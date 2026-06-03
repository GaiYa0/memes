package com.emoji.overlay

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.emoji.overlay.import.media.MediaImagePermissions
import com.emoji.overlay.import.ui.ImportPermissionBanner

@Composable
fun ImportScreen(
    viewModel: MainViewModel,
    onNavigateToAlbumImport: () -> Unit
) {
    val context = LocalContext.current
    var importResult by remember { mutableStateOf("") }
    var hasMediaPermission by remember {
        mutableStateOf(MediaImagePermissions.hasPermission(context))
    }
    var pendingAlbumNavigation by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromUris(context, uris)
            importResult = "正在导入 ${uris.size} 个文件..."
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromUris(context, uris)
            importResult = "正在导入 ${uris.size} 个文件..."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaPermission = granted
        if (granted) {
            onNavigateToAlbumImport()
        }
    }

    fun requestMediaPermission() {
        permissionLauncher.launch(MediaImagePermissions.requiredPermission())
    }

    fun openAlbumImportFlow() {
        if (MediaImagePermissions.hasPermission(context)) {
            onNavigateToAlbumImport()
        } else {
            pendingAlbumNavigation = true
            requestMediaPermission()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = MediaImagePermissions.hasPermission(context)
                hasMediaPermission = granted
                if (granted && pendingAlbumNavigation) {
                    pendingAlbumNavigation = false
                    onNavigateToAlbumImport()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("导入表情包", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("支持 PNG、JPG、WebP、GIF 格式", fontSize = 13.sp, color = Color(0xFF666666))
            }
        }

        if (!hasMediaPermission) {
            ImportPermissionBanner(
                onRequestPermission = { requestMediaPermission() },
                onOpenSettings = {
                    pendingAlbumNavigation = true
                    MediaImagePermissions.openAppSettings(context)
                }
            )
        }

        ImportOptionCard(
            icon = Icons.Default.PhotoLibrary,
            title = "从相册选择",
            description = "浏览全部相册分类（微信、QQ、DCIM 等）",
            color = Color(0xFF4CAF50),
            onClick = {
                try {
                    openAlbumImportFlow()
                } catch (e: Exception) {
                    Log.e(TAG, "Album import launch failed", e)
                }
            }
        )

        ImportOptionCard(
            icon = Icons.Default.Folder,
            title = "从文件夹选择",
            description = "浏览文件系统选择图片",
            color = Color(0xFF2196F3),
            onClick = {
                try {
                    filePickerLauncher.launch(arrayOf("image/*"))
                } catch (e: Exception) {
                    Log.e(TAG, "File picker launch failed", e)
                }
            }
        )

        if (!hasMediaPermission) {
            ImportOptionCard(
                icon = Icons.Default.PhotoLibrary,
                title = "系统选择器（相册较少）",
                description = "无需权限，但只能看到少量相册",
                color = Color(0xFF9E9E9E),
                onClick = {
                    try {
                        imagePickerLauncher.launch("image/*")
                    } catch (e: Exception) {
                        Log.e(TAG, "Fallback gallery launch failed", e)
                    }
                }
            )
        }

        if (importResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = importResult,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(description, fontSize = 13.sp, color = Color(0xFF888888))
            }
        }
    }
}
