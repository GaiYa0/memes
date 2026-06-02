package com.emoji.overlay.import.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Import source selection screen.
 *
 * Shows available import sources for the user to choose from.
 * Each source triggers a different import flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSourceScreen(
    onAlbumImport: () -> Unit,
    onFolderImport: () -> Unit,
    onZipImport: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入表情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "选择导入来源",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Album import
            ImportSourceCard(
                icon = Icons.Default.PhotoLibrary,
                title = "从相册选择",
                description = "从手机相册中选择表情图片",
                supportedFormats = "PNG, JPG, GIF, WebP",
                onClick = onAlbumImport
            )

            // Folder import
            ImportSourceCard(
                icon = Icons.Default.CreateNewFolder,
                title = "从文件夹导入",
                description = "选择包含表情的文件夹",
                supportedFormats = "支持递归扫描子目录",
                onClick = onFolderImport
            )

            // ZIP import
            ImportSourceCard(
                icon = Icons.Default.FolderZip,
                title = "从ZIP压缩包导入",
                description = "导入ZIP压缩包中的表情",
                supportedFormats = "支持ZIP内嵌目录结构",
                onClick = onZipImport
            )

            Spacer(modifier = Modifier.weight(1f))

            // Info text
            Text(
                text = "提示：导入前会自动检测重复和损坏文件，不会自动导入，需要您确认选择。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ImportSourceCard(
    icon: ImageVector,
    title: String,
    description: String,
    supportedFormats: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = supportedFormats,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
