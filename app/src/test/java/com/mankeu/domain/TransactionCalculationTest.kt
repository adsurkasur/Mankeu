package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TransactionCalculationTest {

    @Test
    fun `parseQuantity correctly parses standard and Indonesian decimal formats`() {
        assertNull(TransactionCalculation.parseQuantity(null))
        assertNull(TransactionCalculation.parseQuantity(""))
        assertNull(TransactionCalculation.parseQuantity("   "))
        assertNull(TransactionCalculation.parseQuantity("0"))
        assertNull(TransactionCalculation.parseQuantity("-5"))
        assertNull(TransactionCalculation.parseQuantity("abc"))
        assertNull(TransactionCalculation.parseQuantity("1.2.3"))

        assertEquals(BigDecimal("10"), TransactionCalculation.parseQuantity("10"))
        assertEquals(BigDecimal("2.5"), TransactionCalculation.parseQuantity("2,5"))
        assertEquals(BigDecimal("2.5"), TransactionCalculation.parseQuantity("2.5"))
        assertEquals(BigDecimal("0.75"), TransactionCalculation.parseQuantity("0,75"))
        assertEquals(BigDecimal("100.5"), TransactionCalculation.parseQuantity("  100,5  "))
    }

    @Test
    fun `computeTotalRupiah calculates exact integer rupiah with half-up rounding`() {
        // 1. Integer volume and price: 10 * 15,000 = 150,000
        assertEquals(
            150_000L,
            TransactionCalculation.computeTotalRupiah(BigDecimal("10"), 15_000L),
        )

        // 2. Decimal volume: 2.5 * 20,000 = 50,000
        assertEquals(
            50_000L,
            TransactionCalculation.computeTotalRupiah(BigDecimal("2.5"), 20_000L),
        )

        // 3. Fractional volume with half-up rounding: 0.333 * 10,000 = 3,330
        assertEquals(
            3_330L,
            TransactionCalculation.computeTotalRupiah(BigDecimal("0.333"), 10_000L),
        )

        // 4. Zero / negative safety
        assertEquals(0L, TransactionCalculation.computeTotalRupiah(BigDecimal.ZERO, 15_000L))
        assertEquals(0L, TransactionCalculation.computeTotalRupiah(BigDecimal("10"), 0L))
        assertEquals(0L, TransactionCalculation.computeTotalRupiah(BigDecimal("10"), -5_000L))
    }

    @Test
    fun `computeUnitPriceRupiah derives unit price accurately from total and volume`() {
        // 150,000 / 10 = 15,000
        assertEquals(
            15_000L,
            TransactionCalculation.computeUnitPriceRupiah(150_000L, BigDecimal("10")),
        )

        // 50,000 / 2.5 = 20,000
        assertEquals(
            20_000L,
            TransactionCalculation.computeUnitPriceRupiah(50_000L, BigDecimal("2.5")),
        )

        // 50,000 / 5 = 10,000
        assertEquals(
            10_000L,
            TransactionCalculation.computeUnitPriceRupiah(50_000L, BigDecimal("5")),
        )

        // Safety on zero
        assertEquals(0L, TransactionCalculation.computeUnitPriceRupiah(0L, BigDecimal("10")))
        assertEquals(0L, TransactionCalculation.computeUnitPriceRupiah(100_000L, BigDecimal.ZERO))
    }

    @Test
    fun `isConsistent correctly verifies mathematical integrity`() {
        // Consistent
        assertTrue(TransactionCalculation.isConsistent(150_000L, "10", 15_000L))
        assertTrue(TransactionCalculation.isConsistent(50_000L, "2,5", 20_000L))

        // Inconsistent (10 kg * Rp5.000 != Rp100.000)
        assertFalse(TransactionCalculation.isConsistent(100_000L, "10", 5_000L))

        // Lump-sum without quantity or unit price is consistently valid
        assertTrue(TransactionCalculation.isConsistent(100_000L, null, null))
        assertTrue(TransactionCalculation.isConsistent(100_000L, "", null))
        assertTrue(TransactionCalculation.isConsistent(100_000L, "10", null))
    }

    @Test
    fun `reconcile produces coherent non-contradictory database triplets`() {
        // 1. Direct lump-sum input without quantity details
        val lumpSum = TransactionCalculation.reconcile(100_000L, null, null)
        assertEquals(100_000L, lumpSum.totalRupiah)
        assertNull(lumpSum.quantityText)
        assertNull(lumpSum.unitPriceRupiah)

        // 2. Fully consistent input
        val consistent = TransactionCalculation.reconcile(150_000L, "10", 15_000L)
        assertEquals(150_000L, consistent.totalRupiah)
        assertEquals("10", consistent.quantityText)
        assertEquals(15_000L, consistent.unitPriceRupiah)

        // 3. User entered total & volume, omitting unit price -> auto-derives unit price
        val autoDerivedPrice = TransactionCalculation.reconcile(50_000L, "5", null)
        assertEquals(50_000L, autoDerivedPrice.totalRupiah)
        assertEquals("5", autoDerivedPrice.quantityText)
        assertEquals(10_000L, autoDerivedPrice.unitPriceRupiah)

        // 4. User manually changed total to 180,000 while volume was 10 -> reconciles unit price to 18,000
        val reconciledPrice = TransactionCalculation.reconcile(180_000L, "10", 15_000L)
        assertEquals(180_000L, reconciledPrice.totalRupiah)
        assertEquals("10", reconciledPrice.quantityText)
        assertEquals(18_000L, reconciledPrice.unitPriceRupiah)

        // 5. Total was 0 but volume and price provided -> calculates formula total
        val formulaResult = TransactionCalculation.reconcile(0L, "2,5", 20_000L)
        assertEquals(50_000L, formulaResult.totalRupiah)
        assertEquals("2,5", formulaResult.quantityText)
        assertEquals(20_000L, formulaResult.unitPriceRupiah)
    }

    @Test
    fun `a derived purchase price that rounds to zero is no price`() {
        assertEquals(27_500L, TransactionCalculation.derivedUnitPriceRupiah(55_000, BigDecimal("2")))
        assertNull(TransactionCalculation.derivedUnitPriceRupiah(1, BigDecimal("3")))
    }
}
