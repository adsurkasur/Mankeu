package com.mankeu.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Schema migrations against real old databases.
 *
 * Schema 7 → 8 on a database that holds both cash and legacy non-cash rows: cash survives intact,
 * every credit/loan row goes with its journal and postings, a notice says so, category
 * roles are remapped, and sync change capture is live afterwards (DEC-011, DEC-013).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate7To8PurgesNonCashKeepsCashAndStartsChangeCapture() {
        helper.createDatabase(DB, 7).apply {
            execSQL("INSERT INTO businesses VALUES ('b', 'Warung', 'IDR', 0, 0, 0)")
            execSQL("INSERT INTO projects(id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Utama', 0, 0, 0, '', 0)")
            execSQL("INSERT INTO books(id, projectId, mode, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('k', 'p', 'REALIZATION', 0, 0)")
            category("c-modal", "EQUITY", "Modal Sendiri", "modal sendiri")
            category("c-pinjam", "LIABILITY", "Pinjaman Usaha", "pinjaman usaha")
            category("c-bunga", "EXPENSE", "Biaya Bunga Pinjaman", "biaya bunga pinjaman")
            event("cash", "CASH_INCOME", "OPERATING", 1_000_000, posts = listOf("CASH" to "DEBIT", "REVENUE" to "CREDIT"))
            event("credit", "CREDIT_SALE", "NON_CASH", 500_000, posts = listOf("ACCOUNTS_RECEIVABLE" to "DEBIT", "REVENUE" to "CREDIT"))
            event("credit-rev", "REVERSAL", "NON_CASH", 500_000, reverses = "credit", posts = listOf("ACCOUNTS_RECEIVABLE" to "CREDIT", "REVENUE" to "DEBIT"))
            event("loan", "LOAN_RECEIVED", "FINANCING", 2_000_000, posts = listOf("CASH" to "DEBIT", "LOAN_PAYABLE" to "CREDIT"))
            execSQL("INSERT INTO scenario_assumptions(id, bookId, openingCashRupiah, ownerCapitalRupiah, otherFinancingCostRupiah, updatedAtEpochMillis) VALUES ('a', 'k', 0, 0, 400000, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 8, true, AppDatabase.MIGRATION_7_8)

        // The reversal of the credit sale is left for schema 11, which deletes every reversal (DEC-019).
        assertEquals(listOf("cash", "credit-rev"), db.strings("SELECT id FROM financial_events ORDER BY id"))
        assertEquals(listOf("journal-cash", "journal-cash"), db.strings("SELECT journalId FROM postings WHERE journalId = 'journal-cash' ORDER BY id"))
        assertEquals(0, db.count("SELECT COUNT(*) FROM postings WHERE journalId = 'journal-credit'"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM import_notices WHERE batchId IS NULL AND severity = 'SYSTEM'"))
        assertEquals(listOf("CAPITAL_IN"), db.strings("SELECT role FROM categories WHERE id = 'c-modal'"))
        assertEquals(listOf("0"), db.strings("SELECT active FROM categories WHERE id IN ('c-pinjam')"))
        assertEquals(listOf("0"), db.strings("SELECT active FROM categories WHERE id IN ('c-bunga')"))
        assertNotNull(db.strings("SELECT value FROM sync_meta WHERE `key` = 'device_id'").singleOrNull())
        assertEquals(listOf("1"), db.strings("SELECT value FROM sync_meta WHERE `key` = 'requires_full_snapshot'"))

        // Change capture is live once sync is switched on (schema 12): a write lands in the outbox.
        db.execSQL("INSERT INTO sync_meta(`key`, value) VALUES ('sync_enabled', '1')")
        db.execSQL("UPDATE businesses SET name = 'Warung Baru' WHERE id = 'b'")
        assertEquals(listOf("businesses|b|UPSERT"), db.strings("SELECT tableName || '|' || rowId || '|' || op FROM sync_outbox"))
        assertTrue(db.strings("PRAGMA foreign_key_check").isEmpty())
    }

    /**
     * Schema 8 → 9 (DEC-014): the old four-number calculator of a Proyeksi book becomes a product
     * with a recipe line, a monthly fixed plan line and a capital line; opening cash is only named
     * in the notice; basic expense categories get their default cost nature; the new rows are in
     * the sync outbox.
     */
    @Test
    fun migrate8To9TurnsTheOldCalculatorIntoABusinessPlan() {
        helper.createDatabase(DB, 8).apply {
            execSQL("INSERT INTO businesses VALUES ('b', 'Kedai', 'IDR', 0, 0, 0)")
            execSQL("INSERT INTO projects(id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Utama', 0, 0, 0, '', 0)")
            execSQL("INSERT INTO books(id, projectId, mode, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('plan', 'p', 'PROJECTION', 0, 0)")
            category("c-bahan", "EXPENSE", "Bahan Baku & Stok", "bahan baku stok")
            category("c-gaji", "EXPENSE", "Gaji & Upah", "gaji upah")
            execSQL(
                "INSERT INTO scenario_assumptions(id, bookId, openingCashRupiah, ownerCapitalRupiah, outputQuantityText, outputUnit, outputPriceRupiah, fixedCostRupiah, variableCostPerUnitRupiah, updatedAtEpochMillis) " +
                    "VALUES ('a', 'plan', 1000000, 5000000, '700', 'cup', 10000, 2000000, 5000, 0)",
            )
            execSQL("DELETE FROM sync_outbox")
            // Converted rows must reach the outbox on a phone that syncs (capture needs sync on, schema 12).
            execSQL("INSERT INTO sync_meta(`key`, value) VALUES ('sync_enabled', '1')")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 9, true, AppDatabase.MIGRATION_8_9)

        assertEquals(listOf("Produk utama|cup|10000|700"), db.strings("SELECT name || '|' || unit || '|' || priceRupiah || '|' || targetPerMonthText FROM products"))
        assertEquals(listOf("5000|1"), db.strings("SELECT amountRupiah || '|' || yieldText FROM product_cost_components"))
        assertEquals(
            listOf("CAPITAL_IN|5000000|ONCE|", "EXPENSE|2000000|MONTHLY|FIXED"),
            db.strings(
                "SELECT c.role || '|' || i.plannedTotalRupiah || '|' || i.recurrence || '|' || COALESCE(i.costNature, '') " +
                    "FROM budget_items i JOIN categories c ON c.id = i.categoryId ORDER BY c.role",
            ),
        )
        assertEquals(listOf("VARIABLE", "FIXED"), db.strings("SELECT costNature FROM categories WHERE id IN ('c-bahan', 'c-gaji') ORDER BY id"))
        assertTrue(db.strings("SELECT explanation FROM import_notices WHERE severity = 'SYSTEM'").single().contains("Saldo kas awal Rp1.000.000"))
        assertEquals(0, db.count("SELECT COUNT(*) FROM sqlite_master WHERE name = 'scenario_assumptions'"))
        assertEquals(0, db.count("SELECT COUNT(*) FROM financial_events"))
        assertEquals(1, db.count("SELECT COUNT(*) FROM sync_outbox WHERE tableName = 'products'"))
        assertTrue(db.strings("PRAGMA foreign_key_check").isEmpty())
    }

    /**
     * Schema 9 → 10: products sell from month 1, existing stock stays shared by the business (which
     * branch holds it cannot be guessed), old batches have no money-in split, and one name may now
     * exist once per unit.
     */
    @Test
    fun migrate9To10KeepsDataAndLetsEachUnitHoldItsOwnStock() {
        helper.createDatabase(DB, 9).apply {
            execSQL("INSERT INTO businesses VALUES ('b', 'Toko', 'IDR', 0, 0, 0)")
            execSQL("INSERT INTO projects(id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Pusat', 0, 0, 0, '', 0)")
            execSQL("INSERT INTO products(id, projectId, name, unit, priceRupiah, targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('kopi', 'p', 'Kopi', 'cup', 10000, '700', 0, 0, 0, 0)")
            execSQL("INSERT INTO stock_items(id, businessId, name, unit, basePriceRupiah, initialStockText, currentStockText, minAlertStockText, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('s', 'b', 'Gula', 'kg', 15000, '5', '5', '1', 0, 0, 0)")
            execSQL("INSERT INTO import_batches(id, sourceName, sourceSha256, importerVersion, state, committedAtEpochMillis, projectId, recordCount, totalAmountRupiah) VALUES ('ib', 'lama.xlsx', 'sha', 'v', 'COMMITTED', 0, 'p', 3, 900000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 10, true, AppDatabase.MIGRATION_9_10)

        assertEquals(listOf("Kopi|1"), db.strings("SELECT name || '|' || sellFromMonth FROM products"))
        assertEquals(listOf("Gula|shared"), db.strings("SELECT name || '|' || COALESCE(projectId, 'shared') FROM stock_items"))
        assertEquals(listOf("900000|none"), db.strings("SELECT totalAmountRupiah || '|' || COALESCE(incomingRupiah, 'none') FROM import_batches"))
        db.execSQL("INSERT INTO projects(id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p2', 'b', 'Cabang', 0, 0, 0, '', 0)")
        db.execSQL("INSERT INTO stock_items(id, businessId, projectId, name, unit, basePriceRupiah, initialStockText, currentStockText, minAlertStockText, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('s1', 'b', 'p', 'Teh', 'kg', 1, '1', '1', '0', 0, 0, 0)")
        db.execSQL("INSERT INTO stock_items(id, businessId, projectId, name, unit, basePriceRupiah, initialStockText, currentStockText, minAlertStockText, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('s2', 'b', 'p2', 'Teh', 'kg', 1, '1', '1', '0', 0, 0, 0)")
        assertTrue(runCatching {
            db.execSQL("INSERT INTO stock_items(id, businessId, projectId, name, unit, basePriceRupiah, initialStockText, currentStockText, minAlertStockText, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('s3', 'b', 'p', 'Teh', 'kg', 1, '1', '1', '0', 0, 0, 0)")
        }.isFailure)
        assertTrue(db.strings("PRAGMA foreign_key_check").isEmpty())
    }

    /**
     * Schema 10 → 11 (DEC-019): each "Balik Transaksi" pair is deleted with its journals (it nets
     * to zero, so cash is unchanged), a notice says so, the reversal columns are gone, and change
     * capture still works on the rebuilt tables.
     */
    @Test
    fun migrate10To11DeletesReversalPairsAndTheirColumns() {
        helper.createDatabase(DB, 10).apply {
            execSQL("INSERT INTO businesses VALUES ('b', 'Kedai', 'IDR', 0, 0, 0)")
            execSQL("INSERT INTO projects(id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Utama', 0, 0, 0, '', 0)")
            execSQL("INSERT INTO books(id, projectId, mode, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('k', 'p', 'REALIZATION', 0, 0)")
            event("cash", "CASH_INCOME", "OPERATING", 1_000_000, posts = listOf("CASH" to "DEBIT", "REVENUE" to "CREDIT"))
            event("typo", "CASH_EXPENSE", "OPERATING", 50_000, posts = listOf("OPERATING_EXPENSE" to "DEBIT", "CASH" to "CREDIT"))
            event("typo-undo", "REVERSAL", "OPERATING", 50_000, reverses = "typo", posts = listOf("OPERATING_EXPENSE" to "CREDIT", "CASH" to "DEBIT"))
            execSQL("UPDATE financial_events SET status = 'REVERSED', reversedByEventId = 'typo-undo' WHERE id = 'typo'")
            execSQL("DELETE FROM sync_outbox")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 11, true, AppDatabase.MIGRATION_10_11)

        assertEquals(listOf("cash"), db.strings("SELECT id FROM financial_events"))
        assertEquals(listOf("journal-cash"), db.strings("SELECT id FROM journal_entries"))
        assertEquals(listOf("journal-cash", "journal-cash"), db.strings("SELECT journalId FROM postings ORDER BY id"))
        assertEquals(listOf("2 catatan yang dibalik dihapus"), db.strings("SELECT title FROM import_notices WHERE severity = 'SYSTEM'"))
        val columns = db.strings("SELECT name FROM pragma_table_info('financial_events')") + db.strings("SELECT name FROM pragma_table_info('journal_entries')")
        assertTrue(columns.none { it in setOf("status", "reversesEventId", "reversedByEventId", "reversalOfJournalId") })

        db.execSQL("DELETE FROM sync_outbox")
        db.execSQL("INSERT INTO sync_meta(`key`, value) VALUES ('sync_enabled', '1')")
        db.execSQL("UPDATE financial_events SET description = 'Jual' WHERE id = 'cash'")
        assertEquals(listOf("financial_events|cash|UPSERT"), db.strings("SELECT tableName || '|' || rowId || '|' || op FROM sync_outbox"))
        assertTrue(db.strings("PRAGMA foreign_key_check").isEmpty())
    }

    /**
     * Schema 11 -> 12 (DEC-024): the outbox nobody read is emptied, and with sync off a write is no
     * longer captured; once sync is on it is.
     */
    @Test
    fun migrate11To12CapturesOnlyWhileSyncIsOn() {
        helper.createDatabase(DB, 11).apply {
            execSQL("INSERT INTO businesses VALUES ('b', 'Kedai', 'IDR', 0, 0, 0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 12, true, AppDatabase.MIGRATION_11_12)

        assertEquals(0, db.count("SELECT COUNT(*) FROM sync_outbox"))
        db.execSQL("UPDATE businesses SET name = 'Tanpa sync' WHERE id = 'b'")
        assertEquals(0, db.count("SELECT COUNT(*) FROM sync_outbox"))
        db.execSQL("INSERT INTO sync_meta(`key`, value) VALUES ('sync_enabled', '1')")
        db.execSQL("UPDATE businesses SET name = 'Dengan sync' WHERE id = 'b'")
        assertEquals(listOf("businesses|b|UPSERT"), db.strings("SELECT tableName || '|' || rowId || '|' || op FROM sync_outbox"))
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.category(id: String, role: String, name: String, normalized: String) =
        execSQL("INSERT INTO categories(id, businessId, role, name, normalizedName, aliases, active, createdAtEpochMillis) VALUES ('$id', 'b', '$role', '$name', '$normalized', '', 1, 0)")

    private fun androidx.sqlite.db.SupportSQLiteDatabase.event(
        id: String,
        type: String,
        role: String,
        amount: Long,
        reverses: String? = null,
        posts: List<Pair<String, String>>,
    ) {
        execSQL(
            "INSERT INTO financial_events(id, businessId, projectId, bookId, type, amountRupiah, dateEpochDay, description, category, cashFlowRole, status, reversesEventId, createdAtEpochMillis) " +
                "VALUES ('$id', 'b', 'p', 'k', '$type', $amount, 20000, '$id', 'x', '$role', 'POSTED', ${reverses?.let { "'$it'" } ?: "NULL"}, 0)",
        )
        execSQL("INSERT INTO journal_entries(id, sourceEventId, dateEpochDay, description) VALUES ('journal-$id', '$id', 20000, '$id')")
        posts.forEachIndexed { index, (account, side) ->
            execSQL("INSERT INTO postings(id, journalId, account, side, amountRupiah) VALUES ('$id-$index', 'journal-$id', '$account', '$side', $amount)")
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.strings(sql: String): List<String> =
        query(sql).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.count(sql: String): Int = query(sql).use { it.moveToFirst(); it.getInt(0) }

    private companion object {
        const val DB = "migration-test.db"
    }
}
