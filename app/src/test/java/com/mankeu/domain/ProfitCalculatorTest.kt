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

    private val golden = ProfitInputs(
        sales = listOf(ProfitSale(d(5), 3_700_000, 3_000_000, false), ProfitSale(d(12), 1_680_000, 1_440_000, false)),
        otherIncome = listOf(d(8) to 50_000L),
        expenses = listOf(
            ProfitExpense(d(3), 4_900_000, variable = true),
            ProfitExpense(d(4), 300_000, variable = true),
            ProfitExpense(d(2), 1_200_000, variable = false),
        ),
        assets = listOf(ProfitAsset(d(10, 9), 3_600_000, 36)),
    )

    @Test fun inputsCoverEveryMonthFromTheFirstToTheLastRecord() {
        val months = golden.months()
        assertEquals(listOf(YearMonth.of(2026, 9), oct), months.map { it.month })
        assertEquals(-100_000L, months.first().profitRupiah) // the asset's first month of depreciation
        assertEquals(-310_000L, months.last().profitRupiah)
        assertEquals(emptyList<MonthProfit>(), ProfitInputs(emptyList(), emptyList(), emptyList(), emptyList()).months())
    }

    @Test fun summaryTakesProfitFromDataAndCashFromPostings() {
        val posted = FinancialSummary(
            operatingIncomeRupiah = 999, operatingExpenseRupiah = 999, profitRupiah = 0,
            operatingCashFlowRupiah = 123, financingCashFlowRupiah = 7, cashBalanceRupiah = 130, investingCashFlowRupiah = -3,
            // Postings: Rp2 jt paid on notes (credit Piutang), Rp300.000 still owed to the supplier.
            receivableRupiah = -2_000_000, payableRupiah = 300_000,
        )
        val s = ProfitCalculator.summary(posted, golden.months(), noteTotalRupiah = 5_380_000)
        assertEquals(5_430_000L, s.operatingIncomeRupiah)
        assertEquals(4_440_000L + 1_200_000L + 200_000L, s.operatingExpenseRupiah)
        assertEquals(-410_000L, s.profitRupiah)
        assertEquals(4_440_000L, s.hppRupiah)
        assertEquals(5_200_000L, s.productionSpendRupiah)
        assertEquals(760_000L, s.productionVarianceRupiah)
        assertEquals(200_000L, s.depreciationRupiah)
        assertEquals(3_380_000L, s.receivableRupiah)
        assertEquals(300_000L, s.payableRupiah)
        assertEquals(listOf(123L, 7L, -3L, 130L), listOf(s.operatingCashFlowRupiah, s.financingCashFlowRupiah, s.investingCashFlowRupiah, s.cashBalanceRupiah))
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
