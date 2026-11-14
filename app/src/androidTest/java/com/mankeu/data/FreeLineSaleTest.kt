package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.BookMode
import com.mankeu.domain.ProfitBasis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class FreeLineSaleTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository
    private lateinit var businessId: String
    private lateinit var projectId: String
    private lateinit var bookId: String
    private lateinit var buyerId: String
    private lateinit var productId: String
    private val day = LocalDate.of(2026, 10, 5)

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = FinanceRepository(db)
        val (business, project) = repo.createBusinessWithUnit("Usaha Uji", "Umum")
        businessId = business
        projectId = project
        bookId = repo.books(projectId).first().single { it.mode == BookMode.REALIZATION.name }.id
        buyerId = repo.saveContact(businessId, null, ContactDraft("Bu Ani"))
        productId = repo.saveProduct(projectId, null, ProductDraft(
            name = "Oli",
            baseUnit = "botol",
            batchYieldText = "1",
            units = listOf(SaleUnitDraft(name = "botol", contentText = "1", priceRupiah = 55_000)),
            recipe = listOf(RecipeLineDraft("Bahan", amountRupiah = 5_000)),
        ))
        repo.recordProduction(productId, BigDecimal("5"), day.minusDays(1), "")
    }

    @After fun tearDown() = db.close()

    @Test fun freeLineCanBePartPaidWithoutChangingAnyProductStock() = runBlocking {
        val stockBefore = repo.products(projectId).first().single().stock

        val saleId = repo.saveSale(projectId, null, SaleDraft(
            contactId = buyerId,
            date = day,
            dueDate = null,
            discountRupiah = 0,
            notes = "",
            lines = listOf(SaleLineDraft(null, "", "1", 1_500_000, name = "Jahit seragam")),
            paidNowRupiah = 750_000,
        ))

        val sale = repo.sales(projectId).first().single { it.sale.id == saleId }
        assertEquals(1_500_000L, sale.total)
        assertEquals(750_000L, sale.outstanding)
        assertEquals("Bu Ani", sale.contact?.name)
        assertNull(sale.lines.single().productId)
        assertEquals("Jahit seragam", sale.lines.single().name)
        assertEquals(0L, sale.hpp)
        assertEquals(stockBefore, repo.products(projectId).first().single().stock)
        assertEquals(1_500_000L, repo.monthlySummary(bookId, YearMonth.from(day)).first().salesRupiah)
        assertEquals(750_000L, repo.accountingSummary(bookId).first().cashBalanceRupiah)
        assertEquals(1, repo.exportProjectWorkbookWithNotice(projectId).omittedFreeSaleLineCount)
        assertTrue(repo.exportProjectWorkbook(projectId).isNotEmpty())
    }

    @Test fun businessProfitBasisIsReadByTheSingleMonthlySummary() = runBlocking {
        assertEquals(ProfitBasis.SIMPLE, repo.profitInputs(bookId).first().basis)
        val saleId = repo.saveSale(projectId, null, SaleDraft(
            contactId = buyerId, date = day, dueDate = null, discountRupiah = 0, notes = "",
            lines = listOf(SaleLineDraft(productId, "botol", "1", 55_000)), paidNowRupiah = 55_000,
        ))
        val simple = repo.monthlySummary(bookId, YearMonth.from(day)).first()
        repo.setProfitBasis(businessId, ProfitBasis.ITEM_COST)
        val itemCost = repo.monthlySummary(bookId, YearMonth.from(day)).first()
        assertEquals(ProfitBasis.SIMPLE, simple.profit.basis)
        assertEquals(ProfitBasis.ITEM_COST, itemCost.profit.basis)
        assertEquals(55_000L, simple.profitRupiah)
        assertEquals(50_000L, itemCost.profitRupiah)
        assertEquals(0L, simple.hppRupiah)
        assertEquals(5_000L, itemCost.hppRupiah)
        db.openHelper.writableDatabase.execSQL("UPDATE sale_lines SET costKnown = 0, hppRupiah = 0 WHERE saleId = ?", arrayOf(saleId))
        val unknown = repo.monthlySummary(bookId, YearMonth.from(day)).first()
        assertEquals(1, unknown.salesWithoutCostCount)
        assertEquals(55_000L, unknown.salesWithoutCostRupiah)
    }

    @Test fun freeAndProductLinesShareOneNoteAndOnlyProductCarriesCost() = runBlocking {
        repo.setProfitBasis(businessId, ProfitBasis.ITEM_COST)
        val saleId = repo.saveSale(projectId, null, SaleDraft(
            contactId = buyerId,
            date = day,
            dueDate = null,
            discountRupiah = 0,
            notes = "",
            lines = listOf(
                SaleLineDraft(productId, "botol", "1", 55_000),
                SaleLineDraft(null, "", "1", 20_000, name = "Jasa ganti oli"),
            ),
            paidNowRupiah = 75_000,
        ))

        val sale = repo.sales(projectId).first().single { it.sale.id == saleId }
        assertEquals(75_000L, sale.total)
        assertEquals(5_000L, sale.hpp)
        assertEquals(listOf(5_000L, 0L), sale.lines.map { it.hppRupiah })
        assertEquals(listOf("Oli", "Jasa ganti oli"), sale.lines.map { it.name })
        assertEquals(75_000L, repo.monthlySummary(bookId, YearMonth.from(day)).first().salesRupiah)
        assertEquals(5_000L, repo.monthlySummary(bookId, YearMonth.from(day)).first().hppRupiah)
    }

    @Test fun legacyFreeLineFlaggedKnownStillMarksProfitIncomplete() = runBlocking {
        repo.setProfitBasis(businessId, ProfitBasis.ITEM_COST)
        val id = repo.saveSale(projectId, null, SaleDraft(null, day, null, 0, "", listOf(SaleLineDraft(null, "", "1", 50_000, name = "Jasa lama")), 50_000))
        db.openHelper.writableDatabase.execSQL("UPDATE sale_lines SET costKnown = 1 WHERE saleId = ?", arrayOf(id))
        val month = repo.monthlySummary(bookId, YearMonth.from(day)).first()
        assertEquals(1, month.profit.salesWithoutCostCount)
        assertTrue(month.profit.profitIncomplete)
    }

    @Test fun freeLineNeedsAnExplicitName() = runBlocking {
        val error = runCatching {
            repo.saveSale(projectId, null, SaleDraft(
                contactId = null,
                date = day,
                dueDate = null,
                discountRupiah = 0,
                notes = "",
                lines = listOf(SaleLineDraft(null, "", "1", 10_000)),
                paidNowRupiah = 10_000,
            ))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Nama baris harus diisi.", error?.message)
        assertTrue(repo.sales(projectId).first().isEmpty())
        assertEquals(0L, repo.monthlySummary(bookId, YearMonth.from(day)).first().salesRupiah)
        assertEquals(0L, repo.accountingSummary(bookId).first().cashBalanceRupiah)
    }

    @Test fun editingFreeLinePreservesPaymentAndRejectsTotalBelowPaid() = runBlocking {
        val draft = SaleDraft(buyerId, day, null, 0, "", listOf(SaleLineDraft(null, "", "1", 1_500_000, name = "Jahit seragam")), paidNowRupiah = 750_000)
        val saleId = repo.saveSale(projectId, null, draft)
        val before = repo.sales(projectId).first().single { it.sale.id == saleId }
        repo.saveSale(projectId, saleId, draft.copy(notes = "catatan"))
        val afterNotes = repo.sales(projectId).first().single { it.sale.id == saleId }
        assertEquals(before.lines, afterNotes.lines)
        assertEquals(before.payments, afterNotes.payments)
        val invalid = runCatching { repo.saveSale(projectId, saleId, draft.copy(lines = listOf(SaleLineDraft(null, "", "1", 500_000, name = "Jahit seragam")))) }.exceptionOrNull()
        assertTrue(invalid is IllegalArgumentException)
        assertEquals(afterNotes, repo.sales(projectId).first().single { it.sale.id == saleId })
        repo.saveSale(projectId, saleId, draft.copy(notes = "ubah", lines = listOf(SaleLineDraft(null, "", "2", 1_000_000, name = "Jahit baju"))))
        val after = repo.sales(projectId).first().single { it.sale.id == saleId }
        assertEquals("Jahit baju", after.lines.single().name)
        assertEquals(false, after.lines.single().costKnown)
        assertEquals(2_000_000L, after.total)
        assertEquals(750_000L, after.paid)
        assertEquals(before.payments, after.payments)
        assertEquals("ubah", after.sale.notes)
    }
}
