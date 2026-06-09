# GitHub 发布指南

这份清单用于把“看了么”公开到 GitHub 前做最后整理。

## 当前结论

项目源码结构已经接近可公开状态：Android 工程文件、Gradle Wrapper、README、技术说明、构建说明和开发记录都比较完整。正式公开前最需要补的是截图，以及确认不要误传本地私有目录。

## 推荐仓库结构

```text
.
├── .github/                  GitHub Actions、Issue、PR 模板
├── app/                      Android App 模块
├── docs/                     技术说明、隐私说明、发布指南、开发记录
├── gradle/wrapper/           Gradle Wrapper
├── scripts/windows/          Windows 辅助构建脚本
├── README.md                 GitHub 首页展示
├── CONTRIBUTING.md           贡献指南
├── SECURITY.md               安全报告说明
├── local.properties.example  本机配置模板
└── .gitignore                本地文件与构建产物忽略规则
```

## 发布前必须检查

1. 不要手动拖整个文件夹到网页上传。建议用 Git 添加文件，让 `.gitignore` 生效。
2. 确认以下内容没有进入暂存区：
   - `local.properties`
   - `local-private/`
   - `*.keystore`、`*.jks`、`*.p12`、`*.pfx`
   - `*.apk`、`*.aab`
   - `app/build/`、`.gradle/`、`.kotlin/`
   - 真实照片、视频、定位日志或个人路径日志
3. 确认 `LICENSE` 和 `NOTICE` 已随仓库提交；本项目使用 `Apache-2.0 + NOTICE` 来保留原始项目归属。
4. 如准备展示截图，补充到 `docs/screenshots/`，并在 README 新增“项目预览”段落后引用真实图片。
5. 首次推送后检查 GitHub Actions 的 Android CI 是否能通过。

## 建议命令

如果本地还不是 Git 仓库：

```powershell
git init
git branch -M main
git add -A
git status --short --ignored
```

确认私有文件被忽略：

```powershell
git check-ignore -v local-private/kanleme.keystore
git check-ignore -v local.properties
```

本地构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

准备 Release 时再运行：

```powershell
.\gradlew.bat :app:assembleRelease --console=plain
```

## 许可证

本项目已采用 `Apache-2.0`，并额外提供 `NOTICE` 文件声明原始项目来源：

```text
https://github.com/QuentinCrane/Keepix
```

这意味着别人可以使用、修改和分发项目，但需要保留许可证与 NOTICE 中的归属声明。

## GitHub 仓库设置建议

- About：填写一句话简介，例如 `A local-first Android media organizer for safer photo and video cleanup.`
- Topics：`android`、`kotlin`、`jetpack-compose`、`room`、`hilt`、`mediastore`、`local-first`
- Features：开启 Issues、Actions；如果接受外部贡献，再开启 Discussions。
- Branch protection：公开后可给 `main` 开启 PR + CI 通过要求。

## Release 建议

首个 GitHub Release 可以包含：

- 版本号与核心功能列表。
- Debug APK 或 Release APK。如果发布 Release APK，只使用你自己的 keystore。
- Release 附件建议上传已签名 APK 或压缩包，不要上传未签名中间产物。
- 权限说明和隐私边界链接。
- 已知问题，例如不同 Android 版本的相册权限差异。

## 当前 Release 文案要点

本次发布建议在 GitHub Releases 中突出以下内容：

- 首页现代化：白色 / 浅蓝主视觉，横向滑动切换照片整理和视频整理，恢复最近新增照片 / 视频入口，并清理只服务装饰的背景几何元素。
- 照片整理升级：边缘动作反馈、短进度胶囊、左下角数量 / 撤回按钮、带进出场动画的全屏年份 / 月份筛选和指定归档。
- 视频整理升级：下滑暂存保留、上滑回看重判、退出统一提交保留，右侧可拖动整体工具组和数量 / 撤回按钮。
- 媒体查看器升级：照片放大全屏查看、底部缩略图、收藏 / 移动 / 待删 / EXIF / 回到整理等操作。
- 回收站升级：照片 / 视频分离管理，预览页按场景显示分享、取消收藏、删除或从列表移除。
- 设置中心统一：移除首页式总览卡，按常用设置、媒体范围、支持维护重新组织一级入口；只保留实际生效的设置项，底部弹窗字号和卡片风格统一。
- 发布优化：Release 构建保持 R8 压缩和资源裁剪，签名 APK 约 6.2 MiB。

发布附件不要包含签名密钥、密码、本地配置或未压缩的构建中间产物。

完整发布草稿见 [Release 发布说明](release_notes.md)。
