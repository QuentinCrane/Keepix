<p align="center">
  <img src="./app/src/main/res/drawable/ic_launcher_kanlemo.png" width="104" alt="看了么 App 图标">
</p>

<h1 align="center">看了么-Keepix</h1>

<p align="center">
  本地优先的 Android 照片 / 视频整理 App
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4">
  <img alt="Local First" src="https://img.shields.io/badge/privacy-local--first-111827">
  <img alt="License" src="https://img.shields.io/badge/license-Apache--2.0-blue">
</p>

看了么通过 Android MediaStore 读取本机媒体库，帮助用户快速筛选照片和视频、管理收藏、放入 App 内部回收站，并在二次确认后删除媒体内容。v2.2.9 增强了 GIF 动图、实况 / 动态照片预览、相似图片检测、成就陈列柜与发布体积优化。它不是云相册，也不是社交相册，而是一个专注于本地媒体整理的工具。

> 核心原则：不强制登录、不上传照片或视频、不做云同步、不包含会员、支付、广告、订阅或远程授权校验。

## 快速入口

| 目标         | 链接                                |
| ------------ | ----------------------------------- |
| 了解技术设计 | [技术说明](docs/technical_overview.md) |
| 查看隐私边界 | [隐私说明](docs/privacy.md)            |
| 本地构建     | [构建说明](docs/build.md)              |
| AI 接手      | [交接文档](docs/codex_next_prompt.md)  |


## 功能概览

| 模块         | 功能                                                       |
| ------------ | ---------------------------------------------------------- |
| 图片整理     | 滑动筛选、收藏、加入回收站、重新随机、排除文件夹、点击预览、GIF 播放、实况 / 动态照片预览 |
| 视频整理     | 视频预览、上下切换、进度控制、收藏、加入回收站、排除文件夹 |
| 相似图片     | 后台检测、进度显示、继续检测、指纹缓存复用、多特征候选匹配 |
| 收藏与回收站 | 集中管理已收藏和待删除媒体，支持预览、恢复、二次确认删除、可释放空间估算 |
| 当年今日     | 按日期重新发现历史照片和视频，使用更适合照片浏览的长方形照片墙 |
| 成就系统     | 本地成就陈列柜、完成率、XP、稀有度、分类筛选与解锁状态 |
| 设置中心     | 整理规则、外观、交互、触觉反馈、数据管理和关于页面         |
| 适配体验     | 深色模式、OLED 友好显示、手机和平板布局、预测式返回        |

## 隐私与删除策略

看了么围绕 Android 本机媒体库工作，核心流程不依赖服务器。

- 读取用户授权的照片、视频与 MediaStore 元数据。
- 整理状态、回收站、收藏、统计和设置保存在本机 Room / DataStore。
- 不上传用户照片或视频。
- 不做广告追踪、账号体系、云同步、会员支付或远程授权校验。
- 永久删除公共媒体时使用 Android 系统授权流程，不绕过系统确认。

## 技术栈

| 类型 | 技术                                            |
| ---- | ----------------------------------------------- |
| 语言 | Kotlin `2.0.21`                               |
| 构建 | Android Gradle Plugin `8.8.2`、Gradle Wrapper |
| UI   | Jetpack Compose `1.8.2`、Material 3 `1.3.2` |
| 架构 | MVVM、Hilt                                      |
| 数据 | Room `2.7.1`、DataStore `1.1.7`             |
| 媒体 | MediaStore、Media3 `1.6.1`、Coil `2.7.0`    |
| 后台 | WorkManager `2.10.1`                          |

版本要求：

- `minSdk = 30`，Android 11。
- `targetSdk = 35`。
- `compileSdk = 35`。
- JDK 17。

## 快速开始

克隆仓库：

```bash
git clone <your-repo-url>
cd <repo-name>
```

复制本机配置模板：

```powershell
Copy-Item local.properties.example local.properties
```

编辑 `local.properties`，把 `sdk.dir` 改成本机 Android SDK 路径。例如：

```properties
sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

然后用 Android Studio 打开项目根目录，等待 Gradle Sync 完成后即可运行。

## 构建

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux：

```bash
./gradlew :app:assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

常用命令：

| 命令                                   | 用途               |
| -------------------------------------- | ------------------ |
| `.\gradlew.bat :app:assembleDebug`   | 构建 Debug APK     |
| `.\gradlew.bat :app:assembleRelease` | 构建 Release APK   |
| `.\gradlew.bat :app:kapt`            | 单独运行注解处理器 |

如果仓库保留了辅助脚本，Windows 用户也可以使用 `scripts/windows/` 下的脚本；正式发布仍建议以 Gradle Wrapper 命令为准。

## Release APK

源码仓库不提交 APK、AAB、签名密钥或本地签名配置。发布安装包时建议：

- 在本机或 CI 中使用自己的 release keystore 签名。
- 不把 keystore、密码、`local.properties` 或本地构建产物提交进仓库。
- 将最终 APK 上传到 GitHub Releases 附件。
- Release 说明中附上版本号、核心变化、权限说明、隐私边界和已知问题。

v2.2.9 Release 说明建议重点标注：GIF 与动态照片支持、轻量媒体扫描恢复、整理收益提示、相似图片后台检测、成就陈列柜、回收站 / 当年今日照片墙优化，以及 R8 压缩后约 6.1 MB 的 APK 体积。

## 项目结构

```text
.
├── .github/                 GitHub Actions、Issue 模板和 PR 模板
├── app/                     Android App 模块
│   ├── src/main/            Manifest、Kotlin 源码与资源
│   └── schemas/             Room schema 导出文件
├── docs/                    公开技术说明、隐私说明和发布说明
├── gradle/wrapper/          Gradle Wrapper
├── scripts/windows/         Windows 辅助构建脚本
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties.example
```

## 贡献

- 保持本地优先，不引入账号、会员、支付、订阅、广告或远程授权校验。
- 修改媒体删除、移动、恢复等高风险逻辑时，优先保证用户可确认、可撤销、可恢复。
- 修改数据库结构时同步更新 `app/schemas/` 下的 Room schema。
- 提交前建议至少执行一次 Debug 构建。

详细流程见 [贡献指南](CONTRIBUTING.md)。

## 许可证

本项目使用 [Apache License 2.0](LICENSE)。

项目同时包含 [NOTICE](NOTICE)，用于声明原始项目来源：`https://github.com/QuentinCrane/Keepix`。分发本项目或衍生作品时，应按 Apache-2.0 要求保留许可证和 NOTICE 归属声明。
