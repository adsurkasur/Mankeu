package com.mankeu.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mankeu.sync.SyncSchema

/**
 * Schema 13 (Fase 1, DEC-028): products get a base unit, a batch yield, sale units and a per-batch
 * recipe; expenses can name the material they bought. Old products are reshaped by
 * [LegacyProductShape]. `products` is rebuilt (its price moves to `product_units`), which drops its
 * sync triggers, so [SyncSchema.createTriggers] runs at the end (see SyncSchema's KDoc).
 */
object Migration12To13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Read the old shape before anything is dropped. Nothing references products except
        // product_cost_components, which goes first, so the rebuild cascades into nothing.
        val units = db.query("SELECT id, unit, priceRupiah FROM products").use { c ->
            buildList { while (c.moveToNext()) add(LegacyProductShape.saleUnit(c.getString(0), c.getString(1), c.getLong(2))) }
        }
        val lines = db.query("SELECT id, productId, name, amountRupiah, yieldText, sortOrder FROM product_cost_components").use { c ->
            buildList {
                while (c.moveToNext()) add(LegacyProductShape.recipeLine(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getInt(5)))
            }
        }
        db.execSQL("DROP TABLE product_cost_components")

        db.execSQL("CREATE TABLE IF NOT EXISTS `products_new` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `batchYieldText` TEXT NOT NULL DEFAULT '1', `targetPerMonthText` TEXT, `sortOrder` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, `importBatchId` TEXT, `sellFromMonth` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`id`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL(
            "INSERT INTO products_new (id, projectId, name, unit, batchYieldText, targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, importBatchId, sellFromMonth) " +
                "SELECT id, projectId, name, unit, '1', targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, importBatchId, sellFromMonth FROM products",
        )
        db.execSQL("DROP TABLE products")
        db.execSQL("ALTER TABLE products_new RENAME TO products")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_projectId` ON `products` (`projectId`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `product_units` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `name` TEXT NOT NULL, `contentText` TEXT NOT NULL, `priceRupiah` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_units_productId` ON `product_units` (`productId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `product_recipe_lines` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `name` TEXT NOT NULL, `stockItemId` TEXT, `quantityText` TEXT, `amountRupiah` INTEGER, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_recipe_lines_productId` ON `product_recipe_lines` (`productId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_recipe_lines_stockItemId` ON `product_recipe_lines` (`stockItemId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `product_movements` (`id` TEXT NOT NULL, `productId` TEXT NOT NULL, `movementType` TEXT NOT NULL, `quantityText` TEXT NOT NULL, `batchesText` TEXT, `dateEpochDay` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_movements_productId` ON `product_movements` (`productId`)")

        units.forEach { u ->
            db.insert("product_units", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", u.id); put("productId", u.productId); put("name", u.name)
                put("contentText", u.contentText); put("priceRupiah", u.priceRupiah); put("sortOrder", u.sortOrder)
            })
        }
        lines.forEach { l ->
            db.insert("product_recipe_lines", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", l.id); put("productId", l.productId); put("name", l.name)
                putNull("stockItemId"); putNull("quantityText"); put("amountRupiah", l.amountRupiah); put("sortOrder", l.sortOrder)
            })
        }

        db.execSQL("ALTER TABLE financial_events ADD COLUMN `stockItemId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_events_stockItemId` ON `financial_events` (`stockItemId`)")

        SyncSchema.createTriggers(db)
    }
}
