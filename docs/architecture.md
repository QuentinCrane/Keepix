# 架构说明

## 技术栈

- Kotlin 2.0.21
- Android Gradle Plugin 8.8.2
- Jetpack Compose + Material 3
- Hilt
- Room + Flow
- MediaStore
- DataStore
- WorkManager
- Media3 / ExoPlayer
- Coil GIF Decoder
- ExifInterface

## 分层

```text
app/
  MainActivity.kt
  KanlemeApplication.kt
  data/
    local/          Room Entity/Dao/Database
    media/          MediaStore 扫描、动态照片识别、相似图片分析
    repository/     业务仓库
  di/               Hilt 注入模块
  ui/
    components/     液态玻璃组件、通用卡片
    navigation/     Compose Navigation
    screens/        页面
    theme/          Material 3 主题
    viewmodel/      AppViewModel
```

## 页面结构

```text
整理 Home
  ├── 照片整理 PhotoClean
  ├── 视频整理 VideoClean
  ├── 相似照片 SimilarPhotos
  ├── 当年今日 TodayInHistory
  └── 待删/收藏/回收入口

我的 Me
  ├── 统计与成就
  ├── 我的收藏 Favorites
  ├── 回收站 Trash
  └── 设置 Settings
```

## v2.2.9 架构要点

- GIF 播放通过全局图片加载链路支持，整理页、收藏、回收站、当年今日和大图预览复用同一套展示能力。
- 实况 / 动态照片识别放在媒体层，通过 MediaStore 元数据、文件线索、轻量片段检测和同名伴随视频段建立本地引用；照片整理页对当前卡片懒加载，避免拖慢首次进入。
- 相似图片检测在本机后台执行，复用 Room 指纹缓存和候选桶，结合 pHash、dHash、aHash、颜色特征降低大相册压力。
- 成就系统只依赖本地整理记录、统计和进度数据，不引入账号、远程授权或云同步。
- 回收站和当年今日的照片墙布局以媒体预览为中心，文件信息收敛到必要位置。

## 液态玻璃转译原则

Apple Liquid Glass 属于 Apple 平台系统材料，Android 上不能直接调用，因此这里采用“理念转译”：

- 内容优先：照片/视频永远比装饰层更清晰；
- 导航层浮起：底部导航不贴边，使用浮动胶囊；
- 透明但可读：玻璃层始终带半透明底色、边框和阴影；
- 动态色适配：优先使用 Android Material You dynamic color；
- edge-to-edge：内容延伸到系统栏下方，并用 WindowInsets 避免遮挡。
