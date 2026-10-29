package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LedgerPolicyTest {
    @Test
    fun `every supported money event produces an exact balanced journal`() {
        FinancialEventType.entries.forEach { type ->
            val event = MoneyEvent(
                id = type.name,
                businessId = "business",
                projectId = "project",
                type = type,
                amountRupiah = 7_000_001L,
                date = LocalDate.of(2026, 8, 15),
                description = "Test ${type.name}",
                category = "Test",
            )
            val journal = LedgerPolicy.journalFor(event)
            assertEquals(7_000_001L, journal.postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah })
            assertEquals(7_000_001L, journal.postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah })
        }
    }

    private fun event(type: FinancialEventType, saleId: String? = null, settlesEventId: String? = null) = MoneyEvent(
        id = "e", businessId = "b", projectId = "p", type = type, amountRupiah = 150_000, date = LocalDate.of(2026, 10, 1),
        description = "x", category = "c", saleId = saleId, settlesEventId = settlesEventId,
    )

    private fun accounts(event: MoneyEvent) = LedgerPolicy.journalFor(event).postings.map { Triple(it.account, it.side, it.amountRupiah) }

    @Test
    fun `a sale note payment is Kas against Piutang, not revenue (DEC-029 §2)`() {
        assertEquals(
            listOf(Triple(LedgerAccount.CASH, PostingSide.DEBIT, 150_000L), Triple(LedgerAccount.RECEIVABLE, PostingSide.CREDIT, 150_000L)),
            accounts(event(FinancialEventType.CASH_INCOME, saleId = "s")),
        )
        assertEquals(LedgerAccount.REVENUE, accounts(event(FinancialEventType.CASH_INCOME))[1].first)
    }

    @Test
    fun `a credit purchase is Beban against Utang and touches no cash (DEC-029 §3)`() {
        val postings = accounts(event(FinancialEventType.CREDIT_PURCHASE))
        assertEquals(
            listOf(Triple(LedgerAccount.OPERATING_EXPENSE, PostingSide.DEBIT, 150_000L), Triple(LedgerAccount.PAYABLE, PostingSide.CREDIT, 150_000L)),
            postings,
        )
        assertEquals(CashFlowRole.OPERATING, event(FinancialEventType.CREDIT_PURCHASE).cashFlowRole)
    }

    @Test
    fun `paying a credit purchase is Utang against Kas (DEC-029 §3)`() {
        assertEquals(
            listOf(Triple(LedgerAccount.PAYABLE, PostingSide.DEBIT, 150_000L), Triple(LedgerAccount.CASH, PostingSide.CREDIT, 150_000L)),
            accounts(event(FinancialEventType.CASH_EXPENSE, settlesEventId = "cp")),
        )
        assertEquals(LedgerAccount.OPERATING_EXPENSE, accounts(event(FinancialEventType.CASH_EXPENSE))[0].first)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero rupiah event is rejected`() {
        MoneyEvent("id", "business", "project", FinancialEventType.CASH_EXPENSE, 0, LocalDate.now(), "Invalid", "Test")
    }
}
