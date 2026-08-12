# 音效许可记录

## 决策

仓库曾包含三个被重命名的 MP3，并在代码注释中称其来自 Universfield，但没有保留原始页面、下载记录、文件对应关系、许可证快照或作者授权凭证。仅凭文件名和搜索结果无法证明当前二进制就是某个公开页面上的具体素材，因此不把推测性的来源写入许可声明。

A07 移除了这三个未核验二进制，照片滑动反馈改为调用 Android `AudioManager` 的平台音效。三种设置仍保留为三种系统反馈映射；它们不再依赖第三方音频文件，也不需要在仓库中分发第三方音频。

## 核验记录

- 代码入口：`app/src/main/java/com/futureape/kanleme/ui/util/PhotoSwipeSoundPlayer.kt`
- 发布声明：`NOTICE` 的 Audio notice 段落
- 资源核验：`app/src/main/res/raw/` 不再包含照片滑动 MP3
- 设备待办：在 API 30/API 36 设备上听测三种反馈、系统静音、媒体音量和页面离开后的行为

## 参考页面

以下页面证明 Pixabay 内容许可的公开摘要和 Universfield 页面信息，但不作为已删除 MP3 的来源证明：

- https://pixabay.com/sound-effects/film-special-effects-swoosh-06-351021/
- https://pixabay.com/sound-effects/film-special-effects-air-swoosh-352049/
- https://pixabay.com/service/license-summary/
