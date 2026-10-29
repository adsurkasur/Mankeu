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
}
