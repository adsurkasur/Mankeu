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
        // Komparasi's cash basis: operating money in and out (Task 4 review I-1).
        assertEquals(2_000_000L to 400_000L, summary.operatingCashInRupiah to summary.operatingCashOutRupiah)
    }

    @Test
    fun `receivable and payable balances come from their accounts`() {
        fun r(account: LedgerAccount, side: PostingSide, amount: Long) = PostedRecord("", CashFlowRole.OPERATING, account, side, amount)
        val summary = PostingReportCalculator.calculate(
            listOf(
                r(LedgerAccount.CASH, PostingSide.DEBIT, 2_000_000), r(LedgerAccount.RECEIVABLE, PostingSide.CREDIT, 2_000_000), // note payment
                r(LedgerAccount.OPERATING_EXPENSE, PostingSide.DEBIT, 500_000), r(LedgerAccount.PAYABLE, PostingSide.CREDIT, 500_000), // credit purchase
                r(LedgerAccount.PAYABLE, PostingSide.DEBIT, 200_000), r(LedgerAccount.CASH, PostingSide.CREDIT, 200_000), // paid in part
            ),
        )
        // Notes post no revenue, so the receivable here is minus what was paid; the repository adds note totals.
        assertEquals(-2_000_000L, summary.receivableRupiah)
        assertEquals(300_000L, summary.payableRupiah)
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
