# 数据库查询优化记录

## D05a — dashboard typed projection

首页、`我的`页和清理页共享的 dashboard 状态原来由 15 路 Flow 组成，并在 Repository 中通过 `Any` 数组按位置转换。现在由 `StatsDao.observeDashboardCounts()` 返回一个 typed projection，再与 `user_stats` 单独合并。

projection 保留了原查询的统计语义：媒体总数、已处理数、收藏数、应用回收站数、待删除数量与字节数，以及指定本地日期的清理事件聚合。所有计数仍由 Room/SQLite 计算，没有把全表媒体实体加载到 Kotlin 内存。

照片类型统计也改为一个 `PhotoTypeStatsProjection`，保留“正常/截图/自拍/实况/动图/长图”可重叠分类以及 active 总数语义。

## 当前边界

- 本块没有凭字段名新增索引
- 清理队列、时间线和相似检测的大实体查询仍待后续块按真实 projection 和基准数据集收敛
- `StatsDao` 的 typed projection 已有 AndroidTest 覆盖；当前环境无设备，测试只完成编译，不能替代仪器执行
