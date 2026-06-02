# Emoji Overlay

Android 表情包悬浮窗应用，支持导入、浏览、搜索与快捷发送。

## 功能

- 表情包导入与管理（图片 / GIF）
- 分类、收藏、最近使用
- 悬浮窗快捷发送
- Room 本地存储

## 构建

```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
```

Release APK 输出路径：`app/build/outputs/apk/release/app-release.apk`

> Release 构建使用 debug 签名，可直接 sideload 安装。上架 Play Store 前需替换为正式签名密钥。

## CI

推送至 `main` / `master` 分支时，GitHub Actions 会自动运行单元测试并构建 Release APK，产物可在 Actions 的 Artifacts 中下载。

## 技术栈

- Kotlin + Jetpack Compose
- Room + WorkManager
- minSdk 25 / targetSdk 34
