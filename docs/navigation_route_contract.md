# 导航与入口约定

## C07 结论

主导航保留三个入口：`照片`、`视频`、`我的`。照片和视频分别进入对应清理流；主路由在三项之间按顺序做普通的左右平移切换。

照片和视频从首页直接进入各自的清理流。`PHOTO_START`、`VIDEO_START` 原本只在 `NavHost` 中声明，没有首页、底部导航、快捷方式或其他 `navigate` 调用；它们已连同无用启动页实现一起删除。

`FloatingGlassNav` 负责手机上的三个主导航项，宽屏横向平板使用 `FloatingGlassRail`。切换动画只在主导航的 `HOME`、`VIDEO`、`ME` 三个路由之间水平平移；打开其他子页面仍使用原有返回与共享转场。

## 当前 route 图

```text
应用壳
└── Onboarding（未完成时拦截）
    └── MediaPermissionGate（权限未就绪时拦截）
        └── HOME
            ├── PHOTO ──> VIEWER(photoId)
            ├── VIDEO
            ├── TIMELINE ──> VIEWER(photoId)
            ├── TODAY ──> VIEWER(photoId)
            ├── FAVORITES ──> VIEWER(photoId)
            ├── TRASH
            ├── SETTINGS[/page]
            └── ME
                ├── FAVORITES
                ├── TRASH
                ├── PHOTO_HISTORY
                ├── ADVANCED
                │   ├── ADVANCED_PHOTO（扫描、筛选、内部预览、移动、移入应用回收站）
                │   ├── ADVANCED_VIDEO（扫描、筛选、内部播放弹窗、外部打开）
                │   └── OPEN_SOURCE（第三方归属与许可证全文）
                ├── SIMILAR ──> VIEWER(photoId)
                ├── ACHIEVEMENTS
                ├── ANNUAL
                └── SETTINGS[/page]
```

底部导航只在 `HOME`、`VIDEO`、`ME` 显示；从 `VIDEO` 返回时回到 `HOME`，照片入口和其他子页面使用系统返回栈回退。

## 外部快捷方式

`MainActivity` 只接受 `com.futureape.kanleme.ACTION_CLEAN`，其 `shortcut_target` 由 `NavigationContract` 映射：

| target | 真实 route | 行为 |
| --- | --- | --- |
| `photo` | `PHOTO` | 启动照片清理会话并直达照片清理流 |
| `video` | `VIDEO` | 启动视频清理会话并直达视频清理流 |
| 其他/缺失 | 无 | 保持当前导航，不创建未知 route |

## 验证边界

- JVM 测试锁定快捷方式到真实 route 的映射、未知 target 不会恢复旧启动页、主导航顺序
- Compose AndroidTest 锁定主导航的可见项
- `git grep` 不应再找到 `PHOTO_START`、`VIDEO_START` 或 `FloatingGlassNav` 的 `onAdd` 参数
- 本地构建验证 route 删除不会破坏返回栈、快捷方式入口或进程恢复所需的导航状态
