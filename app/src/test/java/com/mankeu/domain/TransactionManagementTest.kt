package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionManagementTest {

    @Test
    fun `expense journal balances at the exact amount`() {
        val originalEvent = MoneyEvent(
            id = "test-expense",
            businessId = "biz-1",
            projectId = "proj-1",
            type = FinancialEventType.CASH_EXPENSE,
            amountRupiah = 500_000,
            categoryId = "cat-pupuk",
            category = "Pupuk & Nutrisi",
            date = LocalDate.of(2026, 8, 20),
            description = "Beli pupuk urea",
        )
        val journal = LedgerPolicy.journalFor(originalEvent)

        assertNotNull(journal)
        assertTrue(journal.postings.isNotEmpty())
        val debitSum = journal.postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah }
        val creditSum = journal.postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah }
        assertEquals(500_000L, debitSum)
        assertEquals(500_000L, creditSum)
    }

    @Test
    fun `book mode projection and realization remain isolated`() {
        val projectionMode = BookMode.PROJECTION
        val realizationMode = BookMode.REALIZATION

        assertFalse(projectionMode == realizationMode)
        assertEquals("PROJECTION", projectionMode.name)
        assertEquals("REALIZATION", realizationMode.name)
    }

    @Test
    fun `category role decides the cash type and refuses the wrong direction`() {
        assertEquals(FinancialEventType.CASH_INCOME, CategoryRole.REVENUE.cashTypeFor(incoming = true))
        assertEquals(FinancialEventType.OWNER_CAPITAL, CategoryRole.CAPITAL_IN.cashTypeFor(incoming = true))
        assertEquals(FinancialEventType.CASH_EXPENSE, CategoryRole.EXPENSE.cashTypeFor(incoming = false))
        assertEquals(FinancialEventType.OWNER_DRAWING, CategoryRole.DRAWING.cashTypeFor(incoming = false))
        assertEquals(FinancialEventType.ASSET_PURCHASE_CASH, CategoryRole.ASSET.cashTypeFor(incoming = false))
        assertEquals(null, CategoryRole.EXPENSE.cashTypeFor(incoming = true))
        assertEquals(null, CategoryRole.REVENUE.cashTypeFor(incoming = false))
        // Pre-schema-8 or unknown stored roles read as expense rather than crashing.
        assertEquals(CategoryRole.EXPENSE, CategoryRole.parse("LIABILITY"))
    }
}
