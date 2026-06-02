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
4. 补充截图到 `docs/screenshots/`，并在 README 的“项目预览”处替换为真实图片。
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
- 权限说明和隐私边界链接。
- 已知问题，例如不同 Android 版本的相册权限差异。

## v2.2.9 Release 文案要点

本次发布建议在 GitHub Releases 中突出以下内容：

- GIF 动图支持：整理页、收藏、回收站和大图预览可直接播放动图。
- 实况 / 动态照片支持：识别部分 Android Motion Photo、实况照片和同名伴随视频段，当前整理卡片可自动播放一次，预览场景也支持播放。
- 照片整理体验优化：恢复轻量媒体扫描，实况解析改为当前卡片懒加载，照片信息统一收敛到底部栏。
- 当年今日与回收站优化：长方形照片墙、更少文件信息、回收站显示预计可释放空间、照片 / 视频数量与 30 天恢复提示。
- 相似图片检测优化：后台检测、进度显示、继续检测、指纹缓存复用，多特征检测包含 pHash、dHash、aHash、颜色特征和候选桶匹配。
- 成就系统升级：本地成就陈列柜，支持完成率、XP、稀有度、分类筛选与解锁状态。
- 发布优化：Release 构建启用 R8 压缩和资源裁剪，APK 体积从约 54 MB 降至约 6.1 MB。

发布附件不要包含签名密钥、密码、本地配置或未压缩的构建中间产物。
