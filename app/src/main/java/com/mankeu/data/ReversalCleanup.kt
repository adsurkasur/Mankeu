package com.mankeu.data

import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

/**
 * Removes what the retired "Balik Transaksi" flow left behind (DEC-019): every reversal row and the
 * original it undid, with their journals, postings and import provenance. Each pair nets to zero,
 * so cash and every report stay the same.
 *
 * Shared by `MIGRATION_10_11` and by restoring a backup taken on schema 10 or older (whose reversal
 * columns no longer exist, so the caller finds the ids in the snapshot). A notice says what went.
 */
object ReversalCleanup {
    /** Migration: the columns still exist, so the pairs are found in the database itself. */
    fun run(db: SupportSQLiteDatabase, source: String) {
        val ids = db.query("SELECT id FROM financial_events WHERE type = 'REVERSAL' OR reversedByEventId IS NOT NULL").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        delete(db, ids, source)
    }

    fun delete(db: SupportSQLiteDatabase, ids: Collection<String>, source: String) {
        if (ids.isEmpty()) return
        db.execSQL("DROP TABLE IF EXISTS doomed_events")
        db.execSQL("CREATE TEMP TABLE doomed_events (id TEXT PRIMARY KEY)")
        ids.forEach { db.execSQL("INSERT OR IGNORE INTO doomed_events(id) VALUES (?)", arrayOf<Any>(it)) }
        db.execSQL("DELETE FROM postings WHERE journalId IN (SELECT id FROM journal_entries WHERE sourceEventId IN (SELECT id FROM doomed_events))")
        db.execSQL("DELETE FROM journal_entries WHERE sourceEventId IN (SELECT id FROM doomed_events)")
        db.execSQL("DELETE FROM provenance_records WHERE targetEventId IN (SELECT id FROM doomed_events)")
        db.execSQL("DELETE FROM financial_events WHERE id IN (SELECT id FROM doomed_events)")
        db.execSQL("DROP TABLE doomed_events")
        db.execSQL(
            "INSERT INTO import_notices(id, batchId, severity, title, explanation, amountRupiah, sourceEvidence, reviewed, action, createdAtEpochMillis) " +
                "VALUES (?, NULL, 'SYSTEM', ?, ?, NULL, ?, 0, NULL, ?)",
            arrayOf<Any>(
                UUID.randomUUID().toString(),
                "${ids.size} catatan yang dibalik dihapus",
                "Fitur Balik Transaksi diganti Ubah dan Hapus. Catatan yang pernah dibalik beserta pembaliknya " +
                    "sudah dihapus. Saldo kas dan laporan tidak berubah karena setiap pasangan saling meniadakan.",
                source,
                System.currentTimeMillis(),
            ),
        )
    }
}
