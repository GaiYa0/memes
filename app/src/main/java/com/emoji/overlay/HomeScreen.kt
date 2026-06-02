package com.emoji.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    hasPermission: Boolean,
    isServiceRunning: Boolean,
    onRequestPermission: () -> Unit,
    onToggleService: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val emojiCount by viewModel.getEmojiCount().collectAsState(initial = 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Emoji Overlay", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("表情包管理 · 通知栏点击打开小菜单", fontSize = 14.sp, color = Color(0xFF888888))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Overlay 状态", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                text = if (isServiceRunning) "运行中" else "未启动",
                                fontSize = 13.sp,
                                color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFF999999)
                            )
                        }
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { onToggleService() }
                        )
                    }

                    if (!hasPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("需要悬浮窗权限")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "表情总数: $emojiCount",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            HomeActionRow(
                left = HomeAction("导入表情", "从相册/文件夹添加", Icons.Default.FileUpload, Color(0xFF4CAF50)) {
                    onNavigate(Routes.IMPORT)
                },
                right = HomeAction("浏览表情", "查看全部表情", Icons.Default.GridView, Color(0xFF2196F3)) {
                    onNavigate(Routes.BROWSE)
                }
            )
        }
        item {
            HomeActionRow(
                left = HomeAction("收藏", "收藏的表情", Icons.Default.Favorite, Color(0xFFE91E63)) {
                    onNavigate(Routes.FAVORITES)
                },
                right = HomeAction("最近使用", "使用记录", Icons.Default.History, Color(0xFFFF9800)) {
                    onNavigate(Routes.RECENT)
                }
            )
        }
        item {
            HomeActionRow(
                left = HomeAction("分类管理", "按类别浏览", Icons.Default.Category, Color(0xFF9C27B0)) {
                    onNavigate(Routes.CATEGORIES)
                },
                right = HomeAction("搜索", "搜索表情", Icons.Default.Search, Color(0xFF00BCD4)) {
                    onNavigate(Routes.SEARCH)
                }
            )
        }
    }
}

private data class HomeAction(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun HomeActionRow(left: HomeAction, right: HomeAction) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = left.icon,
            label = left.label,
            description = left.description,
            color = left.color,
            onClick = left.onClick
        )
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = right.icon,
            label = right.label,
            description = right.description,
            color = right.color,
            onClick = right.onClick
        )
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = description, fontSize = 12.sp, color = Color(0xFF888888))
        }
    }
}
