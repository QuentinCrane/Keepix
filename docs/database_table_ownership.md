# Room 数据表用途与保留策略

当前 `AppDatabase` 的实体必须同时满足三个条件：有明确的产品用途、至少一个真实读写者、以及可验证的测试或查询合同。数据库当前使用 `fallbackToDestructiveMigration()`；版本不兼容时允许重建本地库，因此不为已删除的占位表编写虚假的迁移兼容层。

## 当前保留的表

| 表 | 用途 | 主要读写者 | 保留策略 |
| --- | --- | --- | --- |
| `photos` / `videos` | MediaStore 媒体目录、整理状态和缺失状态 | `PhotoDao` / `VideoDao`、媒体同步与清理 Repository | 直到媒体被重新扫描或按产品策略标记缺失 |
| `photo_fingerprints` | 相似照片缩略指纹 | `SimilarDao`、本地后台相似检测 | 与有效照片输入一致；输入版本变化时重算 |
| `similar_groups` / `similar_group_photos` | 相似照片候选分组 | `SimilarDao`、相似照片页面 | 下次成功分析替换；取消/失败保留旧有效结果 |
| `similar_analysis_tasks` | 后台分析状态、游标、版本和取消标记 | `SimilarDao`、WorkManager Worker | 保留当前任务快照，新的输入安全替换 |
| `trash_items` | 看了么应用回收站索引 | `TrashDao`、回收站和授权删除流程 | 直到恢复或系统授权后的永久删除完成 |
| `user_stats` | 首页快速统计缓存和连续天数物化值 | `StatsDao`、首页/我的页面 | 可由事件账本重算的缓存 |
| `achievement_progress` | 成就进度与首次解锁时间 | `StatsDao`、成就页面 | 解锁状态幂等保留，不因当前状态回退 |
| `cleanup_events` | 日报、年度报告、成就和连续天数的不可变事件账本 | `StatsDao`、清理/撤销/回收站流程 | 作为统计事实保留；后续只允许追加补偿事件 |
| `operation_history` | 清理操作和撤销锚点 | `OperationDao`、清理与撤销流程 | 至少保留可撤销历史；未来清理必须先定义导出/保留策略 |

## 本版本移除的无消费者表

以下实体曾被声明在 Room 中，但没有 DAO、Repository 或 UI 读写者，也没有可执行的产品功能依赖，因此在数据库版本 19 中移除：

- `lsh_buckets`：当前相似分析直接使用指纹分页和候选构建，不读取持久 LSH 桶
- `folder_filters`：排除文件夹已由 DataStore 的 `excluded_folder_paths` 作为唯一设置来源
- `folder_usage`：文件夹排行改由年度事件账本的 `folder_path` 聚合，不维护独立缓存
- `swipe_statistics` / `swipe_stat_sessions`：旧滑动日统计被 `cleanup_events` 的动作数和受影响媒体数取代
- `backup_import_audit_logs`：当前没有备份/恢复导入功能，不能保留没有写入者的审计表
- `device_model_cache`：当前没有设备型号解析流程，待真正引入功能时再设计专用缓存

历史 schema JSON 仍保留旧版本表结构，作为版本追踪证据；新安装和 destructive fallback 后的数据库只创建当前实体集合。
