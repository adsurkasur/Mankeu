package com.mankeu.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import com.mankeu.sync.SyncSchema

/**
 * Schema 14 (Fase 2, DEC-029): contacts, sale notes with per-line HPP, consignments; events gain
 * saleId / contactId / dueDateEpochDay / settlesEventId; product movements gain consignmentId.
 * Then F2-4 (DEC-029 §10): every Realisasi cash sale naming a product becomes a paid one-line note
 * through [LegacySaleShape], and its journal is rebuilt as Kas / Piutang. The conversion runs with
 * change capture off (both phones derive the same rows), and [SyncSchema.createTriggers] adds the
 * triggers of the five new tables at the end.
 */
object Migration13To14 : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, `phone` TEXT, `address` TEXT, `notes` TEXT, `isCustomer` INTEGER NOT NULL, `isSupplier` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, `importBatchId` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`businessId`) REFERENCES `businesses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_contacts_businessId` ON `contacts` (`businessId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_contacts_businessId_normalizedName` ON `contacts` (`businessId`, `normalizedName`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sales` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `projectId` TEXT NOT NULL, `contactId` TEXT, `dateEpochDay` INTEGER NOT NULL, `dueDateEpochDay` INTEGER, `discountRupiah` INTEGER NOT NULL, `notes` TEXT NOT NULL, `consignmentId` TEXT, `hppEstimated` INTEGER NOT NULL, `importBatchId` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_projectId` ON `sales` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_contactId` ON `sales` (`contactId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_consignmentId` ON `sales` (`consignmentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_importBatchId` ON `sales` (`importBatchId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sale_lines` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `productId` TEXT NOT NULL, `unitName` TEXT NOT NULL, `contentText` TEXT NOT NULL, `quantityText` TEXT NOT NULL, `unitPriceRupiah` INTEGER NOT NULL, `totalRupiah` INTEGER NOT NULL, `hppRupiah` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_lines_saleId` ON `sale_lines` (`saleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_lines_productId` ON `sale_lines` (`productId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `consignments` (`id` TEXT NOT NULL, `businessId` TEXT NOT NULL, `projectId` TEXT NOT NULL, `contactId` TEXT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `notes` TEXT NOT NULL, `importBatchId` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_consignments_projectId` ON `consignments` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_consignments_contactId` ON `consignments` (`contactId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `consignment_lines` (`id` TEXT NOT NULL, `consignmentId` TEXT NOT NULL, `productId` TEXT NOT NULL, `unitName` TEXT NOT NULL, `contentText` TEXT NOT NULL, `quantityText` TEXT NOT NULL, `priceRupiah` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`consignmentId`) REFERENCES `consignments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_consignment_lines_consignmentId` ON `consignment_lines` (`consignmentId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_consignment_lines_productId` ON `consignment_lines` (`productId`)")

        listOf("saleId" to "TEXT", "contactId" to "TEXT", "dueDateEpochDay" to "INTEGER", "settlesEventId" to "TEXT").forEach { (column, type) ->
            db.execSQL("ALTER TABLE financial_events ADD COLUMN `$column` $type")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_events_$column` ON `financial_events` (`$column`)")
        }
        db.execSQL("ALTER TABLE product_movements ADD COLUMN `consignmentId` TEXT")

        SyncSchema.quietly(db) { convertSales(db) }
        SyncSchema.createTriggers(db)
        // The next sync publishes this phone's whole data, converted, so a phone that skipped this
        // phone's schema-13 change sets (SyncEngine.MIN_SYNC_SCHEMA) still gets it. Harmless while
        // sync is off: switching it on sets the same flag.
        db.execSQL("INSERT OR REPLACE INTO sync_meta(`key`, value) VALUES ('${SyncSchema.REQUIRES_FULL_SNAPSHOT}', '1')")
    }

    private fun convertSales(db: SupportSQLiteDatabase) {
        val oldSales = db.query(
            "SELECT e.id, e.businessId, e.projectId, e.productId, e.dateEpochDay, e.createdAtEpochMillis, e.amountRupiah, e.quantityText, e.unit, e.unitPriceRupiah, e.importBatchId " +
                "FROM financial_events e INNER JOIN books b ON b.id = e.bookId " +
                "WHERE e.type = ? AND e.productId IS NOT NULL AND b.mode = ?",
            arrayOf<Any>(FinancialEventType.CASH_INCOME.name, BookMode.REALIZATION.name),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(
                    LegacySaleShape.OldSale(
                        c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getLong(5), c.getLong(6),
                        c.textOrNull(7), c.textOrNull(8), if (c.isNull(9)) null else c.getLong(9), c.textOrNull(10),
                    ),
                )
            }
        }
        if (oldSales.isEmpty()) return

        val products = readProducts(db)
        val prices = LegacySaleShape.materialPrices(
            items = db.query("SELECT id, businessId, basePriceRupiah FROM stock_items").use { c ->
                buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getString(1), c.getLong(2))) }
            },
            // Same order as FinanceDao.latestPurchasePrice, reversed: the last row per material wins.
            purchasesOldestFirst = db.query(
                "SELECT stockItemId, unitPriceRupiah FROM financial_events WHERE stockItemId IS NOT NULL AND unitPriceRupiah > 0 ORDER BY dateEpochDay, createdAtEpochMillis, rowid",
            ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getLong(1)) } },
        )

        oldSales.forEach { old ->
            val (sale, line) = LegacySaleShape.convert(old, products[old.productId], prices[old.businessId].orEmpty())
            db.insert("sales", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", sale.id); put("businessId", sale.businessId); put("projectId", sale.projectId); putNull("contactId")
                put("dateEpochDay", sale.dateEpochDay); putNull("dueDateEpochDay"); put("discountRupiah", sale.discountRupiah)
                put("notes", sale.notes); putNull("consignmentId"); put("hppEstimated", 1); put("importBatchId", sale.importBatchId)
                put("createdAtEpochMillis", sale.createdAtEpochMillis); put("updatedAtEpochMillis", sale.updatedAtEpochMillis)
            })
            db.insert("sale_lines", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", line.id); put("saleId", line.saleId); put("productId", line.productId); put("unitName", line.unitName)
                put("contentText", line.contentText); put("quantityText", line.quantityText); put("unitPriceRupiah", line.unitPriceRupiah)
                put("totalRupiah", line.totalRupiah); put("hppRupiah", line.hppRupiah); put("sortOrder", line.sortOrder)
            })
            db.execSQL("UPDATE financial_events SET saleId = ? WHERE id = ?", arrayOf<Any>(sale.id, old.eventId))
            val journalId = db.query("SELECT id FROM journal_entries WHERE sourceEventId = ?", arrayOf<Any>(old.eventId)).use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: return@forEach
            db.execSQL("DELETE FROM postings WHERE journalId = ?", arrayOf<Any>(journalId))
            LegacySaleShape.paymentPostings(journalId, old).forEach { p ->
                db.insert("postings", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                    put("id", p.id); put("journalId", p.journalId); put("account", p.account); put("side", p.side); put("amountRupiah", p.amountRupiah)
                })
            }
        }
    }

    /** Only the columns [ProductRecord.spec] and [ProductRecord.contentOf] read. */
    private fun readProducts(db: SupportSQLiteDatabase): Map<String, ProductRecord> {
        val units = db.query("SELECT id, productId, name, contentText, priceRupiah, sortOrder FROM product_units").use { c ->
            buildList { while (c.moveToNext()) add(ProductUnitEntity(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getInt(5))) }
        }.groupBy { it.productId }
        val recipe = db.query("SELECT id, productId, name, stockItemId, quantityText, amountRupiah, sortOrder FROM product_recipe_lines").use { c ->
            buildList {
                while (c.moveToNext()) add(
                    ProductRecipeLineEntity(c.getString(0), c.getString(1), c.getString(2), c.textOrNull(3), c.textOrNull(4), if (c.isNull(5)) null else c.getLong(5), c.getInt(6)),
                )
            }
        }.groupBy { it.productId }
        return db.query("SELECT id, projectId, name, unit, batchYieldText, createdAtEpochMillis FROM products").use { c ->
            buildMap {
                while (c.moveToNext()) {
                    val product = ProductEntity(id = c.getString(0), projectId = c.getString(1), name = c.getString(2), unit = c.getString(3), batchYieldText = c.getString(4), createdAtEpochMillis = c.getLong(5))
                    put(product.id, ProductRecord(product, units[product.id].orEmpty(), recipe[product.id].orEmpty()))
                }
            }
        }
    }

    private fun Cursor.textOrNull(index: Int): String? = if (isNull(index)) null else getString(index)
}
