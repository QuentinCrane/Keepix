# 清理动作契约

C06 把一次 Keep、Favorite 或 Delete 视为一个本地 operation。`operation_history.id` 是持久化 operation ID；它与对应的统计事件 `cleanup_events.source_ref = operation:<id>` 一一关联。

## 提交顺序

1. 手势越过阈值或用户点击等价按钮时，UI 开始退出动画并立即做本地 Deck 排除；触觉属于提交意图反馈，不代表数据库已经成功
2. 相同媒体的重复点击由 ViewModel 的乐观排除/进行中集合拦截；系统授权请求由 operation tracker 拦截重复消费
3. Repository 在一个 Room transaction 中读取最新状态；如果同一动作已经生效，返回已有 operation ID 或安全空操作，不新增历史/统计
4. 对新动作一次性写入 `operation_history`、媒体状态、待删记录和用户统计；有实际统计变化时写一条带 operation ID 的 `cleanup_events`
5. 照片退出动画完成后播放声音；声音不负责重试数据库动作。数据库失败会回滚乐观 Deck 排除并显示错误
6. 撤销先检查系统媒体授权，再在一个 transaction 中恢复媒体状态、删除对应事件、标记 operation 已撤销并增加一次撤销统计；重复撤销找不到可撤销 operation 时不再产生变化

## 边界

- 快速滑动、快捷按钮双击、动画中返回和进程恢复后的重试不得重复增加清理统计
- Keep/Favorite 可以在未删除媒体上形成合法的后续状态转换；只有“目标状态已经存在”才视为同动作重试
- Delete 对 `pending` 或 `trashed` 媒体是幂等重试，不再次增加释放空间或待删数量
- 媒体行不存在或已被外部移除时，Repository 返回空 operation ID，不假装产生新的统计事件；后续媒体同步负责修正可见队列
