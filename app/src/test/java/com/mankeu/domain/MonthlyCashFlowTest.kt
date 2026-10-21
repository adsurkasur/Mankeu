package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MonthlyCashFlowTest {
    @Test
    fun `monthly cash flow includes zero months`() {
        val sale = event("sale", FinancialEventType.CASH_INCOME, 1_000_000, LocalDate.of(2026, 1, 5))
        val expense = event("expense", FinancialEventType.CASH_EXPENSE, 200_000, LocalDate.of(2026, 3, 2))

        val months = MonthlyCashFlowCalculator.calculate(listOf(sale, expense), openingCashRupiah = 50_000)

        assertEquals(3, months.size)
        assertEquals(0, months[1].netRupiah)
        assertEquals(-200_000, months[2].netRupiah)
        assertEquals(850_000, months.last().cumulativeRupiah)
    }

    @Test
    fun `deepest cumulative deficit is the peak working capital need`() {
        val months = MonthlyCashFlowCalculator.calculate(
            listOf(
                event("aug", FinancialEventType.CASH_EXPENSE, 13_464_000, LocalDate.of(2026, 8, 1)),
                event("sep", FinancialEventType.CASH_EXPENSE, 1_330_000, LocalDate.of(2026, 9, 1)),
                event("oct", FinancialEventType.CASH_EXPENSE, 840_000, LocalDate.of(2026, 10, 1)),
                event("nov", FinancialEventType.CASH_EXPENSE, 3_235_000, LocalDate.of(2026, 11, 1)),
                event("dec", FinancialEventType.CASH_INCOME, 45_500_000, LocalDate.of(2026, 12, 1)),
                event("dec_exp", FinancialEventType.CASH_EXPENSE, 3_290_000, LocalDate.of(2026, 12, 1)),
            ),
        )
        // November: 13.464.000 + 1.330.000 + 840.000 + 3.235.000.
        assertEquals(-18_869_000L, months.minOf { it.cumulativeRupiah })
    }

    private fun event(id: String, type: FinancialEventType, amount: Long, date: LocalDate) = MoneyEvent(
        id = id,
        businessId = "business",
        projectId = "project",
        bookId = "book",
        type = type,
        amountRupiah = amount,
        date = date,
        description = id,
        category = "test",
    )
}
