package com.emoji.overlay.import.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.data.database.EmojiDatabase
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.import.manager.ImportManager
import com.emoji.overlay.import.model.ImportSource
import com.emoji.overlay.import.util.DuplicateDetector
import com.emoji.overlay.import.util.FileValidator
import com.emoji.overlay.import.util.ThumbnailGenerator

/**
 * Activity that handles share intents from other apps.
 *
 * When users share images from QQ, WeChat, browsers, or file managers,
 * this activity receives the shared content and launches the import preview.
 *
 * Supports ACTION_SEND and ACTION_SEND_MULTIPLE with image MIME types.
 */
class ShareImportActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ShareImportActivity"
    }

    private lateinit var importManager: ImportManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies
        val database = EmojiDatabase.getInstance(this)
        val resourceManager = ResourceManager(this)
        val emojiRepository = EmojiRepository(
            database.emojiDao(),
            database.categoryDao(),
            database.tagDao(),
            database.recentHistoryDao(),
            resourceManager
        )
        importManager = ImportManager(
            this,
            emojiRepository,
            resourceManager,
            DuplicateDetector(this, database.emojiDao()),
            FileValidator()
        )

        // Process share intent
        val uris = extractSharedUris()
        val sourceApp = intent.`package` ?: "分享导入"

        if (uris.isEmpty()) {
            Log.w(TAG, "No shared content found")
            finish()
            return
        }

        Log.d(TAG, "Received ${uris.size} shared items from $sourceApp")

        // Start import session
        importManager.startShareImport(uris, sourceApp)

        setContent {
            MaterialTheme {
                ShareImportScreen(
                    importManager = importManager,
                    onDismiss = { finish() }
                )
            }
        }
    }

    /**
     * Extract URIs from the share intent.
     */
    private fun extractSharedUris(): List<Uri> {
        val uris = mutableListOf<Uri>()

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }
        }

        return uris.filter { uri ->
            val mimeType = contentResolver.getType(uri)
            mimeType?.startsWith("image/") == true
        }
    }

    override fun onDestroy() {
        importManager.destroy()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareImportScreen(
    importManager: ImportManager,
    onDismiss: () -> Unit
) {
    val session by importManager.currentSession.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分享导入") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (session == null) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在处理分享内容...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "发现 ${session?.totalCount ?: 0} 个文件",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}
