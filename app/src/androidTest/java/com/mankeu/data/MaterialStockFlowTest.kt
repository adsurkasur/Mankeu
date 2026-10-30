package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.StockCalculation
import com.mankeu.domain.StockItem
import com.mankeu.domain.StockMovementType
import com.mankeu.importer.FinanceWorkbookImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Real material stock (owner decision U-2, DEC-030 §3): derived on read from the opening stock, linked
 * purchases, production use, manual moves and counts. Audit L07/L19: buying minyak 10 liter left it at 0.
 */
@RunWith(AndroidJUnit4::class)
class MaterialStockFlowTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository
    private lateinit var businessId: String
    private lateinit var projectId: String
    private lateinit var bookId: String
    private lateinit var planBookId: String
    private lateinit var minyak: String
    private lateinit var singkong: String
    private lateinit var keripik: String
    private val day = LocalDate.of(2026, 10, 1)

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = FinanceRepository(db)
        val (b, p) = repo.createBusinessWithUnit("Keripik Bu Sri", "Umum")
        businessId = b
        projectId = p
        val books = repo.books(p).first()
        bookId = books.single { it.mode == BookMode.REALIZATION.name }.id
        planBookId = books.single { it.mode == BookMode.PROJECTION.name }.id
        minyak = repo.createStockItem(b, p, false, "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal("5"))
        singkong = repo.createStockItem(b, p, false, "Singkong", "kg", 30_000, BigDecimal(100), BigDecimal.ZERO)
        keripik = repo.saveProduct(p, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "400",
            units = listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000)),
            recipe = listOf(
                RecipeLineDraft("", stockItemId = singkong, quantityText = "50"),
                RecipeLineDraft("", stockItemId = minyak, quantityText = "20"),
                RecipeLineDraft("Gas", amountRupiah = 100_000),
            ),
        ))
    }

    @After fun tearDown() = db.close()

    private suspend fun item(id: String, project: String = projectId, business: String = businessId): StockItem =
        repo.stockItems(business, project).first().single { it.id == id }

    private suspend fun stock(id: String) = item(id).currentStock.stripTrailingZeros().toPlainString()

    private fun buy(book: String, liters: String, date: LocalDate = day) =
        EventDraft(businessId, projectId, book, FinancialEventType.CASH_EXPENSE, 260_000, date, "Minyak", "Bahan Baku & Stok", quantityText = liters, unit = "liter", stockItemId = minyak)

    @Test fun buyingProducingDeletingAndCountingMoveTheRealStock() = runBlocking {
        repo.recordEvent(buy(bookId, "10"))
        assertEquals("10", stock(minyak))

        // A plan line is not a purchase that happened.
        repo.recordEvent(buy(planBookId, "99"))
        assertEquals("10", stock(minyak))

        // Bought on credit from a supplier adds stock too.
        val pemasok = repo.saveContact(businessId, null, ContactDraft("Pemasok", isCustomer = false, isSupplier = true))
        val bahan = db.financeDao().getCategories(businessId).single { it.name == "Bahan Baku & Stok" }
        repo.recordEvent(EventDraft(businessId, projectId, bookId, FinancialEventType.CREDIT_PURCHASE, 130_000, day, "Minyak", bahan.name, categoryId = bahan.id, contactId = pemasok, quantityText = "5", unit = "liter", stockItemId = minyak))
        assertEquals("15", stock(minyak))

        repo.recordProduction(keripik, BigDecimal.ONE, day.plusDays(1), "")
        assertEquals("50", stock(singkong))
        assertEquals("-5", stock(minyak))
        assertEquals(listOf(StockMovementType.PURCHASE, StockMovementType.PURCHASE, StockMovementType.CONSUME), item(minyak).history.map { it.movement.type })

        val production = repo.products(projectId).first().single().history.single().id
        repo.deleteProductMovement(production)
        assertEquals("100", stock(singkong))
        assertEquals("15", stock(minyak))
        assertEquals(0, count("SELECT COUNT(*) FROM stock_movements"))

        repo.recordStockMovement(minyak, StockMovementType.ADJUSTMENT, BigDecimal(7), day.plusDays(2), "hitung ulang")
        assertEquals("7", stock(minyak))
        // Taken out after the fact, more than recorded: allowed, the stock shows minus.
        repo.recordStockMovement(minyak, StockMovementType.OUT, BigDecimal(9), day.plusDays(3), "")
        assertEquals("-2", stock(minyak))
    }

    @Test fun twoBatchesUseTwiceTheRecipe() = runBlocking {
        repo.recordProduction(keripik, BigDecimal(2), day, "")
        assertEquals("0", stock(singkong))
        assertEquals("-40", stock(minyak))
    }

    /** Audit L05/L19: "menipis" follows the real stock; no minimum means no warning. */
    @Test fun lowStockUsesTheDerivedStock() = runBlocking {
        suspend fun summary() = StockCalculation.summarize(repo.stockItems(businessId, projectId).first())
        assertTrue(item(minyak).isLowStock)
        assertFalse(item(singkong).isLowStock)
        assertEquals(1, summary().lowStockCount)

        repo.recordEvent(buy(bookId, "10"))

        assertFalse(item(minyak).isLowStock)
        assertEquals(0, summary().lowStockCount)
    }

    /** DEC-012: purchases, production use, manual moves and counts travel, so the copy has the same material stock. */
    @Test fun materialStockSurvivesExportAndImport() = runBlocking {
        repo.recordEvent(buy(bookId, "10"))
        repo.recordProduction(keripik, BigDecimal.ONE, day.plusDays(1), "")
        repo.recordStockMovement(singkong, StockMovementType.IN, BigDecimal(30), day.plusDays(2), "panen sendiri")
        repo.recordStockMovement(minyak, StockMovementType.ADJUSTMENT, BigDecimal(4), day.plusDays(3), "")
        repo.recordStockMovement(minyak, StockMovementType.OUT, BigDecimal(1), day.plusDays(4), "tumpah")
        repo.recordProduction(keripik, BigDecimal.ONE, day.plusDays(5), "")
        val before = repo.stockItems(businessId, projectId).first().associate { it.name to it.currentStock.stripTrailingZeros().toPlainString() }
        assertEquals(mapOf("Minyak" to "-17", "Singkong" to "30"), before)

        val preview = FinanceWorkbookImporter().preview("ekspor.xlsx", repo.exportProjectWorkbook(projectId))
        assertEquals(emptyList<String>(), preview.errors)
        val (other, unit) = repo.createBusinessWithUnit("Usaha Lain", "Pusat")
        repo.commitImport(other, unit, preview)

        val after = repo.stockItems(other, unit).first()
        assertEquals(before, after.associate { it.name to it.currentStock.stripTrailingZeros().toPlainString() })
        assertEquals(
            item(minyak).history.map { it.movement.type to it.stockAfter.stripTrailingZeros().toPlainString() },
            after.single { it.name == "Minyak" }.history.map { it.movement.type to it.stockAfter.stripTrailingZeros().toPlainString() },
        )
        // The copy's production use belongs to the copy's production: deleting it gives the material back.
        val copiedProduction = repo.products(unit).first().single().history.last().id
        repo.deleteProductMovement(copiedProduction)
        assertEquals("3", repo.stockItems(other, unit).first().single { it.name == "Minyak" }.currentStock.stripTrailingZeros().toPlainString())

        repo.undoImportBatch(db.financeDao().getImportBatches(unit).single().id)
        assertEquals(7, count("SELECT COUNT(*) FROM stock_movements"))
    }

    private fun count(sql: String) = db.openHelper.writableDatabase.query(sql).use { it.moveToFirst(); it.getInt(0) }
}
