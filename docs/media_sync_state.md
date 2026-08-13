# MediaStore 同步状态

## D04a 已落地内容

- Room 新增 `media_sync_state` 表，按 MediaStore volume 保存：volume 名称、MediaStore version、最近一次完整扫描后的 generation，以及本次扫描对应的权限范围指纹
- 图片和视频扫描都成功，并且能读取 MediaStore 同步快照时，Repository 才会在同一个 Room transaction 中写入状态
- 任意一个媒体类型扫描失败，或系统无法读取 version/generation 时，会保留上一次状态，避免把不完整扫描误记为可增量续接的检查点
- 权限范围指纹来自当前 `MediaAccessState.key`；当前块只持久化它，尚未用它决定增量扫描或删除

## 当前边界

本块没有改变现有全量扫描行为。下一块才会在 volume、version、权限范围和 generation 均可比较时接入 `GENERATION_ADDED` / `GENERATION_MODIFIED` 查询；版本变化、权限范围变化或游标无效时仍应回退到全量重建。

## 验证

- `AppDatabaseTest` 覆盖同步状态的写入和读取
- 设备上的 MediaStore version/generation、SD 卡 volume、部分相册权限以及进程重启场景仍需在 D04 后续块接入后进行设备验收
