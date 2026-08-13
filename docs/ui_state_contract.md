# 屏幕 UiState 与 Flow 订阅合同

## D06a

- Repository 的长生命周期观察流统一通过 `SharingStarted.WhileSubscribed(5_000)` 暴露；离开目的地后，上游在短暂重进缓冲后停止，不在 ViewModel 中持续执行全屏列表查询
- `MeScreen` 现在只订阅一个 `MeScreenUiState`，内部包含 dashboard 和照片类型统计
- `SimilarPhotosScreen` 现在只订阅一个 `SimilarPhotosUiState`，内部包含相似组和检测进度
- 删除了无消费者的 `similarDetectionRunning` 二次派生 StateFlow，避免重复状态管线
- 其他清理屏幕的高频 deck、设置和动作状态仍保持独立，因为它们有不同的生命周期和交互频率；后续改动必须以 Compose/Perfetto 观测为依据，不把所有状态盲目合并

当前没有设备上的 recomposition/frame trace，因此本块不宣称已完成屏幕级性能验收；此处先固定屏幕状态边界和订阅生命周期，后续 D07/D10 再根据实测热点调整。
