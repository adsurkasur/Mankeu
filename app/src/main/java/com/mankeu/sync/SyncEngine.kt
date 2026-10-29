package com.mankeu.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mankeu.backup.BackupTable
import com.mankeu.backup.BackupValue
import com.mankeu.backup.PortableBackup
import com.mankeu.data.AppDatabase

/**
 * The local half of multi-device sync (DEC-013): turns the trigger-filled outbox into a change set,
 * and applies other devices' change sets. No network here; see [SyncCoordinator] and [SyncTransport].
 *
 * A change set is a [PortableBackup] so it reuses the backup codec and [com.mankeu.backup.PasswordBackupCipher]
 * unchanged: one [BackupTable] per data table, whose first two columns are [OP] and [CHANGED_AT],
 * plus a [HEADER] table with the device id, its push counter and whether it is a full snapshot.
 *
 * Conflicts are row-level last-writer-wins on `changedAt`: a remote change is skipped when this
 * device still has an unpushed change to the same row that is newer. Ledger rows travel with their
 * journals and postings in the same change set, so an applied set keeps every journal balanced.
 *
 * Two devices that create the same category, stock item or product offline (same natural key,
 * different ids) are merged: the incoming row is not inserted, an alias remote id -> local id is
 * kept in `sync_meta`, and every reference to it (`categoryId`, `productId`, `stockItemId`), now and
 * in later change sets, is rewritten to the local row. A merged product's sale units and recipe lines
 * are merged the same way by name. Counted in [ApplyResult.merged]. A row a
 * constraint still refuses is skipped and counted in [ApplyResult.rejected].
 */
class SyncEngine(private val database: AppDatabase) {
    data class LocalChanges(val changeSet: PortableBackup, val pushNumber: Long, val upToSeq: Long, val full: Boolean)
    data class Header(val deviceId: String, val pushNumber: Long, val full: Boolean)
    data class ApplyResult(val applied: Int, val skippedOlder: Int, val rejected: Int, val merged: Int = 0)

    private val dao get() = database.syncDao()

    suspend fun isEnabled(): Boolean = dao.meta(SyncSchema.SYNC_ENABLED) != null

    /** Starts change capture; the first push after this carries every row (a full snapshot). */
    suspend fun enable() = database.withTransaction {
        dao.setMeta(SyncSchema.SYNC_ENABLED, "1")
        dao.setMeta(SyncSchema.REQUIRES_FULL_SNAPSHOT, "1")
    }

    /** Stops change capture and drops what was waiting; turning sync on again sends everything. */
    suspend fun disable() = database.withTransaction {
        dao.clearMeta(SyncSchema.SYNC_ENABLED)
        dao.acknowledge(Long.MAX_VALUE)
    }

    suspend fun deviceId(): String = database.withTransaction {
        dao.meta(SyncSchema.DEVICE_ID) ?: run {
            SyncSchema.ensureDeviceId(database.openHelper.writableDatabase)
            requireNotNull(dao.meta(SyncSchema.DEVICE_ID))
        }
    }

    /** Pending local changes as one change set, or null when there is nothing to push. */
    suspend fun collectLocalChanges(): LocalChanges? = database.withTransaction {
        val db = database.openHelper.writableDatabase
        val full = dao.meta(SyncSchema.REQUIRES_FULL_SNAPSHOT) != null
        val pending = dao.pending()
        if (!full && pending.isEmpty()) return@withTransaction null
        val now = System.currentTimeMillis()
        val latest = pending.groupBy { it.tableName to it.rowId }.mapValues { (_, entries) -> entries.last() }
        val tables = AppDatabase.DATA_TABLES.mapNotNull { table ->
            val columns = columnsOf(db, table)
            val rows = if (full) {
                db.query("SELECT * FROM `$table`").use { cursor -> buildList { while (cursor.moveToNext()) add(upsertRow(cursor, now)) } }
            } else {
                latest.filterKeys { it.first == table }.values.map { change ->
                    val live = db.query("SELECT * FROM `$table` WHERE id = ?", arrayOf(change.rowId)).use { c -> if (c.moveToFirst()) upsertRow(c, change.changedAtEpochMillis) else null }
                    live ?: deleteRow(change.rowId, change.changedAtEpochMillis, columns)
                }
            }
            rows.takeIf { it.isNotEmpty() }?.let { BackupTable(table, listOf(OP, CHANGED_AT) + columns, it) }
        }
        val pushNumber = (dao.meta(PUSH_COUNTER)?.toLongOrNull() ?: 0) + 1
        val header = BackupTable(
            HEADER,
            listOf("deviceId", "pushNumber", "full"),
            listOf(listOf(BackupValue.Text(deviceId()), BackupValue.Integer(pushNumber), BackupValue.Integer(if (full) 1 else 0))),
        )
        LocalChanges(
            changeSet = PortableBackup(schemaVersion(db), now, listOf(header) + tables),
            pushNumber = pushNumber,
            upToSeq = pending.maxOfOrNull { it.seq } ?: 0,
            full = full,
        )
    }

    /** Takes [changes]' push number before uploading, so a failed upload never reuses it. */
    suspend fun reserve(changes: LocalChanges) = dao.setMeta(PUSH_COUNTER, changes.pushNumber.toString())

    /** Call once [changes] is safely stored remotely. */
    suspend fun acknowledge(changes: LocalChanges) = database.withTransaction {
        dao.acknowledge(changes.upToSeq)
        if (changes.full) dao.clearMeta(SyncSchema.REQUIRES_FULL_SNAPSHOT)
    }

    fun header(changeSet: PortableBackup): Header {
        val row = changeSet.tables.single { it.name == HEADER }.rows.single()
        return Header(
            deviceId = (row[0] as BackupValue.Text).value,
            pushNumber = (row[1] as BackupValue.Integer).value,
            full = (row[2] as BackupValue.Integer).value == 1L,
        )
    }

    /** Applies another device's change set in one transaction, with change capture off. */
    suspend fun applyRemote(changeSet: PortableBackup): ApplyResult = database.withTransaction {
        val db = database.openHelper.writableDatabase
        require(changeSet.schemaVersion <= schemaVersion(db)) {
            "Perubahan dari perangkat lain memakai versi data lebih baru. Perbarui aplikasi dulu."
        }
        val localPending = dao.pending().groupBy { it.tableName to it.rowId }.mapValues { (_, e) -> e.maxOf { it.changedAtEpochMillis } }
        val byTable = changeSet.tables.filter { it.name in AppDatabase.DATA_TABLES }.associateBy { it.name }
        var applied = 0
        var older = 0
        var rejected = 0
        var merged = 0
        fun alias(table: String, id: String): String? =
            db.query("SELECT value FROM sync_meta WHERE `key` = ?", arrayOf<Any>("$ALIAS$table:$id")).use { if (it.moveToFirst()) it.getString(0) else null }
        fun resolve(values: Map<String, BackupValue>): Map<String, BackupValue> = values.mapValues { (column, value) ->
            val target = REFERENCES[column]
            if (target != null && value is BackupValue.Text) alias(target, value.value)?.let { BackupValue.Text(it) } ?: value else value
        }
        SyncSchema.quietly(db) {
            fun eachChange(tables: List<String>, op: String, apply: (String, String, Map<String, BackupValue>) -> Unit) {
                tables.forEach { table ->
                    val data = byTable[table] ?: return@forEach
                    val live = columnsOf(db, table).toSet()
                    data.rows.forEach { row ->
                        val values = data.columns.zip(row).toMap()
                        if ((values[OP] as BackupValue.Text).value != op) return@forEach
                        val remoteId = (values.getValue("id") as BackupValue.Text).value
                        val id = alias(table, remoteId) ?: remoteId
                        val changedAt = (values.getValue(CHANGED_AT) as BackupValue.Integer).value
                        if ((localPending[table to id] ?: Long.MIN_VALUE) > changedAt) {
                            older++
                            return@forEach
                        }
                        try {
                            apply(table, id, resolve(values.filterKeys { it in live }) + ("id" to BackupValue.Text(id)))
                            applied++
                        } catch (_: SQLiteConstraintException) {
                            rejected++
                        }
                    }
                }
            }
            // A product's units and recipe lines are deleted first: the editor replaces them, and a
            // replacement must not be merged into the very row it replaces (cleanup P4). Nothing refers to them.
            eachChange(PRODUCT_CHILDREN, DELETE) { table, id, _ -> db.delete(table, "id = ?", arrayOf(id)) }
            // Parents before children on the way in, children before parents on the way out.
            eachChange(AppDatabase.DATA_TABLES, UPSERT) { table, id, values ->
                val content = ContentValues(values.size).apply { values.forEach { (column, value) -> put(column, value) } }
                // UPDATE then INSERT, never INSERT OR REPLACE: REPLACE deletes first and would
                // cascade away the row's journals and postings.
                if (db.update(table, SQLiteDatabase.CONFLICT_ABORT, content, "id = ?", arrayOf(id)) == 0) {
                    val twin = NATURAL_KEYS[table]?.takeIf { table !in PRODUCT_CHILDREN || isMergedProduct(values) }?.let { keys -> twinOf(db, table, keys, values) }
                    if (twin != null) {
                        db.execSQL("INSERT OR REPLACE INTO sync_meta(`key`, value) VALUES (?, ?)", arrayOf<Any>("$ALIAS$table:$id", twin))
                        merged++
                    } else {
                        db.insert(table, SQLiteDatabase.CONFLICT_ABORT, content)
                    }
                }
            }
            eachChange(AppDatabase.DATA_TABLES.asReversed() - PRODUCT_CHILDREN.toSet(), DELETE) { table, id, _ ->
                db.delete(table, "id = ?", arrayOf(id))
            }
        }
        ApplyResult(applied - merged, older, rejected, merged)
    }

    /**
     * Whether the (already alias-resolved) product of an incoming unit or recipe line is a local product
     * that absorbed another phone's twin. Only then are its children matched by name: within one product
     * a name match would merge two lines that merely share a name ("Lain-lain").
     */
    private fun isMergedProduct(values: Map<String, BackupValue>): Boolean {
        val product = (values["productId"] as? BackupValue.Text)?.value ?: return false
        return db().query("SELECT 1 FROM sync_meta WHERE `key` LIKE ? AND value = ? LIMIT 1", arrayOf<Any>("${ALIAS}products:%", product)).use { it.moveToFirst() }
    }

    private fun db() = database.openHelper.writableDatabase

    /** The local row with the same natural key as an incoming one, e.g. a category of that name. */
    private fun twinOf(db: SupportSQLiteDatabase, table: String, keys: List<String>, values: Map<String, BackupValue>): String? {
        if (keys.any { it !in values }) return null
        val where = keys.joinToString(" AND ") { if (values[it] == BackupValue.Null) "`$it` IS NULL" else "`$it` = ? COLLATE NOCASE" }
        val args = keys.mapNotNull { key -> when (val v = values.getValue(key)) {
            BackupValue.Null -> null
            is BackupValue.Text -> v.value
            is BackupValue.Integer -> v.value
            is BackupValue.Real -> v.value
            is BackupValue.Blob -> v.value
        } }
        return db.query("SELECT id FROM `$table` WHERE $where LIMIT 1", args.toTypedArray()).use { if (it.moveToFirst()) it.getString(0) else null }
    }

    suspend fun remoteCursor(deviceId: String): Long = dao.meta("$CURSOR$deviceId")?.toLongOrNull() ?: 0

    suspend fun setRemoteCursor(deviceId: String, pushNumber: Long) = dao.setMeta("$CURSOR$deviceId", pushNumber.toString())

    private fun columnsOf(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info(`$table`)").use { c -> buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) } }

    private fun schemaVersion(db: SupportSQLiteDatabase): Int = db.query("PRAGMA user_version").use { it.moveToFirst(); it.getInt(0) }

    private fun upsertRow(cursor: Cursor, changedAt: Long): List<BackupValue> =
        listOf(BackupValue.Text(UPSERT), BackupValue.Integer(changedAt)) + (0 until cursor.columnCount).map { index ->
            when (cursor.getType(index)) {
                Cursor.FIELD_TYPE_NULL -> BackupValue.Null
                Cursor.FIELD_TYPE_INTEGER -> BackupValue.Integer(cursor.getLong(index))
                Cursor.FIELD_TYPE_FLOAT -> BackupValue.Real(cursor.getDouble(index))
                Cursor.FIELD_TYPE_BLOB -> BackupValue.Blob(cursor.getBlob(index))
                else -> BackupValue.Text(cursor.getString(index))
            }
        }

    private fun deleteRow(id: String, changedAt: Long, columns: List<String>): List<BackupValue> =
        listOf(BackupValue.Text(DELETE), BackupValue.Integer(changedAt)) +
            columns.map { if (it == "id") BackupValue.Text(id) else BackupValue.Null }

    private fun ContentValues.put(column: String, value: BackupValue) = when (value) {
        BackupValue.Null -> putNull(column)
        is BackupValue.Integer -> put(column, value.value)
        is BackupValue.Real -> put(column, value.value)
        is BackupValue.Text -> put(column, value.value)
        is BackupValue.Blob -> put(column, value.value)
    }

    companion object {
        const val OP = "_op"
        const val CHANGED_AT = "_changedAt"
        const val HEADER = "_sync"
        const val UPSERT = "UPSERT"
        const val DELETE = "DELETE"
        private const val PUSH_COUNTER = "push_counter"
        private const val CURSOR = "cursor:"
        private const val ALIAS = "alias:"

        /** What makes two rows "the same thing" for the owner, per table that can be created offline twice. */
        private val NATURAL_KEYS = mapOf(
            "categories" to listOf("businessId", "role", "normalizedName"),
            "contacts" to listOf("businessId", "normalizedName"),
            "stock_items" to listOf("businessId", "projectId", "name"),
            "products" to listOf("projectId", "name"),
            // Only for a merged twin product, see isMergedProduct (cleanup P4).
            "product_units" to listOf("productId", "name"),
            "product_recipe_lines" to listOf("productId", "name"),
        )
        private val PRODUCT_CHILDREN = listOf("product_units", "product_recipe_lines")

        /** Reference columns and the table they point into, rewritten through aliases. */
        private val REFERENCES = mapOf(
            "categoryId" to "categories",
            "productId" to "products",
            "stockItemId" to "stock_items",
            "contactId" to "contacts",
            "saleId" to "sales",
        )
    }
}
