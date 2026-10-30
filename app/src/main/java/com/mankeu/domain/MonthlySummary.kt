package com.mankeu.domain

import java.time.YearMonth

/** A posting total and the month of its event; see [MonthlySummary.of]. */
data class MonthPosting(val month: YearMonth, val record: PostedRecord)

/**
 * The one summary of a month (U2, audit P7 "satu angka"). Beranda, Laporan, the BEP card, Komparasi and the
 * PDF/xlsx show these numbers and compute none of their own.
 *
 * Profit lines are [ProfitCalculator.month] ([profit]). Money in/out is this month's operating cash
 * (capital and assets are not "uang masuk/keluar"); cash on hand, receivable and payable are balances at
 * the end of the month.
 */
data class MonthlySummary(
    val profit: MonthProfit,
    val moneyInRupiah: Long,
    val moneyOutRupiah: Long,
    val cashOnHandRupiah: Long,
    /** What customers still owe on notes at month end. */
    val receivableRupiah: Long,
    /** What is still owed to suppliers at month end. */
    val payableRupiah: Long,
) {
    val month: YearMonth get() = profit.month
    /** Note revenue after discount. */
    val salesRupiah get() = profit.salesRupiah
    val otherIncomeRupiah get() = profit.otherIncomeRupiah
    val hppRupiah get() = profit.hppRupiah
    /** Every cost besides HPP: spend outside HPP categories (fixed or variable) and depreciation. */
    val otherCostRupiah get() = profit.fixedCostRupiah + profit.variableCostRupiah + profit.depreciationRupiah
    val profitRupiah get() = profit.profitRupiah
    val productionSpendRupiah get() = profit.productionSpendRupiah
    val productionVarianceRupiah get() = profit.productionVarianceRupiah

    companion object {
        fun of(month: YearMonth, inputs: ProfitInputs, postings: List<MonthPosting>): MonthlySummary {
            val upToEnd = PostingReportCalculator.calculate(postings.filter { !it.month.isAfter(month) }.map { it.record })
            val inMonth = PostingReportCalculator.calculate(postings.filter { it.month == month }.map { it.record })
            val notesToDate = inputs.sales.filter { !YearMonth.from(it.date).isAfter(month) }.sumOf { it.revenueRupiah }
            return MonthlySummary(
                profit = inputs.month(month),
                moneyInRupiah = inMonth.operatingCashInRupiah,
                moneyOutRupiah = inMonth.operatingCashOutRupiah,
                cashOnHandRupiah = upToEnd.cashBalanceRupiah,
                // The postings hold minus what was paid on notes; the notes themselves post nothing (DEC-029 §2).
                receivableRupiah = notesToDate + upToEnd.receivableRupiah,
                payableRupiah = upToEnd.payableRupiah,
            )
        }

        /** Every month from the first record (profit row or posting) through the later of the last one and [through]. */
        fun all(inputs: ProfitInputs, postings: List<MonthPosting>, through: YearMonth): List<MonthlySummary> {
            val months = inputs.months(through).map { it.month } + postings.map { it.month }
            if (months.isEmpty()) return emptyList()
            val last = maxOf(months.max(), through)
            // ponytail: O(months x rows); pre-sum per month if a book ever gets years of monthly rows.
            return generateSequence(months.min()) { it.plusMonths(1).takeIf { next -> !next.isAfter(last) } }
                .map { of(it, inputs, postings) }.toList()
        }
    }
}
