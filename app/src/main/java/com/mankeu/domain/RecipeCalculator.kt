package com.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class RecipeWhatIf(val before: ProductCost, val after: ProductCost)

object RecipeCalculator {
    fun ingredientCost(priceRupiah: Long, priceQuantity: BigDecimal, usedQuantity: BigDecimal): Long {
        require(priceRupiah >= 0 && priceQuantity > BigDecimal.ZERO && usedQuantity >= BigDecimal.ZERO)
        return BigDecimal.valueOf(priceRupiah).multiply(usedQuantity)
            .divide(priceQuantity, 0, RoundingMode.HALF_UP).longValueExact()
    }
    fun suggestedPrice(costRupiah: Long, markupPercent: BigDecimal): Long {
        require(costRupiah >= 0 && markupPercent >= BigDecimal.ZERO)
        return BigDecimal.valueOf(costRupiah).multiply(BigDecimal("100") + markupPercent)
            .divide(BigDecimal("10000"), 0, RoundingMode.CEILING)
            .multiply(BigDecimal("100")).longValueExact()
    }
    /** Null materialIds changes all costs, including free-cost recipe lines. */
    fun whatIf(spec: ProductSpec, prices: Map<String, Long>, materialIds: Set<String>?, percent: BigDecimal): RecipeWhatIf {
        require(percent >= BigDecimal("-100"))
        val before = ProductCosting.cost(spec, prices)
        val factor = BigDecimal.ONE + percent.divide(BigDecimal("100"))
        val exactBatch = spec.recipe.fold(BigDecimal.ZERO) { total, line ->
            val amount = ProductCosting.exactLineCost(line, prices) ?: BigDecimal.ZERO
            val changed = materialIds == null || (line.materialId != null && line.materialId in materialIds)
            total + if (changed) amount.multiply(factor) else amount
        }
        val net = spec.batchYield - spec.batchLoss
        fun hpp(content: BigDecimal) = exactBatch.multiply(content).divide(net, 0, RoundingMode.HALF_UP).longValueExact()
        val after = ProductCost(
            batchCostRupiah = exactBatch.setScale(0, RoundingMode.HALF_UP).longValueExact(),
            hppPerBaseUnitRupiah = hpp(BigDecimal.ONE),
            perSaleUnit = spec.saleUnits.map { UnitCost(it, hpp(it.content)) },
            unpricedLines = before.unpricedLines,
            hasRecipe = before.hasRecipe,
        )
        return RecipeWhatIf(before, after)
    }
}
