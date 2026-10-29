package com.mankeu.domain

import java.time.LocalDate
import java.time.YearMonth

data class ProfitSale(val date: LocalDate, val revenueRupiah: Long, val hppRupiah: Long, val hppEstimated: Boolean)
data class ProfitExpense(val date: LocalDate, val amountRupiah: Long, val variable: Boolean)
data class ProfitAsset(val date: LocalDate, val amountRupiah: Long, val usefulLifeMonths: Int)

data class MonthProfit(
    val month: YearMonth,
    val salesRupiah: Long,
    val otherIncomeRupiah: Long,
    val hppRupiah: Long,
    val fixedCostRupiah: Long,
    val depreciationRupiah: Long,
    val productionSpendRupiah: Long,
    val hppEstimatedIncluded: Boolean,
) {
    val grossProfitRupiah get() = salesRupiah - hppRupiah
    val profitRupiah get() = salesRupiah + otherIncomeRupiah - hppRupiah - fixedCostRupiah - depreciationRupiah
    /** Positive: more bought for production than sold at HPP (stock built up or HPP too low). */
    val productionVarianceRupiah get() = productionSpendRupiah - hppRupiah
}

/** Everything profit is computed from, for one Realisasi book. Rows may be pre-summed per month. */
data class ProfitInputs(
    val sales: List<ProfitSale>,
    val otherIncome: List<Pair<LocalDate, Long>>,
    val expenses: List<ProfitExpense>,
    val assets: List<ProfitAsset>,
) {
    /**
     * Every month from the first to the last dated row, in order.
     * ponytail: depreciation stops at the last recorded month, not today, so the figures do not move
     * with the clock; extend to the current month if owners stop recording but still want wear counted.
     */
    fun months(): List<MonthProfit> {
        val dated = sales.map { it.date } + otherIncome.map { it.first } + expenses.map { it.date } + assets.map { it.date }
        if (dated.isEmpty()) return emptyList()
        val first = YearMonth.from(dated.min())
        val last = YearMonth.from(dated.max())
        return generateSequence(first) { it.plusMonths(1).takeIf { next -> !next.isAfter(last) } }
            .map { ProfitCalculator.month(it, sales, otherIncome, expenses, assets) }.toList()
    }
}

/** Profit from data, not postings (Fase 2, F2-1). Payments of notes and debts are neither revenue nor cost. */
object ProfitCalculator {
    /** The one depreciation rule: amount / life (integer division) from the purchase month for [lifeMonths] months. */
    fun depreciation(month: YearMonth, bought: YearMonth, amountRupiah: Long, lifeMonths: Int): Long =
        if (!month.isBefore(bought) && month.isBefore(bought.plusMonths(lifeMonths.toLong()))) amountRupiah / lifeMonths else 0L

    fun month(
        month: YearMonth,
        sales: List<ProfitSale>,
        otherIncome: List<Pair<LocalDate, Long>>,
        expenses: List<ProfitExpense>,
        assets: List<ProfitAsset>,
    ): MonthProfit {
        val s = sales.filter { YearMonth.from(it.date) == month }
        val e = expenses.filter { YearMonth.from(it.date) == month }
        return MonthProfit(
            month = month,
            salesRupiah = s.sumOf { it.revenueRupiah },
            otherIncomeRupiah = otherIncome.filter { YearMonth.from(it.first) == month }.sumOf { it.second },
            hppRupiah = s.sumOf { it.hppRupiah },
            fixedCostRupiah = e.filterNot { it.variable }.sumOf { it.amountRupiah },
            depreciationRupiah = assets.sumOf { depreciation(month, YearMonth.from(it.date), it.amountRupiah, it.usefulLifeMonths) },
            productionSpendRupiah = e.filter { it.variable }.sumOf { it.amountRupiah },
            hppEstimatedIncluded = s.any { it.hppEstimated },
        )
    }

    /**
     * The Realisasi summary: profit lines from [months], cash and payable from the postings in [posted],
     * receivable = [noteTotalRupiah] minus what the postings show paid on notes.
     */
    fun summary(posted: FinancialSummary, months: List<MonthProfit>, noteTotalRupiah: Long): FinancialSummary {
        val hpp = months.sumOf { it.hppRupiah }
        val depreciation = months.sumOf { it.depreciationRupiah }
        val production = months.sumOf { it.productionSpendRupiah }
        val income = months.sumOf { it.salesRupiah + it.otherIncomeRupiah }
        val expense = hpp + months.sumOf { it.fixedCostRupiah } + depreciation
        return posted.copy(
            operatingIncomeRupiah = income,
            operatingExpenseRupiah = expense,
            profitRupiah = income - expense,
            hppRupiah = hpp,
            productionSpendRupiah = production,
            productionVarianceRupiah = production - hpp,
            depreciationRupiah = depreciation,
            receivableRupiah = noteTotalRupiah + posted.receivableRupiah,
        )
    }
}
