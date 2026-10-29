package com.mankeu.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitCalculatorTest {
    private val oct = YearMonth.of(2026, 10)
    private fun d(day: Int, month: Int = 10) = LocalDate.of(2026, month, day)

    @Test fun goldenKeripikOctober() {
        val p = ProfitCalculator.month(
            oct,
            sales = listOf(
                ProfitSale(d(5), 3_700_000, 3_000_000, false), // nota A after discount
                ProfitSale(d(12), 1_680_000, 1_440_000, false), // laku titipan Toko B
                ProfitSale(d(1, 9), 999_999, 999_999, false), // September, ignored
            ),
            otherIncome = listOf(d(8) to 50_000L),
            expenses = listOf(
                ProfitExpense(d(3), 4_900_000, variable = true),
                ProfitExpense(d(4), 300_000, variable = true), // on credit, same month
                ProfitExpense(d(2), 1_000_000, variable = false),
                ProfitExpense(d(2), 200_000, variable = false),
                ProfitExpense(d(2, 11), 777, variable = false), // November, ignored
            ),
            assets = listOf(ProfitAsset(d(10, 9), 3_600_000, 36)),
        )
        assertEquals(5_380_000L, p.salesRupiah)
        assertEquals(50_000L, p.otherIncomeRupiah)
        assertEquals(4_440_000L, p.hppRupiah)
        assertEquals(1_200_000L, p.fixedCostRupiah)
        assertEquals(100_000L, p.depreciationRupiah)
        assertEquals(5_200_000L, p.productionSpendRupiah)
        assertEquals(-310_000L, p.profitRupiah)
        assertEquals(940_000L, p.grossProfitRupiah)
        assertEquals(760_000L, p.productionVarianceRupiah)
        assertFalse(p.hppEstimatedIncluded)
    }

    @Test fun estimatedHppIsFlagged() {
        val p = ProfitCalculator.month(oct, listOf(ProfitSale(d(1), 10, 4, true)), emptyList(), emptyList(), emptyList())
        assertTrue(p.hppEstimatedIncluded)
    }

    @Test fun depreciationRunsFromPurchaseMonthForLifeMonthsOnly() {
        val asset = ProfitAsset(d(20, 9), 3_600_001, 36)
        fun dep(m: YearMonth) = ProfitCalculator.month(m, emptyList(), emptyList(), emptyList(), listOf(asset)).depreciationRupiah
        assertEquals(0L, dep(YearMonth.of(2026, 8)))
        assertEquals(100_000L, dep(YearMonth.of(2026, 9))) // integer division
        assertEquals(100_000L, dep(YearMonth.of(2029, 8)))
        assertEquals(0L, dep(YearMonth.of(2029, 9)))
    }
}
