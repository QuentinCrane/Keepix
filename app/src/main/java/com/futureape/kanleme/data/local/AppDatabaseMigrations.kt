package com.futureape.kanleme.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Direct upgrade paths for exported schemas, including the published v15 and v16 databases. */
internal object AppDatabaseMigrations {
    val ALL: Array<Migration> = (13 until 22).map { fromVersion ->
        object : Migration(fromVersion, 22) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateTo22(db, fromVersion)
        }
    }.toTypedArray()

    private fun migrateTo22(db: SupportSQLiteDatabase, fromVersion: Int) {
        if (fromVersion < 14) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `operation_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `media_id` INTEGER NOT NULL, `media_type` TEXT NOT NULL, `previous_processing_status` TEXT NOT NULL,
                    `previous_deletion_status` TEXT NOT NULL, `new_processing_status` TEXT NOT NULL,
                    `new_deletion_status` TEXT NOT NULL, `action` TEXT NOT NULL, `created_at` INTEGER NOT NULL,
                    `is_undone` INTEGER NOT NULL)""".trimIndent(),
            )
        }
        if (fromVersion < 18) migrateLegacyEvents(db, fromVersion)

        if (fromVersion < 17) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `similar_analysis_tasks` (`id` INTEGER NOT NULL, `status` TEXT NOT NULL,
                    `run_token` TEXT NOT NULL, `media_version` TEXT NOT NULL, `access_key` TEXT NOT NULL,
                    `algorithm_version` INTEGER NOT NULL, `requested_limit` INTEGER NOT NULL, `cursor` INTEGER NOT NULL,
                    `total_photos` INTEGER NOT NULL, `stage` TEXT NOT NULL, `cancel_requested` INTEGER NOT NULL,
                    `last_result_count` INTEGER NOT NULL, `last_error` TEXT, `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))""".trimIndent(),
            )
        }
        if (fromVersion < 20) {
            createMediaSyncState(db)
        } else if (fromVersion == 20) {
            // v20 had no completion checkpoint; force a full rescan without losing its cursor.
            db.execSQL("ALTER TABLE `media_sync_state` RENAME TO `media_sync_state_before_v22`")
            createMediaSyncState(db)
            db.execSQL(
                """INSERT INTO `media_sync_state`
                    (`sync_key`, `volume_name`, `media_store_version`, `last_generation`,
                     `permission_scope_fingerprint`, `updated_at`, `checkpoint_state`, `checkpoint_generation`)
                    SELECT `sync_key`, `volume_name`, `media_store_version`, `last_generation`,
                     `permission_scope_fingerprint`, `updated_at`, 'running', `last_generation`
                    FROM `media_sync_state_before_v22`""".trimIndent(),
            )
            db.execSQL("DROP TABLE `media_sync_state_before_v22`")
        }

        // Old tables without current readers are intentionally retained; migration must not
        // discard user data just because a later schema stopped using a table.
        db.execSQL("DROP INDEX IF EXISTS `index_photos_processing_status_deletion_status`")
        db.execSQL("DROP INDEX IF EXISTS `index_videos_processing_status_deletion_status`")
        val indexes = listOf(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_photos_media_store_id` ON `photos` (`media_store_id`)",
            "CREATE INDEX IF NOT EXISTS `index_photos_active_date` ON `photos` (`deletion_status`, `missing_since`, `date_taken`)",
            "CREATE INDEX IF NOT EXISTS `index_photos_queue_date` ON `photos` (`processing_status`, `deletion_status`, `missing_since`, `date_taken`)",
            "CREATE INDEX IF NOT EXISTS `index_photos_favorite_updated` ON `photos` (`processing_status`, `deletion_status`, `missing_since`, `updated_at`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_videos_media_store_id` ON `videos` (`media_store_id`)",
            "CREATE INDEX IF NOT EXISTS `index_videos_active_date` ON `videos` (`deletion_status`, `missing_since`, `date_taken`)",
            "CREATE INDEX IF NOT EXISTS `index_videos_queue_date` ON `videos` (`processing_status`, `deletion_status`, `missing_since`, `date_taken`)",
            "CREATE INDEX IF NOT EXISTS `index_videos_favorite_updated` ON `videos` (`processing_status`, `deletion_status`, `missing_since`, `updated_at`)",
            "CREATE INDEX IF NOT EXISTS `index_trash_items_media_id_media_type` ON `trash_items` (`media_id`, `media_type`)",
            "CREATE INDEX IF NOT EXISTS `index_operation_history_created_at` ON `operation_history` (`created_at`)",
            "CREATE INDEX IF NOT EXISTS `index_operation_history_media_type` ON `operation_history` (`media_type`)",
            "CREATE INDEX IF NOT EXISTS `index_operation_history_undo` ON `operation_history` (`is_undone`, `action`, `media_type`, `created_at`)",
            "CREATE INDEX IF NOT EXISTS `index_operation_history_media_action` ON `operation_history` (`media_id`, `media_type`, `action`, `is_undone`, `created_at`)",
            "CREATE INDEX IF NOT EXISTS `index_cleanup_events_event_type` ON `cleanup_events` (`event_type`)",
            "CREATE INDEX IF NOT EXISTS `index_cleanup_events_local_date` ON `cleanup_events` (`local_date`)",
            "CREATE INDEX IF NOT EXISTS `index_cleanup_events_occurred_at` ON `cleanup_events` (`occurred_at`)",
            "CREATE INDEX IF NOT EXISTS `index_cleanup_events_operation_id` ON `cleanup_events` (`operation_id`)",
            "CREATE INDEX IF NOT EXISTS `index_cleanup_events_reversal_of_event_id` ON `cleanup_events` (`reversal_of_event_id`)",
            "CREATE INDEX IF NOT EXISTS `index_cleanup_events_source` ON `cleanup_events` (`source_ref`, `reversal_of_event_id`, `occurred_at`)",
            "CREATE INDEX IF NOT EXISTS `index_similar_analysis_tasks_status` ON `similar_analysis_tasks` (`status`)",
            "CREATE INDEX IF NOT EXISTS `index_similar_analysis_tasks_updated_at` ON `similar_analysis_tasks` (`updated_at`)",
        )
        indexes.forEach(db::execSQL)
    }

    private fun migrateLegacyEvents(db: SupportSQLiteDatabase, fromVersion: Int) {
        val legacyTable = "cleanup_events_legacy_v" + fromVersion
        db.execSQL("ALTER TABLE `cleanup_events` RENAME TO `$legacyTable`")
        listOf("event_type", "local_date", "occurred_at").forEach {
            db.execSQL("DROP INDEX IF EXISTS `index_cleanup_events_" + it + "`")
        }
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `cleanup_events` (`event_id` TEXT NOT NULL, `event_type` TEXT NOT NULL,
                `operation_id` INTEGER, `media_type` TEXT NOT NULL, `photo_delta` INTEGER NOT NULL,
                `video_delta` INTEGER NOT NULL, `affected_media_count` INTEGER NOT NULL, `media_size_bytes` INTEGER NOT NULL,
                `size_delta_bytes` INTEGER NOT NULL, `favorite_delta` INTEGER NOT NULL, `action_count` INTEGER NOT NULL,
                `local_date` TEXT NOT NULL, `occurred_at` INTEGER NOT NULL, `source_type` TEXT NOT NULL, `source_ref` TEXT,
                `reversal_of_event_id` TEXT, `folder_path` TEXT, `created_at` INTEGER NOT NULL, PRIMARY KEY(`event_id`))""".trimIndent(),
        )
        db.execSQL(
            """INSERT INTO `cleanup_events` (
                `event_id`, `event_type`, `operation_id`, `media_type`, `photo_delta`, `video_delta`,
                `affected_media_count`, `media_size_bytes`, `size_delta_bytes`, `favorite_delta`, `action_count`,
                `local_date`, `occurred_at`, `source_type`, `source_ref`, `reversal_of_event_id`, `folder_path`, `created_at`
            ) SELECT
                e.`event_id`,
                CASE o.`action` WHEN 'Keep' THEN 'kept' WHEN 'Favorite' THEN 'favorited'
                    WHEN 'Delete' THEN 'moved_to_app_trash' ELSE 'legacy_clean_action' END,
                o.`id`,
                CASE WHEN o.`media_type` IN ('photo', 'video') THEN o.`media_type`
                    WHEN e.`photo_count` > 0 AND e.`video_count` = 0 THEN 'photo'
                    WHEN e.`video_count` > 0 AND e.`photo_count` = 0 THEN 'video'
                    ELSE 'unknown' END,
                e.`photo_count`, e.`video_count`,
                CASE WHEN e.`photo_count` + e.`video_count` > 0 THEN e.`photo_count` + e.`video_count` ELSE 1 END,
                MAX(e.`freed_bytes`, 0), e.`freed_bytes`,
                CASE WHEN o.`action` = 'Favorite' AND o.`previous_processing_status` != 'favorited'
                    AND o.`new_processing_status` = 'favorited' THEN 1 ELSE 0 END,
                1, e.`local_date`, e.`occurred_at`, e.`source_type`, e.`source_ref`, NULL, NULL, e.`created_at`
            FROM `$legacyTable` AS e LEFT JOIN `operation_history` AS o
                ON o.`id` = CAST(SUBSTR(e.`source_ref`, 11) AS INTEGER)
                AND e.`source_ref` = 'operation:' || CAST(o.`id` AS TEXT)""".trimIndent(),
        )
    }

    private fun createMediaSyncState(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `media_sync_state` (`sync_key` TEXT NOT NULL, `volume_name` TEXT NOT NULL,
                `media_store_version` TEXT NOT NULL, `last_generation` INTEGER NOT NULL,
                `permission_scope_fingerprint` TEXT NOT NULL, `updated_at` INTEGER NOT NULL,
                `checkpoint_state` TEXT NOT NULL, `checkpoint_generation` INTEGER NOT NULL, PRIMARY KEY(`sync_key`))""".trimIndent(),
        )
    }
}
