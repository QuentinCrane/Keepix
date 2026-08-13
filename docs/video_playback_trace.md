# 视频切页播放观测

## D09a

- `VideoCleanScreen` 的播放器创建条件仍是当前 active page；`VerticalPager` 的相邻页面只保留 Compose 内容，不创建 ExoPlayer
- 离开页面时继续移除监听器、停止播放、清空 media item 并释放播放器；没有引入跨页面共享播放器或自动预加载
- 每次播放器会话增加不含视频 id/URI 的异步 trace：创建到第一个 `STATE_READY`；同时记录 ready、错误和释放计数，便于 Perfetto/调试摘要核对播放器数量是否有界
- 这一步只增加观测，不宣称首帧时延、decoder 数、切页 PSS 或 30 次快速 fling 已验证；当前无连接设备，后续应在真实设备上决定是否需要复用或预加载
