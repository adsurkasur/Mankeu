package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ExactRecipeCostFlowTest {
    @Test fun fractionalRecipeIsRoundedOnlyAfterSaleQuantity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        try {
            val repo = FinanceRepository(db)
            val (business, project) = repo.createBusinessWithUnit("Usaha uji", "Utama")
            val material = repo.createStockItem(business, project, false, "Bahan uji", "kg", 5, BigDecimal.ZERO, BigDecimal.ZERO)
            val product = repo.saveProduct(project, null, ProductDraft("Barang uji", "pcs", "2",
                listOf(SaleUnitDraft(name = "pcs", contentText = "1", priceRupiah = 10)), listOf(RecipeLineDraft("Bahan uji", material, "0.3"))))
            val sale = repo.saveSale(project, null, SaleDraft(null, LocalDate.of(2026, 11, 12), null, 0, "",
                listOf(SaleLineDraft(product, "pcs", "3", 10)), 30))
            // 0.3 kg × Rp5 × 3 pcs / 2 pcs = Rp2.25, rounded once to Rp2.
            assertEquals(2L, repo.sales(project).first().single { it.sale.id == sale }.lines.single().hppRupiah)
        } finally { db.close() }
    }
}
