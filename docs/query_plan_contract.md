# 查询计划合同

## D05c

在当前 SQLite 结构上做 `EXPLAIN QUERY PLAN` 对照后，增加了以下按真实谓词顺序排列的索引：

- `photos/videos`: active + `date_taken`、清理队列 + `date_taken`、收藏 + `updated_at`
- `operation_history`: undo 查询、media/action 幂等查询
- `cleanup_events`: `source_ref` + reversal + `occurred_at` 幂等查询

清理队列和时间线原先主要使用单列索引并产生临时排序；加入复合索引后可以直接使用队列/时间索引。`AppDatabaseTest.criticalQueryPlansUseD05CompositeIndexes` 会在仪器环境中对关键 SQL 的实际 SQLite 计划做门禁。

索引不代表已完成超大数据性能验收：10k/50k 行的耗时、写放大、数据库体积和真实设备分布，仍需结合 D01 基准在设备上记录。
