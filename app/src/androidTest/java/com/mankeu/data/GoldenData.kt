package com.mankeu.data

import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.domain.FinancialEventType
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate

/** Seeded data shared by the U2 "satu angka" tests; every row goes through the repository. */
object GoldenData {
    data class Ids(val businessId: String, val projectId: String, val bookId: String, val keripik: String)

    private suspend fun unit(repo: FinanceRepository, name: String, recipeRupiah: Long): Ids {
        val (b, p) = repo.createBusinessWithUnit(name, "Umum")
        val book = repo.books(p).first().single { it.mode == BookMode.REALIZATION.name }.id
        // HPP Rp6.000 a bungkus at Rp2,4 jt per batch of 400; a dus holds 40.
        val keripik = repo.saveProduct(p, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "400",
            units = listOf(SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000), SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000)),
            recipe = listOf(RecipeLineDraft("Bumbu & bahan", amountRupiah = recipeRupiah)),
        ))
        repo.recordProduction(keripik, BigDecimal(2), LocalDate.of(2026, 10, 1), "")
        return Ids(b, p, book, keripik)
    }

    private suspend fun event(repo: FinanceRepository, ids: Ids, type: FinancialEventType, amount: Long, date: LocalDate, category: CategoryEntity, life: Int? = null, contact: String? = null) =
        repo.recordEvent(EventDraft(ids.businessId, ids.projectId, ids.bookId, type, amount, date, category.name, category.name, categoryId = category.id, usefulLifeMonths = life, contactId = contact))

    /**
     * The phase 2 golden (SalesFlowTest.goldenProfitWhenSoldSummary). October 2026: untung −310.000,
     * HPP 4.440.000, belanja produksi 5.200.000, selisih +760.000, money in 2.050.000 / out 6.200.000,
     * cash 2.250.000, receivable 3.380.000, payable 200.000.
     */
    suspend fun keripikOctober(repo: FinanceRepository, db: AppDatabase): Ids {
        val ids = unit(repo, "Keripik Golden", 2_400_000)
        val categories = db.financeDao().getCategories(ids.businessId)
        fun role(role: CategoryRole) = categories.first { it.role == role.name }
        val kemasanId = repo.createCategory(ids.businessId, "Kemasan", CategoryRole.EXPENSE, costNature = CostNature.VARIABLE)
        repo.setCategoryIncludedInHpp(kemasanId, true)
        val kemasan = db.financeDao().getCategory(kemasanId)!!
        val toko = repo.saveContact(ids.businessId, null, ContactDraft("Toko B"))
        val pemasok = repo.saveContact(ids.businessId, null, ContactDraft("Pemasok", isCustomer = false, isSupplier = true))
        val day = LocalDate.of(2026, 10, 5)

        event(repo, ids, FinancialEventType.OWNER_CAPITAL, 10_000_000, LocalDate.of(2026, 9, 1), role(CategoryRole.CAPITAL_IN))
        event(repo, ids, FinancialEventType.ASSET_PURCHASE_CASH, 3_600_000, LocalDate.of(2026, 9, 10), role(CategoryRole.ASSET), life = 36)
        val notaA = repo.saveSale(ids.projectId, null, SaleDraft(toko, day, null, 100_000, "", listOf(SaleLineDraft(ids.keripik, "bungkus", "500", 7_600)), 0))
        repo.recordSalePayment(notaA, 2_000_000, day.plusDays(1))
        val titip = repo.sendConsignment(ids.projectId, toko, day, listOf(ConsignmentLineDraft(ids.keripik, "dus", "6", 280_000)))
        val line = repo.consignments(ids.projectId).first().single().lines.single().line.id
        repo.reportConsignmentSold(titip, listOf(line to "6"), LocalDate.of(2026, 10, 12), 0)
        event(repo, ids, FinancialEventType.CASH_INCOME, 50_000, LocalDate.of(2026, 10, 8), role(CategoryRole.REVENUE))
        event(repo, ids, FinancialEventType.CASH_EXPENSE, 4_900_000, LocalDate.of(2026, 10, 3), categories.single { it.name == "Bahan Baku & Stok" })
        val tempo = event(repo, ids, FinancialEventType.CREDIT_PURCHASE, 300_000, LocalDate.of(2026, 10, 4), kemasan, contact = pemasok)
        repo.recordSupplierPayment(tempo, 100_000, LocalDate.of(2026, 10, 20))
        event(repo, ids, FinancialEventType.CASH_EXPENSE, 1_000_000, LocalDate.of(2026, 10, 2), categories.single { it.name == "Sewa Tempat" })
        event(repo, ids, FinancialEventType.CASH_EXPENSE, 200_000, LocalDate.of(2026, 10, 2), categories.single { it.name == "Listrik, Air & Internet" })
        return ids
    }

    /**
     * The audit's Keripik Bu Sri mismatch (P7, L19, L27, L28), October 2026: modal 500.000, a note of
     * 2 dus (Rp600.000, HPP 480.000) with 1 dus paid, minyak 260.000 in cash, other income 50.000. Then
     * the recipe gets dearer (HPP Rp6.050 a bungkus), which made the BEP card say Rp224.000 while the
     * other screens said Rp220.000. One value each: untung 170.000, selisih −220.000.
     */
    suspend fun buSri(repo: FinanceRepository, db: AppDatabase): Ids {
        val ids = unit(repo, "Keripik Bu Sri", 2_400_000)
        val categories = db.financeDao().getCategories(ids.businessId)
        val toko = repo.saveContact(ids.businessId, null, ContactDraft("Toko A"))
        event(repo, ids, FinancialEventType.OWNER_CAPITAL, 500_000, LocalDate.of(2026, 10, 1), categories.first { it.role == CategoryRole.CAPITAL_IN.name })
        repo.saveSale(ids.projectId, null, SaleDraft(toko, LocalDate.of(2026, 10, 5), null, 0, "", listOf(SaleLineDraft(ids.keripik, "dus", "2", 300_000)), paidNowRupiah = 300_000))
        event(repo, ids, FinancialEventType.CASH_EXPENSE, 260_000, LocalDate.of(2026, 10, 6), categories.single { it.name == "Bahan Baku & Stok" })
        event(repo, ids, FinancialEventType.CASH_INCOME, 50_000, LocalDate.of(2026, 10, 7), categories.first { it.role == CategoryRole.REVENUE.name })
        val product = repo.products(ids.projectId).first().single { it.product.id == ids.keripik }
        repo.saveProduct(ids.projectId, ids.keripik, ProductDraft(
            product.product.name, product.product.unit, product.product.batchYieldText,
            product.units.sortedBy { it.sortOrder }.map { SaleUnitDraft(it.id, it.name, it.contentText, it.priceRupiah) },
            listOf(RecipeLineDraft("Bumbu & bahan", amountRupiah = 2_420_000)),
        ))
        return ids
    }
}
