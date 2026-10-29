package com.mankeu.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.sync.SyncSchema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration13To14Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private fun SupportSQLiteDatabase.event(id: String, book: String, type: String, amount: Long, productId: String? = null, quantity: String? = null, unit: String? = null, unitPrice: Long? = null, stockItemId: String? = null, day: Long = 20_000, batch: String? = null, created: Long = 7) {
        execSQL(
            "INSERT INTO financial_events (id, businessId, projectId, bookId, type, amountRupiah, dateEpochDay, description, category, quantityText, unit, unitPriceRupiah, cashFlowRole, createdAtEpochMillis, productId, stockItemId, importBatchId) " +
                "VALUES (?, 'b', 'p', ?, ?, ?, ?, 'x', 'c', ?, ?, ?, 'OPERATING', ?, ?, ?, ?)",
            arrayOf<Any?>(id, book, type, amount, day, quantity, unit, unitPrice, created, productId, stockItemId, batch),
        )
        execSQL("INSERT INTO journal_entries (id, sourceEventId, dateEpochDay, description) VALUES (?, ?, ?, 'x')", arrayOf<Any>("journal-$id", id, day))
        val (debit, credit) = if (type == "CASH_INCOME") "CASH" to "REVENUE" else "OPERATING_EXPENSE" to "CASH"
        execSQL("INSERT INTO postings (id, journalId, account, side, amountRupiah) VALUES (?, ?, ?, 'DEBIT', ?)", arrayOf<Any>("journal-$id-0", "journal-$id", debit, amount))
        execSQL("INSERT INTO postings (id, journalId, account, side, amountRupiah) VALUES (?, ?, ?, 'CREDIT', ?)", arrayOf<Any>("journal-$id-1", "journal-$id", credit, amount))
    }

    @Test
    fun productCashSalesBecomePaidNotesWithKasPiutangJournals() {
        helper.createDatabase(DB, 13).use { db ->
            db.execSQL("INSERT INTO businesses (id, name, currency, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('b', 'Usaha', 'IDR', 0, 0, 0)")
            db.execSQL("INSERT INTO projects (id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Umum', 1, 0, 0, '', 0)")
            db.execSQL("INSERT INTO books (id, projectId, mode, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('real', 'p', 'REALIZATION', 0, 0), ('plan', 'p', 'PROJECTION', 0, 0)")
            // Keripik: a batch of 10 bungkus from 2 kg singkong + Rp10.000 gas; sold per dus of 40 or per bungkus.
            db.execSQL("INSERT INTO products (id, projectId, name, unit, batchYieldText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, sellFromMonth) VALUES ('krp', 'p', 'Keripik', 'bungkus', '10', 0, 0, 0, 0, 1)")
            db.execSQL("INSERT INTO product_units (id, productId, name, contentText, priceRupiah, sortOrder) VALUES ('u-dus', 'krp', 'dus', '40', 300000, 0), ('u-bks', 'krp', 'bungkus', '1', 8000, 1)")
            db.execSQL("INSERT INTO product_recipe_lines (id, productId, name, stockItemId, quantityText, amountRupiah, sortOrder) VALUES ('r1', 'krp', 'Singkong', 'm1', '2', NULL, 0), ('r2', 'krp', 'Gas', NULL, NULL, 10000, 1)")
            db.execSQL("INSERT INTO stock_items (id, businessId, name, unit, basePriceRupiah, initialStockText, currentStockText, minAlertStockText, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('m1', 'b', 'Singkong', 'kg', 20000, '0', '0', '0', 0, 0, 0)")
            // The latest purchase (Rp25.000/kg) prices singkong, not the item's Rp20.000 or the older Rp22.000.
            db.event("buy1", "real", "CASH_EXPENSE", 44_000, quantity = "2", unitPrice = 22_000, stockItemId = "m1", day = 19_990)
            db.event("buy2", "real", "CASH_EXPENSE", 50_000, quantity = "2", unitPrice = 25_000, stockItemId = "m1", day = 19_995)
            db.event("sale", "real", "CASH_INCOME", 600_000, productId = "krp", quantity = "2", unit = "dus")
            db.event("loose", "real", "CASH_INCOME", 40_000, productId = "krp", quantity = "5", batch = "batch-1", created = 1_234)
            // Review M1: the event's own unit price, the base unit (content 1): 3 × Rp6.000 HPP.
            db.event("bks", "real", "CASH_INCOME", 30_000, productId = "krp", quantity = "3", unit = "bungkus", unitPrice = 9_000)
            // Review M1: a deleted product: content 1, HPP 0; 100.000 ÷ 3 rounds to 33.333.
            db.event("gone", "real", "CASH_INCOME", 100_000, productId = "deleted", quantity = "3", unit = "pak")
            // Review M2: a unit renamed since ("pak" no longer exists): the line names the unit it counted (dus, 40).
            db.event("renamed", "real", "CASH_INCOME", 300_000, productId = "krp", quantity = "1", unit = "pak")
            db.event("jasa", "real", "CASH_INCOME", 75_000)
            db.event("planned", "plan", "CASH_INCOME", 900_000, productId = "krp", quantity = "3", unit = "dus")
            // Sync on: the conversion must not fill the outbox (both phones derive the same rows).
            SyncSchema.createTriggers(db)
            db.execSQL("INSERT INTO sync_meta (`key`, value) VALUES ('${SyncSchema.SYNC_ENABLED}', '1')")
        }

        helper.runMigrationsAndValidate(DB, 14, true, Migration13To14).use { db ->
            db.query("SELECT id, projectId, dateEpochDay, hppEstimated, contactId IS NULL, discountRupiah, importBatchId, createdAtEpochMillis, updatedAtEpochMillis FROM sales ORDER BY id").use { c ->
                val rows = buildList { while (c.moveToNext()) add((0..8).map { c.getString(it) }) }
                // Timestamps come from the event (two phones migrating alone agree); the batch id follows the event (review I-1).
                assertEquals(
                    listOf(
                        listOf("bks-s", "p", "20000", "1", "1", "0", null, "7", "7"),
                        listOf("gone-s", "p", "20000", "1", "1", "0", null, "7", "7"),
                        listOf("loose-s", "p", "20000", "1", "1", "0", "batch-1", "1234", "1234"),
                        listOf("renamed-s", "p", "20000", "1", "1", "0", null, "7", "7"),
                        listOf("sale-s", "p", "20000", "1", "1", "0", null, "7", "7"),
                    ),
                    rows,
                )
            }
            db.query("SELECT id, saleId, productId, unitName, contentText, quantityText, unitPriceRupiah, totalRupiah, hppRupiah FROM sale_lines ORDER BY id").use { c ->
                val rows = buildList { while (c.moveToNext()) add((0..8).map { c.getString(it) }) }
                // Batch 2 × 25.000 + 10.000 = 60.000 for 10 bungkus. 2 dus = 80 bungkus → 480.000;
                // no unit means the main unit (dus): 5 dus = 200 bungkus → 1.200.000, price 40.000 ÷ 5.
                assertEquals(
                    listOf(
                        listOf("bks-l", "bks-s", "krp", "bungkus", "1", "3", "9000", "30000", "18000"),
                        listOf("gone-l", "gone-s", "deleted", "pak", "1", "3", "33333", "100000", "0"),
                        listOf("loose-l", "loose-s", "krp", "dus", "40", "5", "8000", "40000", "1200000"),
                        listOf("renamed-l", "renamed-s", "krp", "dus", "40", "1", "300000", "300000", "240000"),
                        listOf("sale-l", "sale-s", "krp", "dus", "40", "2", "300000", "600000", "480000"),
                    ),
                    rows,
                )
            }
            db.query("SELECT id, saleId FROM financial_events ORDER BY id").use { c ->
                val rows = buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) }
                assertEquals(
                    listOf("bks" to "bks-s", "buy1" to null, "buy2" to null, "gone" to "gone-s", "jasa" to null, "loose" to "loose-s", "planned" to null, "renamed" to "renamed-s", "sale" to "sale-s"),
                    rows,
                )
            }
            fun postings(event: String) = db.query("SELECT account, side, amountRupiah FROM postings WHERE journalId = ? ORDER BY id", arrayOf<Any>("journal-$event")).use { c ->
                buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getString(1), c.getLong(2))) }
            }
            assertEquals(listOf(Triple("CASH", "DEBIT", 600_000L), Triple("RECEIVABLE", "CREDIT", 600_000L)), postings("sale"))
            assertEquals(listOf(Triple("CASH", "DEBIT", 75_000L), Triple("REVENUE", "CREDIT", 75_000L)), postings("jasa"))
            assertEquals("REVENUE", postings("planned")[1].first)
            db.query("SELECT COUNT(*) FROM (SELECT journalId FROM postings GROUP BY journalId HAVING SUM(CASE side WHEN 'DEBIT' THEN amountRupiah ELSE -amountRupiah END) != 0)").use { c ->
                assertTrue(c.moveToFirst()); assertEquals("unbalanced journals", 0, c.getInt(0))
            }
            val triggers = db.query("SELECT name FROM sqlite_master WHERE type = 'trigger'").use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
            listOf("contacts", "sales", "sale_lines", "consignments", "consignment_lines").forEach { table ->
                assertTrue("missing sync trigger on $table", "sync_${table}_ins" in triggers)
            }
            db.query("SELECT COUNT(*) FROM sync_outbox").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
            // The upgraded phone republishes everything, converted (fix round 2): it covers what skipped old change sets carried.
            db.query("SELECT COUNT(*) FROM sync_meta WHERE `key` = ?", arrayOf<Any>(SyncSchema.REQUIRES_FULL_SNAPSHOT)).use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        }
    }

    private companion object { const val DB = "migration-13-14" }
}
