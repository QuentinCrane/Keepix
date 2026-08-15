# 相似照片检测任务约定

## C08a：持久化状态

相似照片检测的任务状态保存在 Room 的 `similar_analysis_tasks` 单例行中，任务不会把生命周期绑定到某个页面或 ViewModel。该行记录：

- `runToken`：本次任务实例的唯一标识，防止旧 Worker 覆盖新任务
- `mediaVersion`：当前可分析照片输入的 SHA-256 版本
- `accessKey`：系统媒体权限快照
- `algorithmVersion`：指纹/聚类算法版本
- `requestedLimit`、`cursor`、`totalPhotos`：请求范围和可恢复分页位置
- `status`、`stage`、`cancelRequested`：排队、运行、暂停、取消、完成和失败语义
- `lastResultCount`、`lastError`：最后一次结果与可诊断失败信息

状态值：

```text
idle -> queued -> running -> completed
                   ├──────> paused -> queued
                   ├──────> cancel_requested -> cancelled
                   └──────> failed -> queued
```

## 游标和一致性

候选照片按 `date_taken DESC, id DESC` 分页，每批 24 张。只有一批指纹成功写入后才推进 `cursor`；进程在批次中途死亡时最多重复当前批次，不会跳过照片。聚类结果只在完整输入通过一致性检查后替换旧分组，因此取消或失败不会删除仍有效的旧分组。

## 失效边界

任务输入由可分析照片的 ID、MediaStore ID、稳定 key、URI、文件大小、拍摄时间、修改时间和尺寸组成。以下任一项变化会让任务从新 `runToken` 重新准备，并清空旧指纹缓存：

- MediaStore 可见照片新增、删除或属性变化
- Android 媒体权限快照变化，尤其是部分媒体重新选择
- 指纹/聚类算法版本变化
- 用户主动重新检测

关闭“相似照片检测”只取消未来任务，不清空已经生成的分组或指纹。后台 Worker 使用唯一工作名 `kanleme-similar-photo-analysis`：自动任务采用 `KEEP`、非低电量/非低存储/设备空闲约束；手动开始或继续采用 `REPLACE`、非低电量/非低存储约束。两种模式共享同一个工作名，不会并发运行；失败使用 30 秒起步的指数退避，最多重试 4 次，进程被系统停止时由 WorkManager 保留并恢复任务。

## 当前验证

- JVM：活动状态集合、分页常量和算法版本合同
- Room AndroidTest 源码覆盖：任务 token、权限/媒体版本、游标和取消标记可写入并恢复；当前门禁只完成 AndroidTest Kotlin 编译，真实设备执行仍待连接设备
- JVM：自动/手动约束、唯一工作名、KEEP/REPLACE 策略和退避合同
- AndroidTest：实际 `WorkInfo` 断言 AUTO 携带设备空闲、电量和存储约束，MANUAL 不携带设备空闲但保留电量和存储约束
- 构建：Debug/KAPT/Room schema 已通过；WorkManager 的强杀、重启、低电量和真实权限对话框仍需设备验收
- 验证边界：单个 instrumentation 测试不能在 `am force-stop` 目标包后继续执行断言，因为强停会同时终止当前目标测试进程；进程重建必须由外部两阶段 runner 完成（阶段一入队并退出，外部强停/重启，阶段二重新安装或启动测试并读取唯一工作状态）。在该 runner 接入前，不把现有入队/取消测试当作进程重建证据。

### 两阶段 AVD 验证入口

`SimilarAnalysisSchedulerTest` 支持显式 runner 参数 `kanleme.processRecoveryPhase`，默认不改变普通测试行为。使用已安装的 Debug 与 AndroidTest APK 时，可执行：

```powershell
adb shell am instrument -w `
  -e class com.futureape.kanleme.data.worker.SimilarAnalysisSchedulerTest `
  -e kanleme.processRecoveryPhase enqueue `
  com.futureape.kanleme.test/com.futureape.kanleme.HiltTestRunner
adb shell am force-stop com.futureape.kanleme
adb shell am instrument -w `
  -e class com.futureape.kanleme.data.worker.SimilarAnalysisSchedulerTest `
  -e kanleme.processRecoveryPhase verify `
  com.futureape.kanleme.test/com.futureape.kanleme.HiltTestRunner
```

2026-08-15 在 `KeepixApi30` 上两阶段均返回 `OK (1 test)`；阶段二在目标包被强停后重新读取唯一任务并成功清理。该证据覆盖 WorkManager 数据持久化与应用进程停止/重新初始化，不覆盖低电量、设备空闲约束或 OEM 后台策略。
