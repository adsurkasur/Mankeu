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

    @Test(expected = IllegalArgumentException::class)
    fun `zero rupiah event is rejected`() {
        MoneyEvent("id", "business", "project", FinancialEventType.CASH_EXPENSE, 0, LocalDate.now(), "Invalid", "Test")
    }
}
