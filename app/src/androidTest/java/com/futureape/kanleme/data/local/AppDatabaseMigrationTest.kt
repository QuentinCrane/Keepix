package com.futureape.kanleme.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseNames = mutableListOf<String>()

    @After
    fun cleanup() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun exportedVersionsPreserveMediaTrashStatsHistoryAndLegacyRows() = runBlocking {
        for (version in 13..21) {
            val name = "migration-$version.db".also(databaseNames::add)
            createLegacyDatabase(name, version).use { db -> seedDatabase(db, version) }

            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(*AppDatabaseMigrations.ALL).allowMainThreadQueries().build()
            try {
                val db = migrated.openHelper.writableDatabase
                assertEquals(2L, scalar(db, "SELECT COUNT(*) FROM photos"))
                assertEquals(2L, scalar(db, "SELECT COUNT(*) FROM videos"))
                assertEquals(2L, scalar(db, "SELECT COUNT(*) FROM trash_items"))
                assertEquals(7L, scalar(db, "SELECT total_photos_cleared FROM user_stats WHERE id = 1"))
                assertEquals(1L, scalar(db, "SELECT unlocked FROM achievement_progress WHERE achievement_id = 'first'"))
                assertEquals(2L, scalar(db, "SELECT COUNT(*) FROM cleanup_events"))
                if (version < 18) {
                    assertEquals(2L, scalar(db, "SELECT COUNT(*) FROM cleanup_events_legacy_v$version"))
                    assertEquals(7L, scalar(db, "SELECT swipe_count FROM swipe_statistics WHERE date = '2024-01-01'"))
                    assertEquals(1024L, scalar(db, "SELECT size_delta_bytes FROM cleanup_events WHERE event_id = 'unknown'"))
                }
                if (version >= 20) {
                    assertEquals(42L, scalar(db, "SELECT last_generation FROM media_sync_state WHERE sync_key = 'external'"))
                    assertEquals(42L, scalar(db, "SELECT checkpoint_generation FROM media_sync_state WHERE sync_key = 'external'"))
                    db.query("SELECT checkpoint_state FROM media_sync_state WHERE sync_key = 'external'").use { cursor ->
                        assertTrue(cursor.moveToFirst())
                        assertEquals(if (version == 20) "running" else "complete", cursor.getString(0))
                    }
                }
                assertEquals(ProcessingStatus.FAVORITED, migrated.photoDao().byId(11)?.processingStatus)
                assertEquals(DeletionStatus.PENDING, migrated.photoDao().byId(12)?.deletionStatus)
                assertEquals(ProcessingStatus.FAVORITED, migrated.videoDao().byId(21)?.processingStatus)
                assertEquals(DeletionStatus.PENDING, migrated.videoDao().byId(22)?.deletionStatus)
                val totals = migrated.statsDao().aggregateEvents(0, Long.MAX_VALUE)
                assertEquals(1L, totals.photoDelta)
                assertEquals(1L, totals.videoDelta)
                assertEquals(2L, totals.actionCount)
                assertEquals(2L, totals.cleanedMediaCount)
                assertEquals(listOf("2024-01-01"), migrated.statsDao().qualifyingCleanupDates())
                if (version >= 14) assertNotNull(migrated.operationDao().undoableById(31))
                if (version in 14..17) {
                    assertEquals(1L, totals.favoriteDelta)
                    val originalEvent = migrated.statsDao().eventBySourceRef("operation:31")
                    assertEquals("favorited", originalEvent?.eventType)
                    assertEquals(31L, originalEvent?.operationId)
                }
            } finally {
                migrated.close()
            }
        }
    }

    @Test
    fun duplicateLegacyMediaIdsFailWithoutDestroyingTheOriginalDatabase() {
        val name = "migration-duplicate.db".also(databaseNames::add)
        createLegacyDatabase(name, 13).use { db ->
            seedDatabase(db, 13)
            insertRow(db, "photos", mapOf("id" to 13L, "media_store_id" to 101L))
        }
        val candidate = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabaseMigrations.ALL).allowMainThreadQueries().build()
        val failure = runCatching { candidate.openHelper.writableDatabase }.exceptionOrNull()
        candidate.close()
        assertNotNull("Ambiguous duplicate IDs must not be discarded", failure)
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(13, db.version)
            db.rawQuery("SELECT COUNT(*) FROM photos", null).use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals(3L, rows.getLong(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM cleanup_events", null).use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals(2L, rows.getLong(0))
            }
        }
    }

    @Test
    fun unsupportedSchemaDoesNotFallBackToDeletingData() {
        val name = "migration-unsupported.db".also(databaseNames::add)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE saved_data (value TEXT NOT NULL)")
            db.execSQL("INSERT INTO saved_data VALUES ('preserve')")
            db.version = 12
        }
        val candidate = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabaseMigrations.ALL).allowMainThreadQueries().build()
        val failure = runCatching { candidate.openHelper.writableDatabase }.exceptionOrNull()
        candidate.close()
        assertNotNull("An unsupported upgrade must fail rather than erase the database", failure)
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(12, db.version)
            db.rawQuery("SELECT value FROM saved_data", null).use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals("preserve", rows.getString(0))
            }
        }
    }

    private fun seedDatabase(db: SQLiteDatabase, version: Int) {
        insertRow(db, "photos", mapOf("id" to 11L, "media_store_id" to 101L,
            "uri" to "content://media/external/images/media/101", "processing_status" to "favorited", "deletion_status" to "none"))
        insertRow(db, "photos", mapOf("id" to 12L, "media_store_id" to 102L,
            "uri" to "content://media/external/images/media/102", "processing_status" to "unprocessed", "deletion_status" to "pending"))
        insertRow(db, "videos", mapOf("id" to 21L, "media_store_id" to 201L,
            "uri" to "content://media/external/video/media/201", "processing_status" to "favorited", "deletion_status" to "none"))
        insertRow(db, "videos", mapOf("id" to 22L, "media_store_id" to 202L,
            "uri" to "content://media/external/video/media/202", "processing_status" to "unprocessed", "deletion_status" to "pending"))
        insertRow(db, "trash_items", mapOf("id" to 41L, "media_id" to 12L, "media_type" to "photo", "media_store_id" to 102L))
        insertRow(db, "trash_items", mapOf("id" to 42L, "media_id" to 22L, "media_type" to "video", "media_store_id" to 202L))
        insertRow(db, "user_stats", mapOf("id" to 1L, "total_photos_cleared" to 7L))
        insertRow(db, "achievement_progress", mapOf("achievement_id" to "first", "current_value" to 3L, "unlocked" to 1L))
        if (version >= 14) insertRow(db, "operation_history", mapOf(
            "id" to 31L, "media_id" to 11L, "media_type" to "photo", "action" to "Favorite",
            "previous_processing_status" to "unprocessed", "new_processing_status" to "favorited", "is_undone" to 0L))
        if (version < 18) {
            insertRow(db, "cleanup_events", mapOf("event_id" to "favorite", "event_type" to "clean_action", "photo_count" to 1L,
                "source_ref" to "operation:31", "local_date" to "2024-01-01", "occurred_at" to 1L))
            insertRow(db, "cleanup_events", mapOf("event_id" to "unknown", "event_type" to "clean_action", "video_count" to 1L,
                "freed_bytes" to 1024L, "source_ref" to "legacy", "local_date" to "2024-01-01", "occurred_at" to 2L))
            insertRow(db, "swipe_statistics", mapOf("date" to "2024-01-01", "swipe_count" to 7L))
        } else {
            insertRow(db, "cleanup_events", mapOf("event_id" to "favorite", "event_type" to "favorited", "photo_delta" to 1L,
                "favorite_delta" to 1L, "affected_media_count" to 1L, "action_count" to 1L, "local_date" to "2024-01-01", "occurred_at" to 1L))
            insertRow(db, "cleanup_events", mapOf("event_id" to "unknown", "event_type" to "kept", "video_delta" to 1L,
                "affected_media_count" to 1L, "action_count" to 1L, "local_date" to "2024-01-01", "occurred_at" to 2L))
        }
        if (version >= 20) insertRow(db, "media_sync_state", mapOf("sync_key" to "external", "last_generation" to 42L,
            "checkpoint_generation" to 42L, "checkpoint_state" to "complete"))
    }

    private fun insertRow(db: SQLiteDatabase, table: String, overrides: Map<String, Any>) {
        val values = ContentValues()
        val columns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                columns += name
                if (cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1) {
                    when (type.uppercase()) {
                        "TEXT" -> values.put(name, "")
                        "REAL" -> values.put(name, 0.0)
                        "BLOB" -> values.put(name, byteArrayOf())
                        else -> values.put(name, 0L)
                    }
                }
            }
        }
        overrides.filterKeys { it in columns }.forEach { (name, value) ->
            when (value) {
                is String -> values.put(name, value)
                is Long -> values.put(name, value)
                is Int -> values.put(name, value)
                else -> error("Unsupported fixture value for " + name)
            }
        }
        db.insertOrThrow(table, null, values)
    }

    private fun createLegacyDatabase(name: String, version: Int): SQLiteDatabase {
        val asset = "com.futureape.kanleme.data.local.AppDatabase/" + version + ".json"
        val schema = InstrumentationRegistry.getInstrumentation().context.assets.open(asset)
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(path, null)
        db.beginTransaction()
        try {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices")
                if (indexes != null) for (j in 0 until indexes.length()) {
                    db.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = version
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return db
    }

    private fun scalar(db: SupportSQLiteDatabase, query: String): Long = db.query(query).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0)
    }
}
