package com.mankeu.backup

import com.mankeu.data.LegacyProductShape
import com.mankeu.data.LegacySaleShape
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecipeLineEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductUnitEntity
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType

/**
 * Brings a snapshot from an older schema to the current one, the way the Room migrations bring a
 * database (spec #1 carries schema 12 forward). Columns a newer schema dropped are ignored by the
 * restore and new columns take their SQL default, so only reshaped tables need work here.
 * Steps chain: a schema-12 snapshot goes 12 → 13 → 14.
 */
object BackupUpgrade {
    fun toCurrent(snapshot: PortableBackup): PortableBackup {
        var upgraded = snapshot
        if (upgraded.schemaVersion == 12) upgraded = from12(upgraded)
        if (upgraded.schemaVersion == 13) upgraded = from13(upgraded)
        return upgraded
    }

    private fun BackupTable.text(row: List<BackupValue>, column: String) = columns.indexOf(column).takeIf { it >= 0 }?.let { (row[it] as? BackupValue.Text)?.value }
    private fun BackupTable.long(row: List<BackupValue>, column: String) = (row[columns.indexOf(column)] as BackupValue.Integer).value
    private fun BackupTable.longOrNull(row: List<BackupValue>, column: String) = columns.indexOf(column).takeIf { it >= 0 }?.let { (row[it] as? BackupValue.Integer)?.value }
    private fun text(value: String?): BackupValue = value?.let { BackupValue.Text(it) } ?: BackupValue.Null

    /** Mirrors `Migration12To13`: price → first sale unit, components → per-unit recipe lines. */
    private fun from12(snapshot: PortableBackup): PortableBackup {
        val byName = snapshot.tables.associateBy { it.name }
        val products = byName.getValue("products")
        val components = byName.getValue("product_cost_components")

        val units = BackupTable(
            name = "product_units",
            columns = listOf("id", "productId", "name", "contentText", "priceRupiah", "sortOrder"),
            rows = products.rows.map { row ->
                val unit = LegacyProductShape.saleUnit(products.text(row, "id")!!, products.text(row, "unit")!!, products.long(row, "priceRupiah"))
                listOf(BackupValue.Text(unit.id), BackupValue.Text(unit.productId), BackupValue.Text(unit.name), BackupValue.Text(unit.contentText), BackupValue.Integer(unit.priceRupiah), BackupValue.Integer(0))
            },
        )
        val lines = BackupTable(
            name = "product_recipe_lines",
            columns = listOf("id", "productId", "name", "stockItemId", "quantityText", "amountRupiah", "sortOrder"),
            rows = components.rows.map { row ->
                val line = LegacyProductShape.recipeLine(
                    components.text(row, "id")!!, components.text(row, "productId")!!, components.text(row, "name")!!,
                    components.long(row, "amountRupiah"), components.text(row, "yieldText"), components.long(row, "sortOrder").toInt(),
                )
                listOf(BackupValue.Text(line.id), BackupValue.Text(line.productId), BackupValue.Text(line.name), BackupValue.Null, BackupValue.Null, BackupValue.Integer(line.amountRupiah!!), BackupValue.Integer(line.sortOrder.toLong()))
            },
        )
        val movements = BackupTable(name = "product_movements", columns = listOf("id"), rows = emptyList())
        return snapshot.copy(
            schemaVersion = 13,
            tables = snapshot.tables.filterNot { it.name == "product_cost_components" } + units + lines + movements,
        )
    }

    /** Mirrors `Migration13To14`: product cash sales become paid one-line notes (F2-4, DEC-029 §10). */
    private fun from13(snapshot: PortableBackup): PortableBackup {
        val byName = snapshot.tables.associateBy { it.name }
        val events = byName.getValue("financial_events")
        val realization = byName.getValue("books").let { books ->
            books.rows.filter { books.text(it, "mode") == BookMode.REALIZATION.name }.map { books.text(it, "id")!! }.toSet()
        }
        val oldSales = events.rows.filter { row ->
            events.text(row, "type") == FinancialEventType.CASH_INCOME.name && events.text(row, "productId") != null && events.text(row, "bookId") in realization
        }.map { row ->
            LegacySaleShape.OldSale(
                eventId = events.text(row, "id")!!,
                businessId = events.text(row, "businessId")!!,
                projectId = events.text(row, "projectId")!!,
                productId = events.text(row, "productId")!!,
                dateEpochDay = events.long(row, "dateEpochDay"),
                createdAtEpochMillis = events.long(row, "createdAtEpochMillis"),
                amountRupiah = events.long(row, "amountRupiah"),
                quantityText = events.text(row, "quantityText"),
                unit = events.text(row, "unit"),
                unitPriceRupiah = events.longOrNull(row, "unitPriceRupiah"),
            )
        }

        val products = products(byName)
        val stock = byName.getValue("stock_items")
        val prices = LegacySaleShape.materialPrices(
            items = stock.rows.map { Triple(stock.text(it, "id")!!, stock.text(it, "businessId")!!, stock.long(it, "basePriceRupiah")) },
            // Rows are captured in rowid order; a stable sort by date then time keeps rowid as the tie-break.
            purchasesOldestFirst = events.rows
                .filter { events.text(it, "stockItemId") != null && (events.longOrNull(it, "unitPriceRupiah") ?: 0) > 0 }
                .sortedWith(compareBy({ events.long(it, "dateEpochDay") }, { events.long(it, "createdAtEpochMillis") }))
                .map { events.text(it, "stockItemId")!! to events.longOrNull(it, "unitPriceRupiah")!! },
        )
        val converted = oldSales.map { it to LegacySaleShape.convert(it, products[it.productId], prices[it.businessId].orEmpty()) }

        val sales = BackupTable(
            name = "sales",
            columns = listOf("id", "businessId", "projectId", "contactId", "dateEpochDay", "dueDateEpochDay", "discountRupiah", "notes", "consignmentId", "hppEstimated", "importBatchId", "createdAtEpochMillis", "updatedAtEpochMillis"),
            rows = converted.map { (_, pair) ->
                val s = pair.first
                listOf(
                    BackupValue.Text(s.id), BackupValue.Text(s.businessId), BackupValue.Text(s.projectId), BackupValue.Null, BackupValue.Integer(s.dateEpochDay),
                    BackupValue.Null, BackupValue.Integer(s.discountRupiah), BackupValue.Text(s.notes), BackupValue.Null, BackupValue.Integer(1), BackupValue.Null,
                    BackupValue.Integer(s.createdAtEpochMillis), BackupValue.Integer(s.updatedAtEpochMillis),
                )
            },
        )
        val saleLines = BackupTable(
            name = "sale_lines",
            columns = listOf("id", "saleId", "productId", "unitName", "contentText", "quantityText", "unitPriceRupiah", "totalRupiah", "hppRupiah", "sortOrder"),
            rows = converted.map { (_, pair) ->
                val l = pair.second
                listOf(
                    BackupValue.Text(l.id), BackupValue.Text(l.saleId), BackupValue.Text(l.productId), BackupValue.Text(l.unitName), BackupValue.Text(l.contentText),
                    BackupValue.Text(l.quantityText), BackupValue.Integer(l.unitPriceRupiah), BackupValue.Integer(l.totalRupiah), BackupValue.Integer(l.hppRupiah),
                    BackupValue.Integer(l.sortOrder.toLong()),
                )
            },
        )

        val saleOf = converted.associate { (old, pair) -> old.eventId to pair.first.id }
        val upgradedEvents = events.copy(
            columns = events.columns + "saleId",
            rows = events.rows.map { row -> row + text(saleOf[events.text(row, "id")]) },
        )

        val journals = byName.getValue("journal_entries")
        val journalOf = journals.rows.associate { journals.text(it, "sourceEventId")!! to journals.text(it, "id")!! }
        val rebuilt = oldSales.mapNotNull { old -> journalOf[old.eventId]?.let { LegacySaleShape.paymentPostings(it, old) } }.flatten()
        val rebuiltJournals = rebuilt.map { it.journalId }.toSet()
        val postings = byName.getValue("postings")
        val upgradedPostings = BackupTable(
            name = "postings",
            columns = listOf("id", "journalId", "account", "side", "amountRupiah"),
            rows = postings.rows.filterNot { postings.text(it, "journalId") in rebuiltJournals }.map { row ->
                listOf(text(postings.text(row, "id")), text(postings.text(row, "journalId")), text(postings.text(row, "account")), text(postings.text(row, "side")), BackupValue.Integer(postings.long(row, "amountRupiah")))
            } + rebuilt.map { listOf(BackupValue.Text(it.id), BackupValue.Text(it.journalId), BackupValue.Text(it.account), BackupValue.Text(it.side), BackupValue.Integer(it.amountRupiah)) },
        )

        val empty = listOf("contacts", "consignments", "consignment_lines").map { BackupTable(it, listOf("id"), emptyList()) }
        return snapshot.copy(
            schemaVersion = 14,
            tables = snapshot.tables.filterNot { it.name == "financial_events" || it.name == "postings" } + upgradedEvents + upgradedPostings + sales + saleLines + empty,
        )
    }

    /** Only the columns [ProductRecord.spec] and [ProductRecord.contentOf] read; a schema-12 product has no batch yield (1). */
    private fun products(byName: Map<String, BackupTable>): Map<String, ProductRecord> {
        val units = byName.getValue("product_units").let { t ->
            t.rows.map { ProductUnitEntity(t.text(it, "id")!!, t.text(it, "productId")!!, t.text(it, "name")!!, t.text(it, "contentText")!!, t.long(it, "priceRupiah"), t.long(it, "sortOrder").toInt()) }
        }.groupBy { it.productId }
        val recipe = byName.getValue("product_recipe_lines").let { t ->
            t.rows.map {
                ProductRecipeLineEntity(t.text(it, "id")!!, t.text(it, "productId")!!, t.text(it, "name")!!, t.text(it, "stockItemId"), t.text(it, "quantityText"), t.longOrNull(it, "amountRupiah"), t.long(it, "sortOrder").toInt())
            }
        }.groupBy { it.productId }
        val products = byName.getValue("products")
        return products.rows.associate { row ->
            val product = ProductEntity(
                id = products.text(row, "id")!!,
                projectId = products.text(row, "projectId").orEmpty(),
                name = products.text(row, "name").orEmpty(),
                unit = products.text(row, "unit")!!,
                batchYieldText = products.text(row, "batchYieldText") ?: "1",
                createdAtEpochMillis = 0,
            )
            product.id to ProductRecord(product, units[product.id].orEmpty(), recipe[product.id].orEmpty())
        }
    }
}
