package com.mankeu.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyProductShapeTest {
    @Test fun `old component Rp X for N units becomes a free line of round X over N`() {
        val lines = listOf(
            LegacyProductShape.recipeLine("c1", "p1", "Susu", 18_000, "6", 0),
            LegacyProductShape.recipeLine("c2", "p1", "Plastik", 25_000, "50", 1),
            LegacyProductShape.recipeLine("c3", "p1", "Gula", 10_000, "3", 2),
            LegacyProductShape.recipeLine("c4", "p1", "Gas", 4_000, null, 3),
        )
        assertEquals(listOf(3_000L, 500L, 3_333L, 4_000L), lines.map { it.amountRupiah })
        assertEquals(listOf(null, null, null, null), lines.map { it.stockItemId })
    }

    @Test fun `old unit becomes the first sale unit with content 1`() {
        val unit = LegacyProductShape.saleUnit("p1", "cup", 10_000)
        assertEquals("p1-u0", unit.id)
        assertEquals("1", unit.contentText)
        assertEquals(10_000L, unit.priceRupiah)
        assertEquals(0, unit.sortOrder)
    }
}
