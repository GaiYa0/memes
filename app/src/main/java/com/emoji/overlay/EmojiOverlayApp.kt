package com.emoji.overlay

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepositoryHolder
import com.emoji.overlay.data.util.ResourceManager
import com.emoji.overlay.send.manager.EmojiSendManager
import com.emoji.overlay.service.EmojiOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiOverlayApp(
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val shareScope = rememberCoroutineScope()
    val sendManagerState = remember { mutableStateOf<EmojiSendManager?>(null) }

    LaunchedEffect(Unit) {
        viewModel.init(context)
        sendManagerState.value = withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            EmojiSendManager(
                context = appContext,
                repository = EmojiRepositoryHolder.getRepository(appContext),
                resourceManager = ResourceManager.getInstance(appContext)
            )
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hasPermission = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val isServiceRunning = remember { mutableStateOf(EmojiOverlayService.isActive()) }

    LaunchedEffect(currentRoute) {
        try {
            hasPermission.value = Settings.canDrawOverlays(context)
            isServiceRunning.value = EmojiOverlayService.isActive()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing state", e)
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFF625B71),
            surface = Color(0xFFFFFBFE),
            surfaceVariant = Color(0xFFE7E0EC),
            background = Color(0xFFFFFBFE)
        )
    ) {
        Scaffold(
            topBar = {
                if (currentRoute != Routes.HOME) {
                    TopAppBar(
                        title = {
                            Text(text = topBarTitle(currentRoute))
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                try {
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Back failed", e)
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF6750A4),
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(paddingValues)
            ) {
                val onEmojiClick: (EmojiEntity) -> Unit = { emoji ->
                    val manager = sendManagerState.value
                    if (manager == null) {
                        Toast.makeText(context, "发送组件初始化中，请稍后再试", Toast.LENGTH_SHORT).show()
                    } else {
                        shareScope.launch {
                            val success = manager.sendViaShare(emoji)
                            if (!success) {
                                Toast.makeText(context, "分享失败，请确认已安装 QQ", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        hasPermission = hasPermission.value,
                        isServiceRunning = isServiceRunning.value,
                        onRequestPermission = onRequestPermission,
                        onToggleService = {
                            try {
                                if (isServiceRunning.value) {
                                    EmojiOverlayService.stop(context)
                                    isServiceRunning.value = false
                                } else {
                                    if (!Settings.canDrawOverlays(context)) {
                                        onRequestPermission()
                                        return@HomeScreen
                                    }
                                    val intent = Intent(context, EmojiOverlayService::class.java).apply {
                                        action = EmojiOverlayService.ACTION_SHOW
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                    isServiceRunning.value = true
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Toggle service failed", e)
                                Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigate = { route ->
                            try {
                                if (!isValidTopLevelRoute(route)) {
                                    Log.e(TAG, "Blocked invalid route: $route")
                                    Toast.makeText(context, "页面暂不可用", Toast.LENGTH_SHORT).show()
                                    return@HomeScreen
                                }
                                navController.navigate(route, navOptions {
                                    launchSingleTop = true
                                })
                            } catch (e: Exception) {
                                Log.e(TAG, "Navigation to $route failed", e)
                                Toast.makeText(context, "页面跳转失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                composable(Routes.IMPORT) {
                    ImportScreen(viewModel = viewModel)
                }

                composable(Routes.BROWSE) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onEmojiClick = onEmojiClick
                    )
                }

                composable(Routes.FAVORITES) {
                    FavoritesScreen(
                        viewModel = viewModel,
                        onEmojiClick = onEmojiClick
                    )
                }

                composable(Routes.RECENT) {
                    RecentScreen(
                        viewModel = viewModel,
                        onEmojiClick = onEmojiClick
                    )
                }

                composable(Routes.CATEGORIES) {
                    CategoriesScreen(
                        viewModel = viewModel,
                        onCategoryClick = { category ->
                            try {
                                navController.navigate(Routes.categoryDetail(category.id))
                            } catch (e: Exception) {
                                Log.e(TAG, "Category navigation failed", e)
                                Toast.makeText(context, "页面跳转失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                composable(
                    Routes.CATEGORY_DETAIL,
                    arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
                    CategoryDetailScreen(
                        viewModel = viewModel,
                        categoryId = categoryId,
                        onEmojiClick = onEmojiClick
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        viewModel = viewModel,
                        onEmojiClick = onEmojiClick
                    )
                }
            }
        }
    }
}

private fun topBarTitle(currentRoute: String?): String {
    return when (currentRoute) {
        Routes.IMPORT -> "导入表情"
        Routes.BROWSE -> "浏览表情"
        Routes.FAVORITES -> "收藏"
        Routes.RECENT -> "最近使用"
        Routes.CATEGORIES -> "分类管理"
        Routes.SEARCH -> "搜索"
        else -> if (currentRoute?.startsWith("category/") == true) "分类详情" else ""
    }
}
