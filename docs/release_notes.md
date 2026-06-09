# Release 发布说明草稿

本文档用于发布 GitHub Release 时复制使用。发布前请先确认 `app/build.gradle.kts` 中的 `versionCode` / `versionName` 是否与准备发布的 Tag 一致。

## GitHub Release 填写内容

### Tag

```text
v2.3.0
```

### Release Title

```text
看了么 2.3.0 - 现代化整理工作台与视频暂存保留
```

### Release Body

```markdown
## 主要更新

- 首页重新设计为更现代的白色 / 浅蓝整理工作台，支持横向滑动切换照片整理和视频整理，并清理无实际信息含义的装饰元素
- 恢复首页最近新增照片 / 视频入口，减少主页冗余模块
- 照片整理页优化边缘动作反馈：拖到对应边缘时才显示保留、收藏、待删提示和更明显的屏幕边缘光，并移除视觉上的内部边界框
- 照片整理新增短进度胶囊、左下角本批次数量 / 撤回按钮、带进出场动画的全屏年份 / 月份筛选和指定归档
- 视频整理采用会话暂存保留逻辑：下滑进入下一条时暂记上一条为保留，上滑可回看重判，退出时统一提交仍暂存的视频为保留；时间筛选同样带全屏进入 / 退出动画
- 视频整理右侧栏改为可上下拖动的整体工具组，包含数量 / 撤回、声音、收藏、待删、分享
- 照片 / 视频回收站分离管理，预览页根据场景提供分享、取消收藏、删除或从列表移除等操作
- 照片大图查看器升级，支持顶部媒体信息、底部缩略图和收藏、移动、待删、EXIF、回到整理等操作
- 设置中心重新整理分组，移除顶部总览卡，只保留实际生效的设置项，并统一底部弹窗的字号、卡片密度和视觉风格
- Release 构建继续启用 R8 代码压缩与资源裁剪，签名 APK 约 6.2 MiB

## 隐私与权限

- 所有核心整理流程都在本机完成
- 不强制登录，不上传照片或视频，不包含账号、会员、支付、广告、订阅、云同步或远程授权校验
- 永久删除公共媒体时仍使用 Android 系统授权流程
- 媒体整理状态、回收站、收藏、统计和设置保存在本机 Room / DataStore

## 安装说明

下载本页附件中的签名 APK，在 Android 11 及以上设备安装。首次启动后请按系统提示授权照片 / 视频访问权限；Android 14+ 如果只授权部分媒体，App 只会显示已授权内容。

## 已知说明

- 不同厂商相册对直接打开指定媒体、系统回收站和文件夹移动的支持可能存在差异
- 如果文件夹移动遇到系统写入授权限制，需要根据系统弹窗确认授权
- 相似照片检测在大相册上会消耗一定时间，结果只保存在本机
```

### 附件

上传最终签名 APK，或上传已压缩的 release zip：

```text
app/build/outputs/apk/release/app-release-signed.apk
app/build/outputs/release-package/kanleme-v2.3.0-release.zip
```

不要上传：

- `app-release-unsigned.apk`
- `app-release-aligned.apk`
- `*.idsig`
- `local.properties`
- `local-private/`
- `*.keystore`、`*.jks`、`*.p12`、`*.pfx`
- 任何 APK / AAB 以外的构建中间产物

## 发布前检查

- [ ] `git status --short` 中没有 `local.properties`、`local-private/`、签名密钥、APK、AAB 或 `app/build/`
- [ ] `README.md`、`docs/build.md`、`docs/github_publishing.md` 与本发布说明口径一致
- [ ] `.\gradlew.bat :app:assembleDebug --console=plain` 通过
- [ ] `.\gradlew.bat :app:assembleRelease --console=plain` 通过
- [ ] 最终 APK 已执行 `zipalign`
- [ ] 最终 APK 已用 release keystore 签名
- [ ] `apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release-signed.apk` 通过
- [ ] 真机验证照片整理、视频整理、回收站、收藏、设置页、媒体权限和永久删除系统确认流程
