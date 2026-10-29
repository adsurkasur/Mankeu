package com.mankeu.backup

import com.mankeu.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BackupUpgradeTest {
    private fun table(name: String, columns: List<String> = listOf("id"), rows: List<List<BackupValue>> = emptyList()) = BackupTable(name, columns, rows)
    private fun t(value: String) = BackupValue.Text(value)
    private fun i(value: Long) = BackupValue.Integer(value)
    private val nul = BackupValue.Null

    private val newIn14 = listOf("contacts", "sales", "sale_lines", "consignments", "consignment_lines")

    private val schema12Tables = (AppDatabase.DATA_TABLES - listOf("product_units", "product_recipe_lines", "product_movements") - newIn14).map { name ->
        when (name) {
            "products" -> table(name, listOf("id", "projectId", "name", "unit", "priceRupiah"), listOf(listOf(t("kopi"), t("p"), t("Kopi"), t("cup"), i(10_000))))
            "books" -> table(name, listOf("id", "projectId", "mode"), listOf(listOf(t("real"), t("p"), t("REALIZATION"))))
            "financial_events" -> table(name, listOf("id", "businessId", "projectId", "bookId", "type", "amountRupiah", "dateEpochDay", "createdAtEpochMillis", "productId", "quantityText"),
                listOf(listOf(t("k1"), t("b"), t("p"), t("real"), t("CASH_INCOME"), i(30_000), i(20_000), i(5), t("kopi"), t("3"))))
            "journal_entries" -> table(name, listOf("id", "sourceEventId"), listOf(listOf(t("journal-k1"), t("k1"))))
            "postings" -> table(name, listOf("id", "journalId", "account", "side", "amountRupiah"), listOf(
                listOf(t("journal-k1-0"), t("journal-k1"), t("CASH"), t("DEBIT"), i(30_000)),
                listOf(t("journal-k1-1"), t("journal-k1"), t("REVENUE"), t("CREDIT"), i(30_000)),
            ))
            else -> table(name)
        }
    } + table(
        "product_cost_components",
        listOf("id", "productId", "name", "amountRupiah", "yieldText", "sortOrder"),
        listOf(listOf(t("c1"), t("kopi"), t("Susu"), i(18_000), t("6"), i(0))),
    )

    @Test fun `schema 12 snapshot gets the current table set and the reshaped product rows`() {
        val upgraded = BackupUpgrade.toCurrent(PortableBackup(12, 0, schema12Tables))
        assertEquals(14, upgraded.schemaVersion)
        assertEquals(AppDatabase.DATA_TABLES.toSet(), upgraded.tables.map { it.name }.toSet())
        val units = upgraded.tables.single { it.name == "product_units" }
        assertEquals(listOf(t("kopi-u0"), t("kopi"), t("cup"), t("1"), i(10_000), i(0)), units.rows.single())
        val lines = upgraded.tables.single { it.name == "product_recipe_lines" }
        assertEquals(BackupValue.Integer(3_000), lines.rows.single()[5])
        // ...and then 13 → 14: the sale (no unit: the main unit "cup") is a paid note at 3 × Rp3.000 HPP.
        val line = upgraded.tables.single { it.name == "sale_lines" }.let { tbl -> tbl.columns.zip(tbl.rows.single()).toMap() }
        assertEquals(listOf(t("k1-l"), t("k1-s"), t("cup"), t("1"), t("3"), i(10_000), i(30_000), i(9_000)), listOf("id", "saleId", "unitName", "contentText", "quantityText", "unitPriceRupiah", "totalRupiah", "hppRupiah").map { line.getValue(it) })
        val postings = upgraded.tables.single { it.name == "postings" }
        assertEquals(listOf(t("CASH"), t("RECEIVABLE")), postings.rows.map { it[2] })
    }

    private val eventColumns = listOf("id", "businessId", "projectId", "bookId", "type", "amountRupiah", "dateEpochDay", "createdAtEpochMillis", "productId", "quantityText", "unit", "unitPriceRupiah", "stockItemId")
    private fun event(id: String, book: String, type: String, amount: Long, productId: String? = null, quantity: String? = null, unit: String? = null, unitPrice: Long? = null, stockItemId: String? = null, day: Long = 20_000, created: Long = 0) =
        listOf(t(id), t("b"), t("p"), t(book), t(type), i(amount), i(day), i(created), productId?.let(::t) ?: nul, quantity?.let(::t) ?: nul, unit?.let(::t) ?: nul, unitPrice?.let(::i) ?: nul, stockItemId?.let(::t) ?: nul)

    private fun schema13(eventRows: List<List<BackupValue>>): PortableBackup {
        val tables = (AppDatabase.DATA_TABLES - newIn14).map { name ->
            when (name) {
                "books" -> table(name, listOf("id", "projectId", "mode"), listOf(listOf(t("real"), t("p"), t("REALIZATION")), listOf(t("plan"), t("p"), t("PROJECTION"))))
                // Keripik: batch of 10 bungkus; sold per bungkus or per dus of 40.
                "products" -> table(name, listOf("id", "projectId", "name", "unit", "batchYieldText"), listOf(listOf(t("krp"), t("p"), t("Keripik"), t("bungkus"), t("10"))))
                "product_units" -> table(name, listOf("id", "productId", "name", "contentText", "priceRupiah", "sortOrder"), listOf(
                    listOf(t("u-dus"), t("krp"), t("dus"), t("40"), i(300_000), i(0)),
                    listOf(t("u-bks"), t("krp"), t("bungkus"), t("1"), i(8_000), i(1)),
                ))
                // 2 kg singkong + Rp10.000 gas per batch.
                "product_recipe_lines" -> table(name, listOf("id", "productId", "name", "stockItemId", "quantityText", "amountRupiah", "sortOrder"), listOf(
                    listOf(t("r1"), t("krp"), t("Singkong"), t("m1"), t("2"), nul, i(0)),
                    listOf(t("r2"), t("krp"), t("Gas"), nul, nul, i(10_000), i(1)),
                ))
                "stock_items" -> table(name, listOf("id", "businessId", "basePriceRupiah"), listOf(listOf(t("m1"), t("b"), i(20_000))))
                "financial_events" -> table(name, eventColumns, eventRows)
                "journal_entries" -> table(name, listOf("id", "sourceEventId", "dateEpochDay", "description"), eventRows.map { listOf(t("journal-${(it[0] as BackupValue.Text).value}"), it[0], it[6], t("x")) })
                "postings" -> table(name, listOf("id", "journalId", "account", "side", "amountRupiah"), eventRows.flatMap {
                    val j = "journal-${(it[0] as BackupValue.Text).value}"
                    listOf(listOf(t("$j-0"), t(j), t("CASH"), t("DEBIT"), it[5]), listOf(t("$j-1"), t(j), t("REVENUE"), t("CREDIT"), it[5]))
                })
                else -> table(name)
            }
        }
        return PortableBackup(13, 0, tables)
    }

    @Test fun `schema 13 product cash sale becomes a paid note with the recipe HPP at upgrade`() {
        val upgraded = BackupUpgrade.toCurrent(
            schema13(
                listOf(
                    // Two purchases of singkong; the later one (Rp25.000/kg) prices it, not the item's Rp20.000.
                    event("buy1", "real", "CASH_EXPENSE", 44_000, quantity = "2", unitPrice = 22_000, stockItemId = "m1", day = 19_990),
                    event("buy2", "real", "CASH_EXPENSE", 50_000, quantity = "2", unitPrice = 25_000, stockItemId = "m1", day = 19_995),
                    event("sale", "real", "CASH_INCOME", 600_000, productId = "krp", quantity = "2", unit = "dus"),
                    event("jasa", "real", "CASH_INCOME", 75_000),
                    event("planned", "plan", "CASH_INCOME", 900_000, productId = "krp", quantity = "3", unit = "dus"),
                ),
            ),
        )
        assertEquals(14, upgraded.schemaVersion)
        assertEquals(AppDatabase.DATA_TABLES.toSet(), upgraded.tables.map { it.name }.toSet())
        fun rows(name: String) = upgraded.tables.single { it.name == name }.let { tbl -> tbl.rows.map { tbl.columns.zip(it).toMap() } }

        val sale = rows("sales").single()
        assertEquals(t("sale-s"), sale["id"])
        assertEquals(i(1), sale["hppEstimated"])
        assertEquals(i(20_000), sale["dateEpochDay"])
        val line = rows("sale_lines").single()
        // Batch 2 × 25.000 + 10.000 = 60.000 for 10 bungkus; 2 dus = 80 bungkus → 480.000.
        assertEquals(
            mapOf("id" to t("sale-l"), "saleId" to t("sale-s"), "productId" to t("krp"), "unitName" to t("dus"), "contentText" to t("40"), "quantityText" to t("2"),
                "unitPriceRupiah" to i(300_000), "totalRupiah" to i(600_000), "hppRupiah" to i(480_000), "sortOrder" to i(0)),
            line,
        )
        val saleIds = rows("financial_events").associate { it["id"] to it["saleId"] }
        assertEquals(mapOf(t("buy1") to nul, t("buy2") to nul, t("sale") to t("sale-s"), t("jasa") to nul, t("planned") to nul), saleIds)

        val postings = rows("postings").groupBy({ it["journalId"] }, { Triple(it["account"], it["side"], it["amountRupiah"]) })
        assertEquals(listOf(Triple(t("CASH"), t("DEBIT"), i(600_000)), Triple(t("RECEIVABLE"), t("CREDIT"), i(600_000))), postings[t("journal-sale")])
        assertEquals(listOf(Triple(t("CASH"), t("DEBIT"), i(75_000)), Triple(t("REVENUE"), t("CREDIT"), i(75_000))), postings[t("journal-jasa")])
        assertEquals(t("REVENUE"), postings.getValue(t("journal-planned"))[1].first)
        assertEquals(10, rows("postings").size)
    }

    @Test fun `current snapshot passes through untouched`() {
        val current = PortableBackup(14, 0, AppDatabase.DATA_TABLES.map { table(it) })
        assertSame(current, BackupUpgrade.toCurrent(current))
    }
}
