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

## D04c 已落地内容

- 增量查询使用 `(previousGeneration, startGeneration]` 窗口；扫描期间发生的新变化留给下一次同步，避免把“扫描开始后才发生”的行漏掉。全量扫描完成时保留扫描开始游标，同样让扫描期间的变化在下一次增量中补齐
- 同步前先提交 `running` checkpoint；取消、崩溃或任一查询失败后不会写成 `complete`，下次同步会保守地回到全量路径
- 只有完整图片/视频权限、generation 确实前进且明确允许增量时，才对 active MediaStore ID 做存在性探测；系统 trash 和外部不可见 ID 标记为 `missing_since`，不直接删除 Room 行
- 部分权限、权限范围变化以及选中照片权限不会执行删除 reconciliation，因此不会把不可见媒体误判为已删除
- 删除探测失败时跳过本轮缺失标记，并保留 `running` checkpoint；下一次同步从全量路径重试，避免把已前进的 generation 当成已完成

增量查询负责发现新增/修改项，并在完整权限下做有限的缺失探测；它不永久删除数据。SD 卡和多 volume 的真实设备验收仍需在设备可用后完成，当前代码仍使用 `VOLUME_EXTERNAL` 聚合视图。

## 验证

- `AppDatabaseTest` 覆盖同步状态的写入和读取
- 设备上的 MediaStore version/generation、SD 卡 volume、部分相册权限以及进程重启场景仍需在 D04 后续块接入后进行设备验收
