# Release APK、R8 与依赖审计

## 审计范围

本记录对应 D11，目标是只移除已确认没有消费者的代码或规则，不为了追求一个百分比而猜测删除依赖。审计日期：2026-08-13。

本机没有 `apkanalyzer` 可执行文件，因此使用 SDK 36.0.0 的 `aapt2 dump badging` 和 ZIP entry 枚举完成同一份 APK 的结构化盘点。R8 仍由 Release 构建执行，并保留本机 `mapping.txt`、`usage.txt`、`seeds.txt` 和 `configuration.txt` 供发布审计；这些文件不提交到仓库。

## D11 基线 Release 产物

以下数值是 D11 在 2026-08-13 的历史基线，不是依赖升级后的当前发布候选。当前候选的时间戳、哈希和签名状态以 [G02 发布候选验证记录](发布候选验证记录_2026-08-14.md) 为准。

| 项目 | 结果 |
| --- | --- |
| 文件 | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| 版本 | `2.3.4` / versionCode `60` |
| min / target SDK | 30 / 36 |
| 字节数 | 6,875,830 |
| 构建时间 | 2026-08-13 12:45:42 |
| SHA-256 | `326F796CFF6AA7C5AC43978B78D66CEA461071B5A8F60886482672B5FA338038` |
| 签名 | 未签名；本报告不把它当作可发布安装包 |

D10 结束时 APK 字节数也是 6,875,830，因此 D11 的安全清理没有带来可观测的 APK 体积下降，不能宣称包体优化百分比。R8 可能已经删除了这两处死代码对应的实现；本次价值主要是收敛源码和规则审计面。后续 F01–F05 依赖升级改变了当前候选体积，不能与这份历史基线直接比较而得出优化结论。

## ZIP entry 压缩体积

以下是 APK entry 的 `CompressedLength`，不是文件系统展开后的大小；ZIP 中央目录等封装开销未分摊到下表。

| 部分 | 压缩字节数 |
| --- | ---: |
| `classes.dex` | 5,089,916 |
| `res/` | 1,050,345 |
| `resources.arsc` | 491,532 |
| `lib/`（4 ABI） | 60,292 |
| `assets/dexopt/baseline.prof` + `.profm` | 6,786 |
| 其他 entry（Manifest、META-INF、Kotlin、okhttp3） | 76,850 |
| entry 合计 | 6,775,721 |
| APK 文件总计 | 6,875,830 |

APK 内只有一个 `classes.dex`。`lib/` 中存在 `androidx.graphics.path` 和 DataStore shared-counter 的四 ABI 产物；当前没有证据表明可以在不改变 ABI 覆盖的情况下删除它们。

## 依赖消费者核对

| 依赖或规则 | 真实消费者 | D11 决策 |
| --- | --- | --- |
| WorkManager / Hilt Work | `SimilarAnalysisWork.kt` 的 `@HiltWorker`、`WorkManager` 和唯一后台任务调度 | 保留 |
| DocumentFile | `NativeFolderPicker.kt` 的文件夹授权与遍历 | 保留 |
| Media3 ExoPlayer / UI | `MediaCards.kt`、`PhotoViewerScreen.kt`、`VideoCleanScreen.kt` | 保留 |
| Coil Compose / GIF | 多个照片、视频、网格和沉浸式页面及共享 `ImageLoader` | 保留 |
| DataStore Preferences | `AppSettingsRepository.kt` 的设置持久化 | 保留 |
| Room / Hilt / Navigation / Lifecycle | 数据库、注入、导航和生命周期收集链路 | 保留 |
| Profile Installer | Baseline Profile 运行时与 APK 中的 profile 安装入口 | 保留 |
| `androidx.exifinterface` | 没有依赖声明、导入或运行时调用；只有一条旧 `dontwarn` | 删除旧规则 |

同时删除了 `SimilarDao.allFingerprints()`：D08 已改用 `fingerprintedIds()` 和按 ID 分块的 `fingerprintsByPhotoIds()`，仓库内没有其他调用点。

## R8 规则审阅

Release 继续使用 `minifyEnabled = true` 和 `shrinkResources = true`。应用自有规则只保留 `KanlemeApplication` 与 `MainActivity` 的入口保留规则；Hilt、Room、WorkManager、Profile Installer 和 Media3 的消费者规则由对应插件或库提供。没有新增宽泛包级 `-keep`、签名信息或本机路径。

## 验证与未验证项

已执行并通过：

```text
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease --offline --console=plain --no-daemon
```

`aapt2 dump badging` 已核对包名 `com.futureape.kanleme`、版本、min/target SDK 和应用标签。当前 `adb devices` 没有连接设备，因此本块没有宣称 AndroidTest 真机执行、安装启动、Hilt/Room/Media3 实机路径或 ABI 安装验证。签名配置仍需在安全的本机/CI 环境恢复后，另行生成签名 Release 产物。
