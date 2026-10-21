package com.mankeu.domain

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyCashFlow(
    val month: YearMonth,
    val operatingRupiah: Long,
    val investingRupiah: Long,
    val financingRupiah: Long,
    val netRupiah: Long,
    val cumulativeRupiah: Long,
)

/**
 * The five facts a cash-flow schedule needs from an event.
 *
 * Separate from [MoneyEvent] so the calculation can be fed straight from a narrow database
 * projection: a book with thousands of transactions should not have to materialise every
 * description, category and counterparty to add up six figures a month. Mirrors what
 * `PostedRecord` already does for [PostingReportCalculator].
 */
data class CashFlowRecord(
    val id: String,
    val date: LocalDate,
    val type: FinancialEventType,
    val cashFlowRole: CashFlowRole,
    val amountRupiah: Long,
)

object MonthlyCashFlowCalculator {
    fun calculate(events: List<MoneyEvent>, openingCashRupiah: Long = 0): List<MonthlyCashFlow> =
        calculateFromRecords(
            events.map {
                CashFlowRecord(it.id, it.date, it.type, it.cashFlowRole, it.amountRupiah)
            },
            openingCashRupiah,
        )

    fun calculateFromRecords(events: List<CashFlowRecord>, openingCashRupiah: Long = 0): List<MonthlyCashFlow> {
        if (events.isEmpty()) return emptyList()
        val movements = events.mapNotNull { event ->
            cashMovement(event).takeIf { it != 0L }?.let { Triple(YearMonth.from(event.date), event.cashFlowRole, it) }
        }
        if (movements.isEmpty()) return emptyList()
        val first = movements.minOf { it.first }
        val last = movements.maxOf { it.first }
        val grouped = movements.groupBy { it.first }
        var cumulative = openingCashRupiah
        return generateSequence(first) { month -> month.plusMonths(1).takeIf { it <= last } }.map { month ->
            val values = grouped[month].orEmpty()
            fun role(role: CashFlowRole) = values.filter { it.second == role }.sumOf { it.third }
            val operating = role(CashFlowRole.OPERATING)
            val investing = role(CashFlowRole.INVESTING)
            val financing = role(CashFlowRole.FINANCING)
            val net = operating + investing + financing
            cumulative += net
            MonthlyCashFlow(month, operating, investing, financing, net, cumulative)
        }.toList()
    }

    private fun cashMovement(event: CashFlowRecord): Long = when (event.type) {
        FinancialEventType.CASH_INCOME, FinancialEventType.OWNER_CAPITAL -> event.amountRupiah
        FinancialEventType.CASH_EXPENSE, FinancialEventType.OWNER_DRAWING, FinancialEventType.ASSET_PURCHASE_CASH -> -event.amountRupiah
    }
}
