<p align="center">
  <img src="./app/src/main/res/drawable/ic_launcher_kanlemo.png" width="104" alt="Keepix 应用图标">
</p>

<h1 align="center">看了么</h1>

<p align="center">Keepix · 本地优先的 Android 照片 / 视频整理</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4">
  <img alt="Local First" src="https://img.shields.io/badge/privacy-local--first-111827">
  <img alt="License" src="https://img.shields.io/badge/license-Apache--2.0-blue">
</p>

<p align="center">
  <a href="README.md">English</a> · <a href="README.zh-CN.md">中文</a>
</p>

## 概览

看了么会把杂乱的相册变成可以连续刷选的整理工作流。它通过 Android `MediaStore` 读取本机媒体，所有整理状态都留在设备上，不依赖云端，也不需要账号。

### 先从这里开始

1. 打开 App 并授权照片或视频访问。
2. 选择要整理的照片或视频入口。
3. 左右滑动完成保留、收藏或删除。
4. 之后可以回到回收站、收藏、相似照片和历史视图继续查看。

### 为什么更顺手

- 照片和视频都采用持续刷选的整理流，而不是密集列表。
- 收藏、回收站和相似照片彼此独立，不会互相打扰。
- 删除会先进入 App 回收站，真正永久删除时再走 Android 系统确认流程。
- “当年今日”、成就和轻量回看入口，让 App 不只是一次性清理工具。
- 手机和平板布局都把主要操作放在更容易触达的位置。

### 功能范围

- 照片整理，支持保留、收藏、删除、撤回和按时间筛选。
- 视频整理，支持分阶段复查、暂存保留和退出时统一提交。
- 相似照片检测，带有指纹缓存和后台进度。
- 照片与视频分别管理的收藏和回收站。
- 本机保存的设置中心，用于布局、行为、反馈和维护。

### 隐私边界

- 不做账号、会员、广告、订阅或云同步。
- 不上传用户照片或视频。
- 设置、统计、整理状态和收藏都保存在本机。
- 永久删除遵循 Android 授权流程。

### 截图

这里先预留截图位。你后面补充真实截图时，保持文件名稳定就行，这样 README 可以跟着一起升级。

<!-- 建议放在 docs/screenshots/ 下
home.png
photo-clean.png
video-clean.png
favorites-trash.png
settings.png
tablet.png
dark-mode.png
-->

### 更多说明

- [技术说明](docs/technical_overview.md) - 产品范围、架构要点和行为边界
- [隐私说明](docs/privacy.md) - App 会读取、保存什么，以及不会发送什么
- [构建说明](docs/build.md) - Debug / Release 构建命令、签名流程和产物位置
- [发布说明](docs/release_notes.md) - 当前发布文案和发布检查清单
- [交接文档](docs/AI_HANDOFF.md) - 给下一位维护者或 AI 的公开上下文

<details>
<summary>开发者说明</summary>

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux：

```bash
./gradlew :app:assembleDebug
```

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

版本要求：

- `minSdk = 30`
- `targetSdk = 35`
- `compileSdk = 35`
- JDK 17

技术栈：

- Kotlin `2.0.21`
- Jetpack Compose `1.8.2`
- Material 3 `1.3.2`
- Room `2.7.1`
- DataStore `1.1.7`
- Media3 `1.6.1`
- Coil `2.7.0`

</details>

## 许可证

本项目使用 [Apache License 2.0](LICENSE)。

项目同时包含 [NOTICE](NOTICE)，用于保留原始项目来源：`https://github.com/QuentinCrane/Keepix`
