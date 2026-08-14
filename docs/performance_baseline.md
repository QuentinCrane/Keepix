# 性能实验室基线记录

本文件是可重复记录模板，不是估算结果。D01 已创建 `:benchmark`、确定性元数据 fixture 和冷启动测量入口；2026-08-14 已在本机完成两个 benchmark Release 变体的配置构建，但当前工作环境没有连接 Android 设备，因此真实指标仍保持 `not-run`。

本机配置构建命令：

```powershell
.\gradlew.bat :benchmark:assembleBenchmarkRelease :benchmark:assembleNonMinifiedRelease --offline --configuration-cache --console=plain --no-daemon --max-workers=1
```

结果：明确返回 `Exit code: 0`，`BUILD SUCCESSFUL in 1m 59s`；这不是设备性能数据，也不填充下面的 Baseline CSV。

## Fixture scales

`FixtureManifest.supportedScales` 固定为 `0, 100, 1000, 10000, 50000`。fixture 只生成 `media_store_id`、媒体类型、文件夹、体积和拍摄时间，不生成或提交媒体二进制内容。

## Required run metadata

| 字段 | 值 |
| --- | --- |
| app commit | `not-run` |
| build variant | `release-like profileable` |
| device / Android / API | `not-run` |
| battery / temperature | `not-run` |
| fixture scale | `not-run` |
| iterations | `5` for startup; `10` or more for journey baselines |
| cache state | `cold` / `warm`, each recorded separately |

## Baseline CSV

| journey | fixture_scale | cache | metric | median_ms | p95_ms | pss_mb | dropped_frames | status |
| --- | ---: | --- | --- | ---: | ---: | ---: | ---: | --- |
| cold_startup | 0 | cold | TTID/TTFD |  |  |  |  | not-run |
| cold_startup | 100 | cold | TTID/TTFD |  |  |  |  | not-run |
| home_first_preview | 1000 | warm | first_frame |  |  |  |  | not-run |
| photo_clean_30_swipes | 1000 | warm | journey_time |  |  |  |  | not-run |
| photo_grid_scroll | 10000 | warm | frame_time |  |  |  |  | not-run |
| video_page_10_switches | 1000 | warm | first_frame |  |  |  |  | not-run |
| trash_batch_select | 10000 | warm | journey_time |  |  |  |  | not-run |
| similar_analysis | 10000 | warm | photos_per_second |  |  |  |  | not-run |

Do not fill this table from a Debug build, an emulator without noting it, or code inspection. Save Perfetto traces and AndroidX benchmark reports outside the repository unless they contain only synthetic, non-sensitive data selected for publication.
