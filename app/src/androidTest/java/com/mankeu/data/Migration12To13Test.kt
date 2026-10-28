package com.mankeu.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration12To13Test {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun productsKeepTheirPriceAsFirstSaleUnitAndComponentsBecomePerUnitLines() {
        helper.createDatabase(DB, 12).use { db ->
            db.execSQL("INSERT INTO businesses (id, name, currency, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('b', 'Usaha', 'IDR', 0, 0, 0)")
            db.execSQL("INSERT INTO projects (id, businessId, name, isGeneral, archived, createdAtEpochMillis, description, updatedAtEpochMillis) VALUES ('p', 'b', 'Umum', 1, 0, 0, '', 0)")
            db.execSQL("INSERT INTO products (id, projectId, name, unit, priceRupiah, targetPerMonthText, sortOrder, archived, createdAtEpochMillis, updatedAtEpochMillis, importBatchId, sellFromMonth) VALUES ('kopi', 'p', 'Kopi susu', 'cup', 10000, '700', 0, 0, 0, 0, NULL, 2)")
            db.execSQL("INSERT INTO product_cost_components (id, productId, name, amountRupiah, yieldText, sortOrder) VALUES ('c1', 'kopi', 'Susu', 18000, '6', 0)")
            db.execSQL("INSERT INTO product_cost_components (id, productId, name, amountRupiah, yieldText, sortOrder) VALUES ('c2', 'kopi', 'Gula', 10000, '3', 1)")
        }

        helper.runMigrationsAndValidate(DB, 13, true, Migration12To13).use { db ->
            db.query("SELECT name, unit, batchYieldText, targetPerMonthText, sellFromMonth FROM products").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(listOf("Kopi susu", "cup", "1", "700", "2"), (0..4).map { c.getString(it) })
            }
            db.query("SELECT id, name, contentText, priceRupiah FROM product_units WHERE productId = 'kopi'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(listOf("kopi-u0", "cup", "1", "10000"), (0..3).map { c.getString(it) })
            }
            db.query("SELECT id, amountRupiah, stockItemId FROM product_recipe_lines ORDER BY sortOrder").use { c ->
                val rows = buildList { while (c.moveToNext()) add(Triple(c.getString(0), c.getLong(1), c.isNull(2))) }
                assertEquals(listOf(Triple("c1", 3_000L, true), Triple("c2", 3_333L, true)), rows)
            }
            val triggers = db.query("SELECT name FROM sqlite_master WHERE type = 'trigger'").use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
            listOf("products", "product_units", "product_recipe_lines", "product_movements").forEach { table ->
                assertTrue("missing sync trigger on $table", "sync_${table}_ins" in triggers)
            }
        }
    }

    private companion object { const val DB = "migration-12-13" }
}
