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
                ProfitExpense(d(3), 4_900_000, includedInHpp = true),
                ProfitExpense(d(4), 300_000, includedInHpp = true), // on credit, same month
                ProfitExpense(d(2), 1_000_000, includedInHpp = false),
                ProfitExpense(d(2), 200_000, includedInHpp = false),
                ProfitExpense(d(2, 11), 777, includedInHpp = false), // November, ignored
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

    private val golden = ProfitInputs(
        sales = listOf(ProfitSale(d(5), 3_700_000, 3_000_000, false), ProfitSale(d(12), 1_680_000, 1_440_000, false)),
        otherIncome = listOf(d(8) to 50_000L),
        expenses = listOf(
            ProfitExpense(d(3), 4_900_000, includedInHpp = true),
            ProfitExpense(d(4), 300_000, includedInHpp = true),
            ProfitExpense(d(2), 1_200_000, includedInHpp = false),
        ),
        assets = listOf(ProfitAsset(d(10, 9), 3_600_000, 36)),
    )

    @Test fun inputsCoverEveryMonthFromTheFirstToTheLastRecord() {
        val months = golden.months(through = oct)
        assertEquals(listOf(YearMonth.of(2026, 9), oct), months.map { it.month })
        assertEquals(-100_000L, months.first().profitRupiah) // the asset's first month of depreciation
        assertEquals(-310_000L, months.last().profitRupiah)
        assertEquals(emptyList<MonthProfit>(), ProfitInputs(emptyList(), emptyList(), emptyList(), emptyList()).months(through = oct))
    }

    /** F2-8: an asset keeps wearing after the last record, so the months run to today when that is later. */
    @Test fun monthsRunThroughTodayWhenItIsLater() {
        val months = golden.months(through = YearMonth.of(2026, 12))
        assertEquals(listOf(9, 10, 11, 12), months.map { it.month.monthValue })
        assertEquals(400_000L, months.sumOf { it.depreciationRupiah })
        assertEquals(listOf(9, 10), golden.months(through = YearMonth.of(2026, 8)).map { it.month.monthValue })
    }

    /** F2-8: only spend in a category counted in HPP is replaced by HPP; transport, even variable, is a cost. */
    @Test fun spendOutsideHppCategoriesReducesProfitEvenWhenVariable() {
        val withTransport = golden.copy(expenses = golden.expenses + ProfitExpense(d(15), 150_000, includedInHpp = false, variable = true))
        val october = withTransport.months(through = oct).last()
        assertEquals(-460_000L, october.profitRupiah)
        assertEquals(150_000L, october.variableCostRupiah)
        assertEquals(5_200_000L, october.productionSpendRupiah)
    }

    /** F2-8: a service business without products: every expense reduces profit, nothing is production spend. */
    @Test fun aServiceBusinessWithoutProductsCountsEveryExpense() {
        val inputs = ProfitInputs(
            sales = emptyList(),
            otherIncome = listOf(d(5) to 2_000_000L),
            expenses = listOf(ProfitExpense(d(6), 500_000, includedInHpp = false, variable = true), ProfitExpense(d(7), 300_000, includedInHpp = false)),
            assets = emptyList(),
        )
        val m = inputs.months(through = oct).single()
        assertEquals(1_200_000L, m.profitRupiah)
        assertEquals(0L, m.productionSpendRupiah)
        assertEquals(0L, m.productionVarianceRupiah)
        assertEquals(800_000L, m.fixedCostRupiah + m.variableCostRupiah)
    }

    /** F2-8: only the default materials category starts counted in HPP. */
    @Test fun onlyBahanBakuStartsCountedInHpp() {
        assertEquals(listOf("Bahan Baku & Stok"), DefaultMasterData.categories.filter { it.includedInHpp }.map { it.name })
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
