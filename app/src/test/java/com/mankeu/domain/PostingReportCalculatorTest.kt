package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PostingReportCalculatorTest {
    @Test
    fun `reports derive from balanced postings and cash roles`() {
        val events = listOf(
            event("sale", FinancialEventType.CASH_INCOME, 2_000_000),
            event("expense", FinancialEventType.CASH_EXPENSE, 400_000),
            event("capital", FinancialEventType.OWNER_CAPITAL, 1_000_000),
            event("drawing", FinancialEventType.OWNER_DRAWING, 300_000),
            event("fridge", FinancialEventType.ASSET_PURCHASE_CASH, 500_000),
        )
        val records = events.flatMap { event ->
            LedgerPolicy.journalFor(event).postings.map { posting ->
                PostedRecord(event.id, event.cashFlowRole, posting.account, posting.side, posting.amountRupiah)
            }
        }

        val summary = PostingReportCalculator.calculate(records)

        // Capital, drawing and equipment move cash but never profit.
        assertEquals(2_000_000, summary.operatingIncomeRupiah)
        assertEquals(400_000, summary.operatingExpenseRupiah)
        assertEquals(1_600_000, summary.profitRupiah)
        assertEquals(1_600_000, summary.operatingCashFlowRupiah)
        assertEquals(700_000, summary.financingCashFlowRupiah)
        assertEquals(-500_000, summary.investingCashFlowRupiah)
        assertEquals(1_800_000, summary.cashBalanceRupiah)
    }

    private fun event(id: String, type: FinancialEventType, amount: Long) = MoneyEvent(
        id = id,
        businessId = "business",
        projectId = "project",
        bookId = "book",
        type = type,
        amountRupiah = amount,
        date = LocalDate.of(2026, 8, 15),
        description = id,
        category = "test",
    )
}
