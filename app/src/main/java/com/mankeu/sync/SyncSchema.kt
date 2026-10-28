package com.mankeu.sync

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mankeu.data.AppDatabase
import java.util.UUID

/**
 * The database half of multi-device sync (DEC-013): change capture by SQLite triggers.
 *
 * Every data table gets three triggers that append to `sync_outbox`. Doing it in SQL instead of in
 * the repository means no write path can forget to record itself — imports, restores and future
 * code included. The triggers record only while [SYNC_ENABLED] is set (schema 12: before that the
 * outbox grew by about four rows per transaction on every device, synced or not), and are silent
 * while [APPLYING_REMOTE] is set, so applying another device's changes never echoes back.
 *
 * Room does not manage triggers. They are created by [callback] on fresh installs and after the
 * destructive fallback from schema 1-11 (DEC-026), and **any later migration that rebuilds a data
 * table (create-copy-drop-rename) drops that table's triggers and must call [createTriggers] again.**
 * So must a migration that adds a data table: an older migration runs against the schema of its
 * own time, so [createTriggers] covers only the [AppDatabase.DATA_TABLES] that exist yet.
 */
object SyncSchema {
    const val APPLYING_REMOTE = "applying_remote"
    /** Present while the owner has sync switched on; turning sync on also asks for a full snapshot. */
    const val SYNC_ENABLED = "sync_enabled"
    const val DEVICE_ID = "device_id"
    /** Set after a restore replaced the whole database; the next sync must push a full snapshot. */
    const val REQUIRES_FULL_SNAPSHOT = "requires_full_snapshot"

    fun createTriggers(db: SupportSQLiteDatabase) {
        val quiet = "WHEN EXISTS (SELECT 1 FROM sync_meta WHERE `key` = '$SYNC_ENABLED') " +
            "AND NOT EXISTS (SELECT 1 FROM sync_meta WHERE `key` = '$APPLYING_REMOTE')"
        val now = "CAST(strftime('%s','now') AS INTEGER) * 1000"
        val existing = db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }
        AppDatabase.DATA_TABLES.filter { it in existing }.forEach { table ->
            db.execSQL("DROP TRIGGER IF EXISTS `sync_${table}_ins`")
            db.execSQL("DROP TRIGGER IF EXISTS `sync_${table}_upd`")
            db.execSQL("DROP TRIGGER IF EXISTS `sync_${table}_del`")
            db.execSQL(
                "CREATE TRIGGER `sync_${table}_ins` AFTER INSERT ON `$table` $quiet BEGIN " +
                    "INSERT INTO sync_outbox(tableName, rowId, op, changedAtEpochMillis) VALUES ('$table', NEW.id, 'UPSERT', $now); END",
            )
            db.execSQL(
                "CREATE TRIGGER `sync_${table}_upd` AFTER UPDATE ON `$table` $quiet BEGIN " +
                    "INSERT INTO sync_outbox(tableName, rowId, op, changedAtEpochMillis) VALUES ('$table', NEW.id, 'UPSERT', $now); END",
            )
            db.execSQL(
                "CREATE TRIGGER `sync_${table}_del` AFTER DELETE ON `$table` $quiet BEGIN " +
                    "INSERT INTO sync_outbox(tableName, rowId, op, changedAtEpochMillis) VALUES ('$table', OLD.id, 'DELETE', $now); END",
            )
        }
    }

    fun ensureDeviceId(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT OR IGNORE INTO sync_meta(`key`, value) VALUES (?, ?)",
            arrayOf(DEVICE_ID, UUID.randomUUID().toString()),
        )
    }

    private fun setUp(db: SupportSQLiteDatabase) {
        createTriggers(db)
        ensureDeviceId(db)
    }

    /**
     * Fresh installs skip migrations, so [onCreate] sets up the triggers and device id. Room's
     * destructive fallback does not call onCreate, and calls [onDestructiveMigration] while the
     * tables are dropped and not yet recreated, so that case is finished in the [onOpen] that follows.
     */
    val callback = object : RoomDatabase.Callback() {
        @Volatile private var setUpOnOpen = false

        override fun onCreate(db: SupportSQLiteDatabase) = setUp(db)
        override fun onDestructiveMigration(db: SupportSQLiteDatabase) { setUpOnOpen = true }
        override fun onOpen(db: SupportSQLiteDatabase) {
            if (setUpOnOpen) {
                setUpOnOpen = false
                setUp(db)
            }
        }
    }

    /** Runs [block] with change capture off: for applying remote changes and for restore. */
    inline fun <T> quietly(db: SupportSQLiteDatabase, block: () -> T): T {
        db.execSQL("INSERT OR REPLACE INTO sync_meta(`key`, value) VALUES ('$APPLYING_REMOTE', '1')")
        try {
            return block()
        } finally {
            db.execSQL("DELETE FROM sync_meta WHERE `key` = '$APPLYING_REMOTE'")
        }
    }
}
