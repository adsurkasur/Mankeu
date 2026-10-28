package com.mankeu.data

import com.mankeu.domain.ActualBepCalculator
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.PlanCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Stored rows → plan/actual inputs → the lecturer's benchmark, end to end without a database. */
class PlanAssemblyTest {
    private val today = LocalDate.of(2026, 11, 15)
    private val kopi = ProductRecord(
        ProductEntity("kopi", "p", "Kopi susu", "cup", targetPerMonthText = "700", createdAtEpochMillis = 0),
        listOf(ProductUnitEntity("kopi-u0", "kopi", "cup", "1", 10_000)),
        listOf(
            ProductRecipeLineEntity("c1", "kopi", "Bubuk kopi/teh + susu", amountRupiah = 3_500, sortOrder = 0),
            ProductRecipeLineEntity("c2", "kopi", "Cup + sedotan + tutup", amountRupiah = 1_000, sortOrder = 1),
            // Schema 12 said "Rp 25.000 covers 50"; migrated it is Rp 500 per cup (DEC-028 §6).
            ProductRecipeLineEntity("c3", "kopi", "Kantong plastik", amountRupiah = 500, sortOrder = 2),
        ),
    )
    private val categories = listOf(
        CategoryEntity("sewa", "b", CategoryRole.EXPENSE.name, "Sewa Tempat", "sewa tempat", createdAtEpochMillis = 0, costNature = "FIXED"),
        CategoryEntity("bahan", "b", CategoryRole.EXPENSE.name, "Bahan Baku & Stok", "bahan baku stok", createdAtEpochMillis = 0, costNature = "VARIABLE"),
        CategoryEntity("alat", "b", CategoryRole.ASSET.name, "Beli Peralatan & Aset", "beli peralatan aset", createdAtEpochMillis = 0),
    )

    private fun line(id: String, category: String, amount: Long, recurrence: String? = null, nature: String? = null, life: Int? = null) =
        BudgetItemEntity(
            id = id, bookId = "plan", categoryId = category, name = id, normalizedName = id, plannedTotalRupiah = amount,
            sortOrder = 0, createdAtEpochMillis = 0, recurrence = recurrence, costNature = nature, usefulLifeMonths = life,
        )

    @Test
    fun `stored plan reproduces the lecturer benchmark`() {
        val inputs = PlanAssembly.planInputs(
            project = null,
            products = listOf(kopi),
            planItems = listOf(
                line("Sewa lapak", "sewa", 1_000_000, "MONTHLY"),
                // A line in a FIXED category can still be marked fixed explicitly; inherits otherwise.
                line("Listrik & air", "sewa", 200_000, "MONTHLY"),
                line("Gaji pembantu", "sewa", 800_000, "MONTHLY", nature = "FIXED"),
            ),
            categories = categories,
            today = today,
            materialPrices = emptyMap(),
        )

        val plan = PlanCalculator.analyse(inputs)

        assertEquals(listOf(YearMonth.of(2026, 11)), inputs.months)
        assertEquals(5_000L, plan.products.single().variableCostPerUnit) // 3.500 + 1.000 + 25.000/50
        assertEquals(BigDecimal("400.000"), plan.breakEvenUnits)
        assertEquals(4_000_000L, plan.breakEvenRupiah)
        assertEquals(BigDecimal("1.27"), plan.revenueCostRatio)
    }

    /** DEC-028 §8: the plan prices a product by its main (first) sale unit, its HPP the one variable cost. */
    @Test
    fun `plan product uses the main sale unit and the HPP of a batch recipe`() {
        val keripik = ProductRecord(
            ProductEntity("k", "p", "Keripik", "bungkus", batchYieldText = "40", targetPerMonthText = "10", createdAtEpochMillis = 0),
            listOf(ProductUnitEntity("k-dus", "k", "dus", "40", 200_000, sortOrder = 0), ProductUnitEntity("k-bks", "k", "bungkus", "1", 6_000, sortOrder = 1)),
            listOf(
                ProductRecipeLineEntity("r1", "k", "Tepung", stockItemId = "tepung", quantityText = "2", sortOrder = 0),
                ProductRecipeLineEntity("r2", "k", "Gas", amountRupiah = 6_000, sortOrder = 1),
            ),
        )
        val noUnit = ProductRecord(ProductEntity("x", "p", "Tanpa satuan", "pcs", createdAtEpochMillis = 0), emptyList(), emptyList())

        val plan = PlanAssembly.products(listOf(keripik, noUnit), mapOf("tepung" to 12_000L)).single()

        assertEquals("dus", plan.unit)
        assertEquals(200_000L, plan.priceRupiah)
        // Batch = 2 × 12.000 + 6.000 = 30.000 for 40 bungkus; one dus holds all 40.
        assertEquals(listOf(com.mankeu.domain.CostComponent("HPP", 30_000, BigDecimal.ONE)), plan.components)
    }

    @Test
    fun `a line override beats its category and an asset without life uses the default`() {
        val inputs = PlanAssembly.planInputs(
            null, emptyList(),
            listOf(line("Upah borongan", "sewa", 300_000, nature = "VARIABLE"), line("Etalase", "alat", 3_600_000)),
            categories, today, emptyMap(),
        )
        assertEquals(com.mankeu.domain.CostNature.VARIABLE, inputs.lines[0].nature)
        assertEquals(36, inputs.lines[1].usefulLifeMonths)
        assertEquals(100_000L, PlanCalculator.analyse(inputs).fixedCost.depreciation.single().second)
    }

    @Test
    fun `the plan period follows the business unit dates`() {
        val project = ProjectEntity(
            "p", "b", "Gerai", isGeneral = false, createdAtEpochMillis = 0,
            startDateEpochDay = LocalDate.of(2026, 2, 1).toEpochDay(), endDateEpochDay = LocalDate.of(2026, 4, 30).toEpochDay(),
        )
        assertEquals(listOf(YearMonth.of(2026, 2), YearMonth.of(2026, 3), YearMonth.of(2026, 4)), PlanAssembly.months(project, emptyList(), today))
    }

    @Test
    fun `actual month splits sales, fixed and variable spend and depreciates assets`() {
        val day = LocalDate.of(2026, 11, 3).toEpochDay()
        val rows = listOf(
            CostRow("s1", "CASH_INCOME", day, 5_000_000, null, "kopi", "500", null),
            CostRow("s2", "CASH_INCOME", day, 2_000_000, null, null, null, null),
            CostRow("e1", "CASH_EXPENSE", day, 2_000_000, "FIXED", null, null, null),
            CostRow("e2", "CASH_EXPENSE", day, 3_500_000, "VARIABLE", null, null, null),
            CostRow("m1", "OWNER_CAPITAL", day, 9_000_000, null, null, null, null),
            CostRow("a1", "ASSET_PURCHASE_CASH", LocalDate.of(2026, 10, 1).toEpochDay(), 12_000_000, null, null, null, 24),
        )

        val actual = PlanAssembly.actualMonth(YearMonth.of(2026, 11), rows)
        val result = ActualBepCalculator.analyse(PlanAssembly.products(listOf(kopi), emptyMap()), actual)

        assertEquals(7_000_000L, actual.revenueRupiah)
        assertEquals(2_000_000L, actual.unattributedRevenueRupiah)
        assertEquals(BigDecimal("500"), actual.unitsSold["kopi"])
        assertEquals(500_000L, actual.depreciationRupiah)
        // 500 named + 200 inferred from Rp2 jt at the planned mix = 700 cups.
        assertEquals(BigDecimal("700.000"), result.unitsSold["kopi"])
        assertEquals(0L, result.variableVarianceRupiah)
        assertEquals(2_500_000L, result.fixedCostRupiah)
    }
}
