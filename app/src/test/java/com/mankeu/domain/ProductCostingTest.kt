package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ProductCostingTest {
    private fun qty(s: String) = BigDecimal(s)

    // Kriteria F1: batch Rp2,4 jt → 400 bungkus, dus isi 40.
    private val keripik = ProductSpec(
        id = "keripik",
        name = "Keripik singkong",
        baseUnit = "bungkus",
        batchYield = qty("400"),
        saleUnits = listOf(
            SaleUnit("u-bks", "bungkus", qty("1"), 8_000),
            SaleUnit("u-dus", "dus", qty("40"), 300_000),
        ),
        recipe = listOf(
            RecipeLine("Singkong", materialId = "singkong", quantity = qty("50")),
            RecipeLine("Minyak", materialId = "minyak", quantity = qty("20")),
            RecipeLine("Bumbu & kemasan", amountRupiah = 300_000),
            RecipeLine("Gas", amountRupiah = 100_000),
        ),
    )
    private val prices = mapOf("singkong" to 30_000L, "minyak" to 25_000L)

    @Test fun `keripik golden - HPP per bungkus and per dus without manual division`() {
        val cost = ProductCosting.cost(keripik, prices)
        assertEquals(2_400_000L, cost.batchCostRupiah)
        assertEquals(6_000L, cost.hppPerBaseUnitRupiah)
        assertEquals(listOf(6_000L, 240_000L), cost.perSaleUnit.map { it.hppRupiah })
        assertEquals(listOf(2_000L, 60_000L), cost.perSaleUnit.map { it.profitRupiah })
        assertTrue(cost.complete)
        assertTrue(cost.hasRecipe)
    }

    @Test fun `hpp rounds once half up, never from a rounded per-unit value`() {
        assertEquals(333L, ProductCosting.hpp(1_000, qty("3"), qty("1")))
        assertEquals(667L, ProductCosting.hpp(1_000, qty("3"), qty("2")))
        assertEquals(3L, ProductCosting.hpp(5, qty("2"), qty("1"))) // 2,5 → 3
    }

    @Test fun `material line costs quantity times price rounded half up, fractional quantities allowed`() {
        assertEquals(37_500L, ProductCosting.lineCost(RecipeLine("Gula", "gula", qty("2.5")), mapOf("gula" to 15_000L)))
        assertEquals(2L, ProductCosting.lineCost(RecipeLine("Garam", "garam", qty("0.3")), mapOf("garam" to 5L))) // 1,5 → 2
        assertEquals(1L, ProductCosting.lineCost(RecipeLine("Garam", "garam", qty("0.25")), mapOf("garam" to 5L))) // 1,25 → 1
    }

    @Test fun `missing or unpriced material makes the HPP incomplete instead of silently cheaper`() {
        val cost = ProductCosting.cost(keripik, mapOf("singkong" to 30_000L))
        assertFalse(cost.complete)
        assertEquals(listOf("Minyak"), cost.unpricedLines)
        assertEquals(1_900_000L, cost.batchCostRupiah)
        val zeroPrice = ProductCosting.cost(keripik, prices + ("minyak" to 0L))
        assertEquals(listOf("Minyak"), zeroPrice.unpricedLines)
    }

    @Test fun `empty recipe is reported, not shown as HPP zero`() {
        val cost = ProductCosting.cost(keripik.copy(recipe = emptyList()), prices)
        assertFalse(cost.hasRecipe)
        assertEquals(0L, cost.batchCostRupiah)
    }

    @Test fun `new oil price shows its impact on every product that uses oil (spec 20)`() {
        val other = keripik.copy(id = "lain", name = "Tanpa minyak", recipe = listOf(RecipeLine("Gas", amountRupiah = 100_000)))
        val changes = ProductCosting.priceImpact(listOf(keripik, other), prices, "minyak", 27_500)
        assertEquals(listOf("keripik"), changes.map { it.product.id })
        val change = changes.single()
        assertEquals(6_000L, change.before.hppPerBaseUnitRupiah)
        assertEquals(2_450_000L, change.after.batchCostRupiah)
        assertEquals(listOf(6_125L, 245_000L), change.after.perSaleUnit.map { it.hppRupiah })
        assertEquals(listOf(1_875L, 55_000L), change.after.perSaleUnit.map { it.profitRupiah })
    }

    @Test fun `same price is no impact`() {
        assertTrue(ProductCosting.priceImpact(listOf(keripik), prices, "minyak", 25_000).isEmpty())
    }

    @Test fun `legacy component Rp X for N units becomes a per-unit cost`() {
        assertEquals(500L, ProductCosting.legacyComponentPerUnit(25_000, qty("50")))
        assertEquals(3_333L, ProductCosting.legacyComponentPerUnit(10_000, qty("3")))
        assertEquals(3_000L, ProductCosting.legacyComponentPerUnit(18_000, qty("6")))
    }

    @Test fun `product stock - production adds, sale subtracts, count sets, and it may go negative`() {
        fun m(type: ProductMovementType, q: String) = ProductMovement(type, qty(q))
        assertEquals(0, qty("330").compareTo(ProductStock.level(listOf(m(ProductMovementType.PRODUCTION, "400"), m(ProductMovementType.SALE, "40"), m(ProductMovementType.SALE, "30")))))
        assertEquals(0, qty("290").compareTo(ProductStock.level(listOf(m(ProductMovementType.SALE, "80"), m(ProductMovementType.ADJUSTMENT, "300"), m(ProductMovementType.SALE, "10")))))
        assertEquals(0, qty("-80").compareTo(ProductStock.level(listOf(m(ProductMovementType.SALE, "80")))))
    }

    @Test fun `stock splits into whole sale units and the rest`() {
        val (whole, rest) = ProductStock.split(qty("410"), qty("40"))
        assertEquals(0, qty("10").compareTo(whole))
        assertEquals(0, qty("10").compareTo(rest))
    }
}
