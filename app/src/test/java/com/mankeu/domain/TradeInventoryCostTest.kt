package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class TradeInventoryCostTest {
    private fun q(value: String) = BigDecimal(value)

    @Test fun weightedAverageChangesOnlyWithPurchases() {
        var state = TradeInventoryCost.empty()
        state = state.purchase(q("10"), 100_000)
        state = state.purchase(q("10"), 200_000)
        val sale = state.sell(q("5"))
        assertEquals(75_000L, sale.costRupiah)
        assertTrue(sale.known)
        state = sale.remaining.purchase(q("5"), 100_000)
        assertEquals(16_250L, state.unitCostRupiah())
        assertEquals(32_500L, state.sell(q("2")).costRupiah)
    }

    @Test fun unknownOpeningStockDoesNotPretendToHaveZeroCost() {
        var state = TradeInventoryCost.empty().count(q("3"))
        state = state.purchase(q("2"), 20_000)
        val sale = state.sell(q("1"))
        assertFalse(sale.known)
        assertEquals(null, sale.costRupiah)
    }

    @Test fun zeroCountClearsUnknownCostAndNextPurchaseEstablishesIt() {
        val state = TradeInventoryCost.empty().count(q("3")).count(q("0")).purchase(q("2"), 20_000)
        assertEquals(10_000L, state.unitCostRupiah())
        assertTrue(state.sell(q("1")).known)
    }

    @Test fun fractionalQuantityRoundsOnceForTheWholeSale() {
        val state = TradeInventoryCost.empty().purchase(q("3"), 100)
        assertEquals(67L, state.sell(q("2")).costRupiah)
        assertEquals(33L, state.unitCostRupiah())
    }

    @Test fun repeatingAverageRoundsAnExactHalfUp() {
        val state = TradeInventoryCost.empty().purchase(q("47"), 1)
        assertEquals(1L, state.sell(q("23.5")).costRupiah)
    }

    @Test fun knownZeroCostRemainsKnown() {
        val sale = TradeInventoryCost.empty().purchase(q("2"), 0).sell(q("1"))
        assertTrue(sale.known)
        assertEquals(0L, sale.costRupiah)
    }

    @Test(expected = IllegalArgumentException::class) fun cannotSellMoreThanStock() {
        TradeInventoryCost.empty().purchase(q("1"), 10_000).sell(q("2"))
    }
}
