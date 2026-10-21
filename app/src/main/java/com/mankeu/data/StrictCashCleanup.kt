package com.mankeu.data

import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

/**
 * Removes what strict cash (DEC-011 K-1) no longer has a meaning for: credit sales and purchases,
 * receivable/payable settlements, loans and interest, with their journals, postings and import
 * provenance. Reversals of those rows are removed by [ReversalCleanup] (schema 11).
 *
 * Shared by `MIGRATION_7_8` and by restoring a backup taken on schema 7 or older, so an old backup
 * cannot bring the concepts back. Nothing disappears silently: a summary per type is written as a
 * persistent notice. Categories a user still uses are left alone; only unused loan/interest
 * defaults are archived.
 */
object StrictCashCleanup {
    private val LEGACY_TYPES = linkedMapOf(
        "CREDIT_SALE" to "Penjualan kredit",
        "CREDIT_PURCHASE_EXPENSE" to "Pembelian kredit (biaya)",
        "CREDIT_PURCHASE_ASSET" to "Pembelian kredit (alat)",
        "RECEIVABLE_COLLECTION" to "Penerimaan piutang",
        "PAYABLE_SETTLEMENT" to "Pembayaran utang",
        "LOAN_RECEIVED" to "Pinjaman diterima",
        "LOAN_PRINCIPAL_REPAID" to "Cicilan pokok pinjaman",
        "LOAN_INTEREST_PAID" to "Bunga pinjaman",
    )
    private val TYPE_LIST = LEGACY_TYPES.keys.joinToString(",") { "'$it'" }

    /** @param source shown in the notice, e.g. "pembaruan basis data" or "pemulihan cadangan". */
    fun run(db: SupportSQLiteDatabase, source: String) {
        val summary = buildList {
            db.query(
                "SELECT type, COUNT(*), SUM(amountRupiah), MIN(dateEpochDay), MAX(dateEpochDay) " +
                    "FROM financial_events WHERE type IN ($TYPE_LIST) GROUP BY type",
            ).use { c ->
                while (c.moveToNext()) add(Row(c.getString(0), c.getInt(1), c.getLong(2), c.getLong(3), c.getLong(4)))
            }
        }

        db.execSQL("DROP TABLE IF EXISTS doomed_events")
        db.execSQL(
            "CREATE TEMP TABLE doomed_events AS " +
                "SELECT id FROM financial_events WHERE type IN ($TYPE_LIST)",
        )
        db.execSQL("DELETE FROM postings WHERE journalId IN (SELECT id FROM journal_entries WHERE sourceEventId IN (SELECT id FROM doomed_events))")
        db.execSQL("DELETE FROM journal_entries WHERE sourceEventId IN (SELECT id FROM doomed_events)")
        db.execSQL("DELETE FROM provenance_records WHERE targetEventId IN (SELECT id FROM doomed_events)")
        db.execSQL("DELETE FROM financial_events WHERE id IN (SELECT id FROM doomed_events)")
        db.execSQL("DROP TABLE doomed_events")

        // Category roles (DEC-011 K-2). EQUITY was only ever "owner capital".
        db.execSQL("UPDATE OR IGNORE categories SET role = 'CAPITAL_IN' WHERE role = 'EQUITY'")
        db.execSQL(
            "UPDATE categories SET active = 0 WHERE (role = 'LIABILITY' OR normalizedName = 'biaya bunga pinjaman') " +
                "AND NOT EXISTS (SELECT 1 FROM financial_events e WHERE e.categoryId = categories.id " +
                "OR (e.businessId = categories.businessId AND lower(e.category) = categories.normalizedName)) " +
                "AND NOT EXISTS (SELECT 1 FROM budget_items b WHERE b.categoryId = categories.id)",
        )
        // Still used by the owner: keep it, as an expense (CategoryRole.parse reads leftovers so too).
        db.execSQL("UPDATE OR IGNORE categories SET role = 'EXPENSE' WHERE role = 'LIABILITY'")

        if (summary.isEmpty()) return
        val total = summary.sumOf { it.total }
        val lines = summary.joinToString("\n") {
            "• ${LEGACY_TYPES.getValue(it.type)}: ${it.count} catatan, ${rupiah(it.total)} " +
                "(${LocalDate.ofEpochDay(it.firstDay)} s.d. ${LocalDate.ofEpochDay(it.lastDay)})"
        }
        db.execSQL(
            "INSERT INTO import_notices(id, batchId, severity, title, explanation, amountRupiah, sourceEvidence, reviewed, action, createdAtEpochMillis) " +
                "VALUES (?, NULL, 'SYSTEM', ?, ?, ?, ?, 0, NULL, ?)",
            arrayOf<Any>(
                UUID.randomUUID().toString(),
                "${summary.sumOf { it.count }} catatan non-kas lama dihapus",
                "Mankeu kini murni kas: utang, piutang, pinjaman, cicilan, dan bunga tidak lagi dicatat. " +
                    "Catatan berikut sudah dihapus:\n$lines",
                total,
                source,
                System.currentTimeMillis(),
            ),
        )
    }

    private data class Row(val type: String, val count: Int, val total: Long, val firstDay: Long, val lastDay: Long)

    private fun rupiah(value: Long) = "Rp" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(value)
}
