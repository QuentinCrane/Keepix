# 架构说明

## 技术栈

- Kotlin 2.0.21
- Android Gradle Plugin 8.8.2
- Jetpack Compose + Material 3
- Hilt
- Room + Flow
- MediaStore
- DataStore 预留
- WorkManager 预留
- Media3 预留

## 分层

```text
app/
  MainActivity.kt
  KanlemeApplication.kt
  data/
    local/          Room Entity/Dao/Database
    media/          MediaStore 扫描
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
  └── 待删/收藏/回收入口

我的 Me
  ├── 统计与成就
  ├── 我的收藏 Favorites
  ├── 回收站 Trash
  └── 设置 Settings
```

## 液态玻璃转译原则

Apple Liquid Glass 属于 Apple 平台系统材料，Android 上不能直接调用，因此这里采用“理念转译”：

- 内容优先：照片/视频永远比装饰层更清晰；
- 导航层浮起：底部导航不贴边，使用浮动胶囊；
- 透明但可读：玻璃层始终带半透明底色、边框和阴影；
- 动态色适配：优先使用 Android Material You dynamic color；
- edge-to-edge：内容延伸到系统栏下方，并用 WindowInsets 避免遮挡。
