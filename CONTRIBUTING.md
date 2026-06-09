# 贡献指南

感谢你愿意改进“看了么”。这个项目的核心方向是：本地优先、安全防误删、尊重用户媒体隐私。

## 本地开发

1. 安装 Android Studio、JDK 17 和 Android SDK Platform 35。
2. 复制 `local.properties.example` 为 `local.properties`，配置本机 `sdk.dir`。
3. 执行 Debug 构建：

```bash
./gradlew :app:assembleDebug
```

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 提交前检查

- 不提交 `local.properties`、`local-private/`、签名密钥、APK/AAB、Gradle 构建产物或真实媒体文件。
- 修改数据库 Entity、Dao 或迁移逻辑时，同步更新 `app/schemas/`。
- 修改权限、删除、移动、恢复等逻辑时，优先保证用户可确认、可撤销、可恢复。
- 修改 UI 时保持 Material 3 + 液态玻璃风格，不牺牲照片和视频可读性。
- 修改用户可见功能时，同步 `README.md`、`docs/technical_overview.md`、`docs/release_notes.md` 和相关发布文档。
- 提交前至少运行一次 `:app:assembleDebug`；准备 Release 前再运行 `:app:assembleRelease`。

## 产品边界

请不要引入：

- 账号、会员、VIP、支付、订阅或广告。
- 强制登录、云同步、远程授权校验。
- 上传用户照片或视频的默认流程。
- 绕过 Android 系统确认的公共媒体永久删除逻辑。

## 代码风格

- Kotlin 命名遵循 Android 常规约定：类名 PascalCase，函数和变量 camelCase。
- Compose 页面保持状态驱动，避免在 Composable 中直接创建长期对象。
- Repository 和扫描逻辑应避免阻塞主线程。
- 中文错误提示尽量清晰，媒体权限提示应说明“本机处理、不上传媒体”。
- 发布文档、PR 模板和 Release 草稿应跟实际功能保持一致，避免留下指向本地或被忽略文件的链接。
