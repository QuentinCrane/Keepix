# 隐私说明

看了么围绕 Android 本机媒体库工作，目标是帮助用户整理手机里的照片和视频。项目设计遵循本地优先原则。

## 处理的数据

App 可能在本机读取或保存以下信息：

- 照片和视频的 MediaStore 记录，例如 URI、文件名、大小、时间、宽高、时长、文件夹路径。
- 用户整理状态，例如保留、收藏、加入回收站、恢复、排除文件夹。
- 本地统计数据，例如整理数量、释放空间估算、成就进度。
- 本地设置，例如主题、手势、触觉反馈、默认整理规则。

这些数据主要保存在设备上的 Room 数据库和 DataStore 中。

## 权限用途

- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`：读取用户授权的图片和视频列表。
- `READ_MEDIA_VISUAL_USER_SELECTED`：兼容 Android 14+ 的部分媒体授权。
- `READ_EXTERNAL_STORAGE`：兼容 Android 12 及以下媒体读取。
- `ACCESS_MEDIA_LOCATION`：在用户授权后读取媒体位置元数据。
- `POST_NOTIFICATIONS`：用于可能的整理提醒或后台任务通知。
- `VIBRATE`：用于整理动作和导航反馈。
- `INTERNET`：保留给系统组件或后续明确功能；核心整理流程不依赖上传媒体。

## 不做的事情

- 不强制登录。
- 不上传照片或视频。
- 不做云同步。
- 不做广告追踪。
- 不包含会员、VIP、支付、订阅或远程授权校验。
- 不绕过 Android 系统确认永久删除公共媒体。

## 开发与发布注意

公开仓库前请确认没有提交真实媒体、签名密钥、`local.properties`、`local-private/` 或包含个人路径的日志。发布 APK 时请使用自己的 release keystore，并将密码保存在本机或 CI Secret 中。
