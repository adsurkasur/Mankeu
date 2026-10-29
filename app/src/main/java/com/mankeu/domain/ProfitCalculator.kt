package com.mankeu.domain

import java.time.LocalDate
import java.time.YearMonth

data class ProfitSale(val date: LocalDate, val revenueRupiah: Long, val hppRupiah: Long, val hppEstimated: Boolean)
/**
 * A cash or credit purchase. [includedInHpp]: its category is "Biaya produksi (sudah dihitung di HPP)"
 * (F2-8), so HPP of what is sold stands for it. [variable] (effective cost nature) only splits the
 * other expenses for break-even; both kinds reduce profit.
 */
data class ProfitExpense(val date: LocalDate, val amountRupiah: Long, val includedInHpp: Boolean, val variable: Boolean = false)
data class ProfitAsset(val date: LocalDate, val amountRupiah: Long, val usefulLifeMonths: Int)

data class MonthProfit(
    val month: YearMonth,
    val salesRupiah: Long,
    val otherIncomeRupiah: Long,
    val hppRupiah: Long,
    /** Expenses not counted in HPP whose cost nature is not variable. */
    val fixedCostRupiah: Long,
    /** Expenses not counted in HPP but variable (transport, a service's materials): a cost all the same (F2-8). */
    val variableCostRupiah: Long,
    val depreciationRupiah: Long,
    val productionSpendRupiah: Long,
    val hppEstimatedIncluded: Boolean,
) {
    val grossProfitRupiah get() = salesRupiah - hppRupiah
    val profitRupiah get() = salesRupiah + otherIncomeRupiah - hppRupiah - fixedCostRupiah - variableCostRupiah - depreciationRupiah
    /** Positive: more spent in HPP categories than sold at HPP (stock built up or HPP too low). */
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
     * Every month from the first dated row to the later of the last dated row and [through] (normally
     * today's month), in order: an asset keeps wearing after the last record (F2-8 fix round). No rows, no months.
     */
    fun months(through: YearMonth = YearMonth.now()): List<MonthProfit> {
        val dated = sales.map { it.date } + otherIncome.map { it.first } + expenses.map { it.date } + assets.map { it.date }
        if (dated.isEmpty()) return emptyList()
        val first = YearMonth.from(dated.min())
        val last = maxOf(YearMonth.from(dated.max()), through)
        return generateSequence(first) { it.plusMonths(1).takeIf { next -> !next.isAfter(last) } }
            .map { ProfitCalculator.month(it, sales, otherIncome, expenses, assets) }.toList()
    }
}

/** Profit from data, not postings (Fase 2, F2-1 as corrected by F2-8). Payments of notes and debts are neither revenue nor cost. */
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
            fixedCostRupiah = e.filter { !it.includedInHpp && !it.variable }.sumOf { it.amountRupiah },
            variableCostRupiah = e.filter { !it.includedInHpp && it.variable }.sumOf { it.amountRupiah },
            depreciationRupiah = assets.sumOf { depreciation(month, YearMonth.from(it.date), it.amountRupiah, it.usefulLifeMonths) },
            productionSpendRupiah = e.filter { it.includedInHpp }.sumOf { it.amountRupiah },
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
        val expense = hpp + months.sumOf { it.fixedCostRupiah + it.variableCostRupiah } + depreciation
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
