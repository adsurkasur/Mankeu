package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.ProductCosting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/** Product stock and material price are derived on read (DEC-028 §2 §4); nothing stored goes stale. */
@RunWith(AndroidJUnit4::class)
class ProductStockFlowTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = FinanceRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun keripikGolden_productionSalesAndOilPrice() = runBlocking {
        val (businessId, projectId) = repo.createBusinessWithUnit("Keripik Bu Sri", "Umum")
        val book = repo.books(projectId).first().single { it.mode == BookMode.REALIZATION.name }
        val minyak = repo.createStockItem(businessId, projectId, false, "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        val singkong = repo.createStockItem(businessId, projectId, false, "Singkong", "kg", 30_000, BigDecimal.ZERO, BigDecimal.ZERO)
        val productId = repo.saveProduct(projectId, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "400",
            units = listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000), SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000)),
            recipe = listOf(
                RecipeLineDraft("", stockItemId = singkong, quantityText = "50"),
                RecipeLineDraft("", stockItemId = minyak, quantityText = "20"),
                RecipeLineDraft("Bumbu & kemasan", amountRupiah = 300_000),
                RecipeLineDraft("Gas", amountRupiah = 100_000),
            ),
        ))
        suspend fun keripik() = repo.products(projectId).first().single { it.product.id == productId }
        suspend fun prices() = repo.stockItems(businessId, projectId).first().associate { it.id to it.basePriceRupiah }

        assertEquals(listOf(6_000L, 240_000L), ProductCosting.cost(keripik().spec, prices()).perSaleUnit.map { it.hppRupiah })

        repo.recordProduction(productId, BigDecimal.ONE, LocalDate.of(2026, 10, 1), "")
        assertEquals(0, BigDecimal(400).compareTo(keripik().stock))

        fun sale(qty: String, unit: String?) = EventDraft(businessId, projectId, book.id, FinancialEventType.CASH_INCOME, 600_000, LocalDate.of(2026, 10, 2), "Jual ke Toko A", "Penjualan", quantityText = qty, unit = unit, productId = productId)
        val saleId = repo.recordEvent(sale("2", "dus"))
        assertEquals(0, BigDecimal(320).compareTo(keripik().stock))
        repo.updateEvent(saleId, sale("3", "dus").copy(amountRupiah = 900_000))
        assertEquals(0, BigDecimal(280).compareTo(keripik().stock))
        repo.deleteEvent(saleId)
        assertEquals(0, BigDecimal(400).compareTo(keripik().stock))

        val purchase = repo.recordEvent(EventDraft(businessId, projectId, book.id, FinancialEventType.CASH_EXPENSE, 550_000, LocalDate.of(2026, 10, 3), "Minyak", "Bahan baku", quantityText = "20", unit = "liter", stockItemId = minyak))
        assertEquals(27_500L, prices()[minyak])
        assertEquals(27_500L, repo.materialPrice(minyak))
        assertEquals(listOf(6_125L, 245_000L), ProductCosting.cost(keripik().spec, prices()).perSaleUnit.map { it.hppRupiah })
        repo.deleteEvent(purchase)
        assertEquals(25_000L, prices()[minyak])

        val bad = runCatching { repo.recordEvent(sale("1", "karung")) }
        assertTrue(bad.exceptionOrNull()?.message.orEmpty().contains("karung"))
    }

    /** Final review I-4 / M-5: both price reads pick the same purchase on a tie, and a Rp0 price never wins. */
    @Test fun bothPriceReadsAgreeOnTiesAndIgnoreZeroPrices() = runBlocking {
        val (businessId, projectId) = repo.createBusinessWithUnit("Keripik Bu Sri", "Umum")
        val book = repo.books(projectId).first().single { it.mode == BookMode.REALIZATION.name }
        val minyak = repo.createStockItem(businessId, projectId, false, "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        fun buy(amount: Long) = EventDraft(businessId, projectId, book.id, FinancialEventType.CASH_EXPENSE, amount, LocalDate.of(2026, 10, 3), "Minyak", "Bahan baku", quantityText = "1", unit = "liter", stockItemId = minyak)
        repo.recordEvent(buy(20_000))
        val second = repo.recordEvent(buy(30_000))
        // An import inserts rows within one millisecond: same date, same createdAt.
        db.openHelper.writableDatabase.execSQL("UPDATE financial_events SET createdAtEpochMillis = 1000 WHERE stockItemId = ?", arrayOf(minyak))
        suspend fun flowPrice() = repo.stockItems(businessId, projectId).first().single { it.id == minyak }.basePriceRupiah

        assertEquals(30_000L, flowPrice())
        assertEquals(30_000L, repo.materialPrice(minyak))

        db.openHelper.writableDatabase.execSQL("UPDATE financial_events SET unitPriceRupiah = 0 WHERE id = ?", arrayOf(second))
        assertEquals(20_000L, flowPrice())
        assertEquals(20_000L, repo.materialPrice(minyak))
    }
}
