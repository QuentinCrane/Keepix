# MediaStore 同步状态

## D04a 已落地内容

- Room 新增 `media_sync_state` 表，按 MediaStore volume 保存：volume 名称、MediaStore version、最近一次完整扫描后的 generation，以及本次扫描对应的权限范围指纹
- 图片和视频扫描都成功，并且能读取 MediaStore 同步快照时，Repository 才会在同一个 Room transaction 中写入状态
- 任意一个媒体类型扫描失败，或系统无法读取 version/generation 时，会保留上一次状态，避免把不完整扫描误记为可增量续接的检查点
- 权限范围指纹来自当前 `MediaAccessState.key`；当前块只持久化它，尚未用它决定增量扫描或删除

## D04b 已落地内容

- 启动同步前读取当前 volume、MediaStore version 和 generation，并与 Room 中的上一检查点比较
- 只有四项身份一致且 generation 没有回退时，图片和视频查询才附加 `GENERATION_ADDED` / `GENERATION_MODIFIED` 条件
- 增量路径不会执行 `markAllMissing`；未发生变化的 Room 媒体记录保持原状，删除 reconciliation 留给后续块
- OEM Bundle 查询失败时使用带相同 generation 条件的 SQL fallback，不会静默退回昂贵的全量查询
- 首次同步、MediaStore version 变化、volume 变化、权限范围变化或 generation 回退，都会回到全量扫描

## 当前边界

增量查询只负责发现新增/修改项，不负责判断删除项。下一块会处理 generation/version 变化、权限缩小、系统 trash、外部删除和可恢复 checkpoint 的安全 reconciliation。

## 验证

- `AppDatabaseTest` 覆盖同步状态的写入和读取
- 设备上的 MediaStore version/generation、SD 卡 volume、部分相册权限以及进程重启场景仍需在 D04 后续块接入后进行设备验收
