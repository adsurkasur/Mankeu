package com.mankeu.domain

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

/** U2 (P7 "satu angka"): one monthly summary, built from ProfitCalculator and the postings. */
class MonthlySummaryTest {
    private val sep = YearMonth.of(2026, 9)
    private val oct = YearMonth.of(2026, 10)
    private fun d(day: Int, month: Int = 10) = LocalDate.of(2026, month, day)

    /** The phase 2 golden (SalesFlowTest): keripik, October 2026. */
    private val inputs = ProfitInputs(
        sales = listOf(ProfitSale(d(5), 3_700_000, 3_000_000, false), ProfitSale(d(12), 1_680_000, 1_440_000, false)),
        otherIncome = listOf(d(8) to 50_000L),
        expenses = listOf(
            ProfitExpense(d(3), 4_900_000, includedInHpp = true),
            ProfitExpense(d(4), 300_000, includedInHpp = true), // on credit
            ProfitExpense(d(2), 1_200_000, includedInHpp = false),
        ),
        assets = listOf(ProfitAsset(d(10, 9), 3_600_000, 36)),
    )

    private fun p(month: YearMonth, role: CashFlowRole, account: LedgerAccount, side: PostingSide, amount: Long) =
        MonthPosting(month, PostedRecord("", role, account, side, amount))

    private val postings = listOf(
        // September: capital in, asset bought for cash.
        p(sep, CashFlowRole.FINANCING, LedgerAccount.CASH, PostingSide.DEBIT, 10_000_000),
        p(sep, CashFlowRole.FINANCING, LedgerAccount.OWNER_EQUITY, PostingSide.CREDIT, 10_000_000),
        p(sep, CashFlowRole.INVESTING, LedgerAccount.EQUIPMENT_ASSET, PostingSide.DEBIT, 3_600_000),
        p(sep, CashFlowRole.INVESTING, LedgerAccount.CASH, PostingSide.CREDIT, 3_600_000),
        // October: 2 jt paid on note A, other income 50.000.
        p(oct, CashFlowRole.OPERATING, LedgerAccount.CASH, PostingSide.DEBIT, 2_050_000),
        p(oct, CashFlowRole.OPERATING, LedgerAccount.RECEIVABLE, PostingSide.CREDIT, 2_000_000),
        p(oct, CashFlowRole.OPERATING, LedgerAccount.REVENUE, PostingSide.CREDIT, 50_000),
        // Cash spend 6,1 jt, the 300.000 credit purchase, 100.000 of it paid.
        p(oct, CashFlowRole.OPERATING, LedgerAccount.OPERATING_EXPENSE, PostingSide.DEBIT, 6_400_000),
        p(oct, CashFlowRole.OPERATING, LedgerAccount.CASH, PostingSide.CREDIT, 6_200_000),
        p(oct, CashFlowRole.OPERATING, LedgerAccount.PAYABLE, PostingSide.CREDIT, 300_000),
        p(oct, CashFlowRole.OPERATING, LedgerAccount.PAYABLE, PostingSide.DEBIT, 100_000),
    )

    @Test fun goldenKeripikOctober() {
        val m = MonthlySummary.of(oct, inputs, postings)
        assertEquals(oct, m.month)
        assertEquals(5_380_000L, m.salesRupiah)
        assertEquals(50_000L, m.otherIncomeRupiah)
        assertEquals(5_430_000L, m.incomeRupiah) // "Pendapatan" on every screen and in the export
        assertEquals(4_440_000L, m.hppRupiah)
        assertEquals(1_200_000L + 100_000L, m.otherCostRupiah) // fixed + depreciation
        assertEquals(-310_000L, m.profitRupiah)
        assertEquals(m.salesRupiah + m.otherIncomeRupiah - m.hppRupiah - m.otherCostRupiah, m.profitRupiah)
        assertEquals(5_200_000L, m.productionSpendRupiah)
        assertEquals(760_000L, m.productionVarianceRupiah)
        assertEquals(2_050_000L to 6_200_000L, m.moneyInRupiah to m.moneyOutRupiah)
        assertEquals(10_000_000L - 3_600_000L + 2_050_000L - 6_200_000L, m.cashOnHandRupiah)
        assertEquals(5_380_000L - 2_000_000L, m.receivableRupiah)
        assertEquals(200_000L, m.payableRupiah)
        // The profit is ProfitCalculator's, not a second formula.
        assertEquals(inputs.month(oct).profitRupiah, m.profitRupiah)
    }

    @Test fun balancesAreAtMonthEndAndCapitalIsNotMoneyIn() {
        val september = MonthlySummary.of(sep, inputs, postings)
        assertEquals(-100_000L, september.profitRupiah) // the asset's first month
        assertEquals(0L to 0L, september.moneyInRupiah to september.moneyOutRupiah)
        assertEquals(6_400_000L, september.cashOnHandRupiah)
        assertEquals(0L to 0L, september.receivableRupiah to september.payableRupiah)
        // A month with no records carries the balances and keeps depreciating.
        val november = MonthlySummary.of(YearMonth.of(2026, 11), inputs, postings)
        assertEquals(-100_000L, november.profitRupiah)
        assertEquals(0L, november.moneyInRupiah)
        assertEquals(2_250_000L to 3_380_000L, november.cashOnHandRupiah to november.receivableRupiah)
    }

    @Test fun allRunsFromTheFirstRecordThroughTheCurrentMonth() {
        val all = MonthlySummary.all(inputs, postings, through = YearMonth.of(2026, 11))
        assertEquals(listOf(9, 10, 11), all.map { it.month.monthValue })
        assertEquals(MonthlySummary.of(oct, inputs, postings), all[1])
        // A capital-only month before any profit row still starts the list.
        val capitalFirst = postings + p(YearMonth.of(2026, 8), CashFlowRole.FINANCING, LedgerAccount.CASH, PostingSide.DEBIT, 1)
        assertEquals(8, MonthlySummary.all(inputs, capitalFirst, through = oct).first().month.monthValue)
        assertEquals(emptyList<MonthlySummary>(), MonthlySummary.all(ProfitInputs(emptyList(), emptyList(), emptyList(), emptyList()), emptyList(), oct))
    }
}
