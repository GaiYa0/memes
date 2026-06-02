package com.emoji.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emoji.overlay.service.EmojiOverlayService

/**
 * Entry point activity.
 *
 * Responsibilities:
 * 1. Request SYSTEM_ALERT_WINDOW permission
 * 2. Start/stop the overlay service
 * 3. Show status information
 */
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Permission result handled in compose via re-check
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmojiOverlayApp(
                onRequestPermission = { requestOverlayPermission() },
                onStartService = { startOverlayService() },
                onStopService = { stopOverlayService() }
            )
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return
        }
        val intent = Intent(this, EmojiOverlayService::class.java).apply {
            action = EmojiOverlayService.ACTION_SHOW
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Emoji overlay 已启动\n双指双击屏幕打开面板", Toast.LENGTH_LONG).show()
    }

    private fun stopOverlayService() {
        EmojiOverlayService.stop(this)
        Toast.makeText(this, "Emoji overlay 已停止", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun EmojiOverlayApp(
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isServiceRunning by remember { mutableStateOf(EmojiOverlayService.isActive()) }

    // Refresh permission state when composable recomposes
    LaunchedEffect(Unit) {
        hasPermission = Settings.canDrawOverlays(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFFAFAFA)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "😊",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Emoji Overlay",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "双指双击屏幕打开表情面板",
                fontSize = 16.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Permission status
            StatusChip(
                label = if (hasPermission) "✓ 悬浮窗权限已授予" else "✗ 需要悬浮窗权限",
                isPositive = hasPermission
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatusChip(
                label = if (isServiceRunning) "● 服务运行中" else "○ 服务未启动",
                isPositive = isServiceRunning
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action buttons
            if (!hasPermission) {
                Button(
                    onClick = {
                        onRequestPermission()
                        // Re-check after returning from settings
                        hasPermission = Settings.canDrawOverlays(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4)
                    )
                ) {
                    Text("授予悬浮窗权限", fontSize = 18.sp)
                }
            } else if (!isServiceRunning) {
                Button(
                    onClick = {
                        onStartService()
                        isServiceRunning = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("启动 Emoji Overlay", fontSize = 18.sp)
                }
            } else {
                Button(
                    onClick = {
                        onStopService()
                        isServiceRunning = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("停止 Emoji Overlay", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0EDFF)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "使用说明",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF4A4458)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InstructionItem("1. 授予悬浮窗权限")
                    InstructionItem("2. 点击「启动」按钮")
                    InstructionItem("3. 切换到任意聊天应用")
                    InstructionItem("4. 双指双击屏幕 → 打开表情面板")
                    InstructionItem("5. 点击空白区域 → 关闭面板")
                    InstructionItem("6. 点击表情 → 复制到剪贴板")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, isPositive: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isPositive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}

@Composable
private fun InstructionItem(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF5E5873),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
