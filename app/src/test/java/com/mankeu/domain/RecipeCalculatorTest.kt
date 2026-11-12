package com.mankeu.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class RecipeCalculatorTest {
    private fun d(s: String) = BigDecimal(s)
    private val spec = ProductSpec("p", "Keripik", "bungkus", d("400"), listOf(
        SaleUnit("a", "bungkus", d("1"), 7800), SaleUnit("b", "dus", d("40"), 312000)),
        listOf(RecipeLine("Singkong", "s", d("50")), RecipeLine("Minyak", "m", d("20")),
            RecipeLine("Bumbu", amountRupiah = 300000), RecipeLine("Gas", amountRupiah = 100000)))
    private val prices = mapOf("s" to 30000L, "m" to 25000L)

    @Test fun goldenRecipeAndLoss() {
        val base = ProductCosting.cost(spec, prices)
        assertEquals(2400000L, base.batchCostRupiah)
        assertEquals(listOf(6000L, 240000L), base.perSaleUnit.map { it.hppRupiah })
        val loss = ProductCosting.cost(spec.copy(batchLoss = d("20")), prices)
        assertEquals(listOf(6316L, 252632L), loss.perSaleUnit.map { it.hppRupiah })
        assertThrows(IllegalArgumentException::class.java) { ProductCosting.cost(spec.copy(batchLoss = d("400")), prices) }
    }

    @Test fun packagePriceAndMarkup() {
        assertEquals(3733L, RecipeCalculator.ingredientCost(28000, d("30"), d("4")))
        assertEquals(100L, RecipeCalculator.ingredientCost(5000, d("500"), d("10")))
        assertEquals(7800L, RecipeCalculator.suggestedPrice(6000, d("30")))
        assertEquals(312000L, RecipeCalculator.suggestedPrice(240000, d("30")))
        assertEquals(8300L, RecipeCalculator.suggestedPrice(6316, d("30")))
    }

    @Test fun priceChangesApplyOnlyToSelectedOrAllCosts() {
        val oil = RecipeCalculator.whatIf(spec, prices, setOf("m"), d("10"))
        assertEquals(2400000L, oil.before.batchCostRupiah)
        assertEquals(2450000L, oil.after.batchCostRupiah)
        assertEquals(listOf(6125L, 245000L), oil.after.perSaleUnit.map { it.hppRupiah })
        assertEquals(1675L, oil.after.perSaleUnit.first().profitRupiah)
        val all = RecipeCalculator.whatIf(spec, prices, null, d("10"))
        assertEquals(2640000L, all.after.batchCostRupiah)
        assertEquals(6600L, all.after.hppPerBaseUnitRupiah)
    }

    @Test fun zeroChangeRetainsBaselineWithFractionalLines() {
        val fractional = spec.copy(recipe = listOf(RecipeLine("A", "a", d("0.3")), RecipeLine("B", "b", d("0.3"))))
        val result = RecipeCalculator.whatIf(fractional, mapOf("a" to 5L, "b" to 5L), null, d("0"))
        assertEquals(3L, result.before.batchCostRupiah)
        assertEquals(result.before, result.after)
        assertEquals(1L, ProductCosting.hpp(fractional.copy(batchYield = d("2")), mapOf("a" to 5L, "b" to 5L), d("0.5")))
        val doubled = RecipeCalculator.whatIf(fractional, mapOf("a" to 5L, "b" to 5L), setOf("a"), d("100"))
        assertEquals(5L, doubled.after.batchCostRupiah)
        val single = fractional.copy(batchYield = d("1"), recipe = listOf(fractional.recipe.first()))
        assertEquals(3L, RecipeCalculator.whatIf(single, mapOf("a" to 5L), null, d("100")).after.batchCostRupiah)
    }

    @Test fun invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { RecipeCalculator.ingredientCost(1, d("0"), d("1")) }
        assertThrows(IllegalArgumentException::class.java) { RecipeCalculator.suggestedPrice(100, d("-1")) }
        assertThrows(IllegalArgumentException::class.java) { RecipeCalculator.whatIf(spec, prices, null, d("-101")) }
    }

    @Test fun priceImpactIsDetectedWhenDisplayedBatchTotalDoesNotChange() {
        val product = spec.copy(batchYield = d("1"), saleUnits = listOf(SaleUnit("bulk", "pak", d("100"), 200)),
            recipe = listOf(RecipeLine("A", "a", d("0.01"))))
        val impact = ProductCosting.priceImpact(listOf(product), mapOf("a" to 100L), "a", 101L).single()
        assertEquals(1L, impact.before.batchCostRupiah)
        assertEquals(1L, impact.after.batchCostRupiah)
        assertEquals(100L, impact.before.perSaleUnit.single().hppRupiah)
        assertEquals(101L, impact.after.perSaleUnit.single().hppRupiah)
    }

    @Test fun priceImpactDetectsCompletenessEvenWhenRoundedAmountsStayZero() {
        val product = spec.copy(batchYield = d("1"), saleUnits = listOf(SaleUnit("u", "pcs", d("1"), 10)),
            recipe = listOf(RecipeLine("A", "a", d("0.001"))))
        val change = ProductCosting.priceImpact(listOf(product), emptyMap(), "a", 1).single()
        assertFalse(change.before.complete)
        assertTrue(change.after.complete)
        assertEquals(0L, change.after.batchCostRupiah)
    }
}
