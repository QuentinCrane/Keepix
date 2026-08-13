# Compose 高频路径性能合同

## D10a

- 照片清理页的媒体筛选类型改为静态列表；筛选计数列表只在 `PhotoTypeStats` 变化时重建，不随滑动反馈重建
- 照片 Deck 元信息、网格缩略图、收藏/回收站/今日回顾卡片和实况回顾缩略图的 `ImageRequest` 在媒体版本不变时由 `remember` 复用
- 视频当前页的日期/大小/目录信息只在对应媒体字段变化时重新格式化
- 现有 blur、阴影、渐变和入口动画保持不变；没有设备 GPU/Compose trace 证据时不宣称 overdraw 或帧时间改善，也不整体关闭 Liquid Glass 视觉
- 高风险副作用仍以 `LaunchedEffect`/`DisposableEffect` 生命周期绑定；后续 D10b 只根据 Compose tracing 处理具体热点
