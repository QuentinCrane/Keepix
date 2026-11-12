# 第三方播放器来源与许可

## 本次采用的项目

| 项目 | 固定版本 | 许可 | 使用范围 |
| --- | --- | --- | --- |
| [AndroidX Media3](https://github.com/androidx/media/tree/1.11.0) | 1.11.0 | [Apache License 2.0](https://github.com/androidx/media/blob/1.11.0/LICENSE) | 复用 `media3-exoplayer`、`media3-ui` 的 ExoPlayer 和 PlayerView；新增 `media3-inspector-frame`、`media3-effect`，为进阶视频列表本地提取缩略图 |
| [PlayerActivity 官方示例](https://github.com/androidx/media/blob/1.11.0/demos/main/src/main/java/androidx/media3/demo/main/PlayerActivity.java) | 1.11.0 标签 | Apache License 2.0；Copyright (C) 2016 The Android Open Source Project | 创建播放器、音频焦点、保存播放位置与生命周期释放流程，改写为 `LightweightVideoViewer.kt` |

改写增加 Compose Dialog、单个本地 content URI、播放器局部静音、倍速及外部查看入口，保留原示例版权与许可头。封面帧提取在本地执行。本次未复制官方示例中的网络、DRM、广告、下载逻辑，也未新增第二个播放器。

许可证全文从上游 1.11.0 标签保存到 [apache_2_0.txt](../app/src/main/res/raw/apache_2_0.txt)，归属说明写入根目录 [NOTICE](../NOTICE)。App 在“我的 → 高级功能 → 开源声明”展示项目名、作者、版本、来源、修改说明和可离线阅读的许可证全文。

## GitHub 调研记录（2026-10-05）

- [AndroidX Media3](https://github.com/androidx/media)：与现有依赖完全一致，官方提供可直接复用的控件和生命周期示例，选用。
- [Just Player](https://github.com/moneytoo/Player)：轻量独立播放器，仓库显示 Unlicense，完整应用还包含 FFmpeg 扩展及丰富手势；本次只评估产品范围，未复制其代码、资源或二进制。
- [Next Player](https://github.com/anilbeesetti/nextplayer)：完整的 Kotlin 视频播放器，可用于后续外部查看器兼容测试；本次未复制其代码、资源或二进制。

以上未采用项目不是本应用捆绑的第三方软件，不在 App 内作为依赖声明。若后续引用代码，需先核对对应版本与具体文件的许可，再补齐归属和修改说明。
