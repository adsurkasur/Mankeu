package com.mankeu.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesMathTest {
    private fun line(price: Long, qty: String = "1") = SaleLineInput("p", BigDecimal(qty), price, 0)

    @Test fun noteATotalsFromGolden() {
        val lines = listOf(
            SaleLineInput("dus", BigDecimal("10"), 300_000, 240_000),
            SaleLineInput("bungkus", BigDecimal("100"), 8_000, 6_000),
        )
        assertEquals(3_800_000L, SalesMath.subtotal(lines))
        assertEquals(3_700_000L, SalesMath.total(lines, 100_000))
    }

    @Test fun discountSharedByLineValueRemainderToLast() {
        val lines = listOf(line(1_000), line(2_000), line(3_000))
        val shares = SalesMath.discountShares(lines, 100)
        assertEquals(listOf(17L, 33L, 50L), shares)
        assertEquals(100L, shares.sum())
    }

    @Test fun noShareIsNegativeAndASingleLineTakesItAll() {
        val shares = SalesMath.discountShares(List(4) { line(1) }, 2)
        assertTrue(shares.toString(), shares.all { it >= 0 })
        assertEquals(2L, shares.sum())
        assertEquals(listOf(70L), SalesMath.discountShares(listOf(line(1_000)), 70))
    }

    @Test fun zeroValueLinesShareNothing() {
        assertEquals(listOf(0L, 0L), SalesMath.discountShares(listOf(line(0), line(0)), 0))
    }

    @Test fun discountAboveSubtotalOrNegativeIsRejected() {
        val lines = listOf(line(1_000))
        assertThrows(IllegalArgumentException::class.java) { SalesMath.total(lines, 1_001) }
        assertThrows(IllegalArgumentException::class.java) { SalesMath.total(lines, -1) }
        assertThrows(IllegalArgumentException::class.java) { SalesMath.discountShares(lines, 1_001) }
    }

    @Test fun statusAtTheBoundaries() {
        assertEquals(SaleStatus.UNPAID, SalesMath.status(1_000, 0))
        assertEquals(SaleStatus.PARTIAL, SalesMath.status(1_000, 1))
        assertEquals(SaleStatus.PARTIAL, SalesMath.status(1_000, 999))
        assertEquals(SaleStatus.PAID, SalesMath.status(1_000, 1_000))
        assertEquals(SaleStatus.PAID, SalesMath.status(1_000, 1_500))
        assertEquals(500L, SalesMath.outstanding(1_000, 500))
        assertEquals(0L, SalesMath.outstanding(1_000, 1_500))
    }

    @Test fun overdueNeedsPastDueDateAndOutstanding() {
        val today = LocalDate.of(2026, 10, 29)
        assertTrue(SalesMath.isOverdue(today.minusDays(1), 1, today))
        assertFalse(SalesMath.isOverdue(today.minusDays(1), 0, today))
        assertFalse(SalesMath.isOverdue(today, 1, today))
        assertFalse(SalesMath.isOverdue(null, 1, today))
    }
}
