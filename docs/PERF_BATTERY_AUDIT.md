# Emoji Overlay 后台/电量优化审计报告

## 成本点清单（优化前）

| 模块 | 位置 | 问题类型 | 触发范围 | 频率 |
|------|------|----------|----------|------|
| 前台服务 | `EmojiOverlayService.kt:274` | overlay 创建后 Lifecycle 恒为 RESUMED | AlwaysOn | 持续 |
| 小菜单 Flow | `EmojiPanel.kt:101-110` | 同时 collect 5 路 Room Flow | WhenPanelVisible | 高 |
| 小菜单解码 | `EmojiPanel.kt:324-338` | 无 LRU，重复解码 | WhenPanelVisible | 滚动时高 |
| 主界面解码 | `EmojiSharedComponents.kt:106-114` | 全尺寸 decodeFile | WhenAppForeground | 高 |
| 通知通道 | `EmojiOverlayService.kt:189` | IMPORTANCE_DEFAULT | AlwaysOn FGS | 持续 |
| 通知刷新 | `EmojiOverlayService.kt:252-260` | 仅 show/hide（已合规） | OnDemand | 低 |

## 已实施优化

| 优化项 | 文件 | 原理 |
|--------|------|------|
| 面板隐藏 Lifecycle STARTED | `EmojiOverlayService.kt` | 减少 Compose 活跃窗口 |
| 单路 emoji Flow | `EmojiPanel.kt` | 按 Tab/分类只订阅 1 路 |
| emojiCount 按需 | `EmojiPanel.kt` | 分类筛选时不订阅 count |
| 通知 LOW + 无振动/声音 | `EmojiOverlayService.kt` | 降低唤醒 |
| 缩略图 LRU + 采样 | `ThumbnailBitmapCache.kt`, `BitmapSampling.kt` | 减 IO/RAM |
| 主界面卡片采样 | `EmojiSharedComponents.kt` | 320px 采样 + 缓存 |

## 验证

`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`

## 未做项

- 关闭前台服务、轮询替代 Flow、降低 GIF 质量、Room schema 变更
