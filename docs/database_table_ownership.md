# Room 数据表用途与保留策略

当前 `AppDatabase` 的实体必须同时满足三个条件：有明确的产品用途、至少一个真实读写者、以及可验证的测试或查询合同。数据库版本 13～21 提供到版本 22 的迁移；已发布的 2.3.3 和 2.3.4 分别使用版本 15 和 16。未支持的版本或迁移失败不会自动删除旧库。

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

历史 schema JSON 仍保留旧版本表结构，作为版本追踪和迁移测试依据。新安装只创建当前实体集合；升级时没有当前消费者的旧表保留在数据库中，不再由现有功能读取。旧版 `cleanup_events` 原表也会保留，新表中的历史事件由旧记录及可关联的操作记录转换；旧版未记录的字段不能保证完整还原。

版本 20 的媒体同步状态还没有完整性检查点；迁移保留原同步代数并标记为待恢复，以便下一次同步执行完整扫描，不把旧检查点误当作已完成。
