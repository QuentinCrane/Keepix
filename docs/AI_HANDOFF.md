# AI 接手说明

这份文档用于下一位维护者或 AI 助手快速接手当前公开仓库。它只记录可以随仓库公开的信息，不包含签名密钥、密码、本地路径日志或真实媒体。

## 当前版本

- App 名称：看了么 / Keepix
- 包名：`com.futureape.kanleme`
- 当前版本：`2.3.5`
- `versionCode`：`61`
- 最低系统：Android 11，`minSdk = 30`
- 目标系统：`targetSdk = 36`

## 当前功能状态

- 首页已经收敛为现代白色 / 浅蓝主视觉，照片整理与视频整理可横向切换，并保留最近新增照片 / 视频入口。
- 首页只保留有信息或操作意义的组件，不使用纯装饰背景几何块来制造“热闹感”。
- 照片整理支持边缘动作反馈、短进度胶囊、全屏年份 / 月份筛选、指定归档、左下角数量 / 撤回按钮和点击大图预览。
- 视频整理采用会话暂存保留逻辑：下滑默认确认当前视频为保留并进入下一条，上滑可回到上一条重新判定，退出整理时统一提交仍暂存的视频为保留。
- 视频右侧工具组是可上下拖动的整体控件，包含数量 / 撤回、声音、收藏、待删和分享，避免固定遮挡底部信息。
- 回收站已经拆分照片 / 视频，预览页会根据来源场景显示分享、取消收藏、删除或从列表移除等操作。
- 设置中心按常用设置、媒体范围、支持与维护重新组织，只保留实际影响整理、显示、媒体范围、触觉、外观或维护能力的设置项。


## 2026-08-18 收尾

- 照片/视频滑动引导弹窗改为深色玻璃卡片风格，操作按钮与说明文字改为可本地化文案（`8d6fa26`）。
- 首次启动引导页改为固定深色渐变主题，矮屏自动使用紧凑间距与可滚动布局（`6535ee1`）。
- 设置页“视频整理页实时预览”按容器宽度自适应高度，补齐深色操作栏与底部组件预览（`5b6e239`）。
- 视频整理页顶部进度胶囊可跟随设置隐藏，顶部/底部渐变遮罩只在控制栏可见时叠加（`51f506a`）。
- 使用历史 release keystore 签名构建 `app-release.apk` 并完成证书/元数据校验（`78a4789` 与本地发布记录）。

## 继续维护重点

1. 在真机上复核 Android 13+ 的 `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` 权限和 Android 14+ 的部分媒体访问表现。
2. 继续完善移动文件夹时的 MediaStore 写入授权。若目标设备对非本应用创建的媒体抛出 `SecurityException`，应接入 `MediaStore.createWriteRequest()` 并在用户确认后重试。
3. 大相册场景下继续压测相似照片检测，重点观察指纹缓存复用、候选桶数量、后台任务进度和内存占用。
4. 真机验证照片 / 视频整理页的边缘动作反馈、时间筛选动画、全屏 / 原比例切换和预测式返回是否符合直觉。
5. 发布前检查所有公开文档、Release 草稿和截图是否与实际功能一致。

## 必须遵守

- 不引入账号、会员、VIP、支付、订阅、广告、云同步或远程授权校验。
- 不上传用户照片或视频到服务器。
- 永久删除公共媒体必须走 Android 系统授权流程，不能绕过系统确认。
- 媒体先进入 App 内回收站，再由用户二次确认删除。
- 不提交 `local.properties`、`local-private/`、签名密钥、APK/AAB、`app/build/` 或真实媒体。
- 修改 Room Entity、Dao 或数据库版本时，同步更新 `app/schemas/`。

## 公开文档入口

- `README.md`：项目总览、功能概览、快速开始和 Release 摘要。
- `docs/technical_overview.md`：技术说明、核心流程、数据与隐私边界。
- `docs/architecture.md`：架构分层、页面结构和当前架构要点。
- `docs/build.md`：Debug / Release 构建说明。
- `docs/github_publishing.md`：GitHub 发布前检查清单。
- `docs/release_notes.md`：当前 GitHub Release 草稿。
- `docs/privacy.md`：隐私说明。

## 构建与发布

常用验证命令：

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:assembleRelease --console=plain
```

当前本地发布产物：

- 未签名候选：`app/build/outputs/apk/release/app-release-unsigned.apk`
- 已签名发布 APK：`app/build/outputs/apk/release/app-release.apk`（2026-08-18 使用历史 release keystore 签名，v2，证书 SHA-256 `ade29388…dbee`）

签名密钥、密码与 APK 均不提交进 Git；签名与元数据记录见 `docs/发布候选验证记录_2026-08-14.md`。
