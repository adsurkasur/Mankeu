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

    @Test fun `unit cost uses the recipe before purchase or manual cost for every item kind`() {
        for (kind in ItemKind.entries) {
            val result = ProductCosting.unitCost(keripik.copy(kind = kind, manualCostRupiah = 5_000), prices, 13_000)
            assertEquals(UnitCostResult(6_000, true, UnitCostSource.RECIPE), result)
        }
    }

    @Test fun `batch loss reduces net yield and each sale unit rounds only at the end`() {
        val product = keripik.copy(batchLoss = qty("20"))
        val cost = ProductCosting.cost(product, prices)
        assertEquals(2_400_000L, cost.batchCostRupiah)
        assertEquals(6_316L, cost.hppPerBaseUnitRupiah)
        assertEquals(listOf(6_316L, 252_632L), cost.perSaleUnit.map { it.hppRupiah })
        assertEquals(UnitCostResult(6_316, true, UnitCostSource.RECIPE), ProductCosting.unitCost(product, prices))
    }

    @Test fun `traded item uses last purchase cost before manual cost`() {
        val product = keripik.copy(kind = ItemKind.TRADED, recipe = emptyList(), manualCostRupiah = 5_000)
        assertEquals(UnitCostResult(13_000, true, UnitCostSource.LAST_PURCHASE), ProductCosting.unitCost(product, emptyMap(), 13_000))
    }

    @Test fun `manual unit cost is available for every kind without a recipe or eligible purchase`() {
        for (kind in ItemKind.entries) {
            val product = keripik.copy(kind = kind, recipe = emptyList(), manualCostRupiah = 5_000)
            val purchase = if (kind == ItemKind.TRADED) null else 13_000L
            assertEquals(UnitCostResult(5_000, true, UnitCostSource.MANUAL), ProductCosting.unitCost(product, emptyMap(), purchase))
        }
    }

    @Test fun `no recipe purchase or manual cost means unknown rather than a known zero`() {
        for (kind in ItemKind.entries) {
            val product = keripik.copy(kind = kind, recipe = emptyList())
            assertEquals(UnitCostResult(0, false, UnitCostSource.UNKNOWN), ProductCosting.unitCost(product, emptyMap()))
        }
    }

    @Test fun `made and service items do not use the last purchase fallback`() {
        for (kind in listOf(ItemKind.MADE, ItemKind.SERVICE)) {
            val product = keripik.copy(kind = kind, recipe = emptyList())
            assertEquals(UnitCostResult(0, false, UnitCostSource.UNKNOWN), ProductCosting.unitCost(product, emptyMap(), 13_000))
        }
    }

    @Test fun `incomplete recipe keeps its partial cost unknown without falling back`() {
        val product = keripik.copy(kind = ItemKind.TRADED, manualCostRupiah = 5_000)
        val result = ProductCosting.unitCost(product, mapOf("singkong" to 30_000L), 13_000)
        assertEquals(UnitCostResult(4_750, false, UnitCostSource.RECIPE), result)
    }

    @Test fun `entirely unpriced recipe is unknown even when a manual cost exists`() {
        val product = keripik.copy(recipe = listOf(RecipeLine("Missing material", "missing", qty("1"))), manualCostRupiah = 5_000)
        assertEquals(UnitCostResult(0, false, UnitCostSource.RECIPE), ProductCosting.unitCost(product, emptyMap()))
    }

    @Test fun `loss equal to or greater than batch yield is rejected with an explanation`() {
        for (loss in listOf("400", "401")) {
            val product = keripik.copy(batchLoss = qty(loss))
            for (calculate in listOf<() -> Any>(
                { ProductCosting.cost(product, prices) },
                { ProductCosting.unitCost(product, prices) },
            )) {
                val failure = runCatching(calculate).exceptionOrNull()
                assertTrue("Expected a validation error for loss $loss", failure is IllegalArgumentException)
                assertTrue("Expected an explanation for loss $loss", !failure?.message.isNullOrBlank())
            }
        }
    }

    @Test fun `fractional net yield remains exact before per sale unit rounding`() {
        val product = keripik.copy(
            batchYield = qty("3.5"), batchLoss = qty("0.5"),
            recipe = listOf(RecipeLine("Batch cost", amountRupiah = 1_000)),
            saleUnits = listOf(SaleUnit("two", "pair", qty("2"), 2_000)),
        )
        val cost = ProductCosting.cost(product, emptyMap())
        assertEquals(333L, cost.hppPerBaseUnitRupiah)
        assertEquals(667L, cost.perSaleUnit.single().hppRupiah)
        assertEquals(UnitCostResult(333, true, UnitCostSource.RECIPE), ProductCosting.unitCost(product, emptyMap()))
    }

    @Test fun `a positive net yield smaller than one base unit remains valid`() {
        val product = keripik.copy(
            batchYield = qty("0.02"), batchLoss = qty("0.01"),
            recipe = listOf(RecipeLine("Batch cost", amountRupiah = 1)),
        )
        assertEquals(UnitCostResult(100, true, UnitCostSource.RECIPE), ProductCosting.unitCost(product, emptyMap()))
    }

    @Test fun `negative loss and nonpositive batch yield are rejected even without a recipe`() {
        val invalid = listOf(
            keripik.copy(batchLoss = qty("-1")),
            keripik.copy(batchYield = BigDecimal.ZERO),
            keripik.copy(batchYield = qty("-1")),
            keripik.copy(recipe = emptyList(), batchLoss = qty("400"), manualCostRupiah = 5_000),
        )
        for (product in invalid) {
            val failure = runCatching { ProductCosting.unitCost(product, prices) }.exceptionOrNull()
            assertTrue("Expected a validation error for $product", failure is IllegalArgumentException)
            assertTrue("Expected a validation explanation", !failure?.message.isNullOrBlank())
        }
        assertTrue(runCatching { ProductCosting.cost(keripik.copy(batchLoss = qty("-1")), prices) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun `explicit zero manual or purchase cost is known and retains its source`() {
        val product = keripik.copy(kind = ItemKind.TRADED, recipe = emptyList(), manualCostRupiah = 0)
        assertEquals(UnitCostResult(0, true, UnitCostSource.MANUAL), ProductCosting.unitCost(product, emptyMap()))
        assertEquals(UnitCostResult(0, true, UnitCostSource.LAST_PURCHASE), ProductCosting.unitCost(product, emptyMap(), 0))
    }

    @Test fun `negative manual or purchase unit costs are rejected with an explanation`() {
        val product = keripik.copy(kind = ItemKind.TRADED, recipe = emptyList())
        for (calculate in listOf<() -> UnitCostResult>(
            { ProductCosting.unitCost(product.copy(manualCostRupiah = -1), emptyMap()) },
            { ProductCosting.unitCost(product, emptyMap(), -1) },
        )) {
            val failure = runCatching(calculate).exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertTrue(!failure?.message.isNullOrBlank())
        }
    }

    @Test fun `large integer rupiah costs keep exact precision through every source`() {
        val amount = 9_007_199_254_740_993L
        val product = keripik.copy(
            kind = ItemKind.TRADED, batchYield = BigDecimal.ONE,
            recipe = listOf(RecipeLine("Batch cost", amountRupiah = amount)),
            saleUnits = listOf(SaleUnit("one", "one", BigDecimal.ONE, amount)),
        )
        assertEquals(UnitCostResult(amount, true, UnitCostSource.RECIPE), ProductCosting.unitCost(product, emptyMap()))
        assertEquals(amount, ProductCosting.cost(product, emptyMap()).perSaleUnit.single().hppRupiah)
        assertEquals(UnitCostResult(amount, true, UnitCostSource.LAST_PURCHASE), ProductCosting.unitCost(product.copy(recipe = emptyList()), emptyMap(), amount))
        assertEquals(UnitCostResult(amount, true, UnitCostSource.MANUAL), ProductCosting.unitCost(product.copy(recipe = emptyList(), manualCostRupiah = amount), emptyMap()))
    }

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
