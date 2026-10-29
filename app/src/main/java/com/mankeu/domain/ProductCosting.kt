package com.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** A way a product is sold (spec #5): [content] base units per one of it (bungkus = 1, dus = 40), at its own price. */
data class SaleUnit(val id: String, val name: String, val content: BigDecimal, val priceRupiah: Long)

/**
 * One line of a per-batch recipe (spec #6, #20): [quantity] of material [materialId], priced at the
 * material's last purchase price, or a free [amountRupiah] such as gas or wages.
 */
data class RecipeLine(
    val name: String,
    val materialId: String? = null,
    val quantity: BigDecimal? = null,
    val amountRupiah: Long? = null,
)

data class ProductSpec(
    val id: String,
    val name: String,
    val baseUnit: String,
    /** Base units one batch yields. */
    val batchYield: BigDecimal,
    /** In display order; the first is the main unit the plan and the sale form start from. */
    val saleUnits: List<SaleUnit>,
    val recipe: List<RecipeLine>,
)

data class UnitCost(val unit: SaleUnit, val hppRupiah: Long) {
    val profitRupiah: Long get() = unit.priceRupiah - hppRupiah
}

data class ProductCost(
    val batchCostRupiah: Long,
    val hppPerBaseUnitRupiah: Long,
    val perSaleUnit: List<UnitCost>,
    /** Lines whose cost is unknown (material removed or never priced). The HPP is then a floor, not the HPP. */
    val unpricedLines: List<String>,
    val hasRecipe: Boolean,
) {
    val complete: Boolean get() = hasRecipe && unpricedLines.isEmpty()
}

data class HppChange(val product: ProductSpec, val before: ProductCost, val after: ProductCost)

object ProductCosting {
    fun lineCost(line: RecipeLine, materialPrices: Map<String, Long>): Long? = if (line.materialId != null) {
        val price = materialPrices[line.materialId]?.takeIf { it > 0 }
        val quantity = line.quantity
        if (price == null || quantity == null) null else TransactionCalculation.computeTotalRupiah(quantity, price)
    } else {
        line.amountRupiah?.takeIf { it > 0 }
    }

    /** HPP of [content] base units: batch cost × content ÷ yield, rounded half up once. */
    fun hpp(batchCostRupiah: Long, batchYield: BigDecimal, content: BigDecimal): Long =
        BigDecimal.valueOf(batchCostRupiah).multiply(content).divide(batchYield, 0, RoundingMode.HALF_UP).longValueExact()

    fun cost(product: ProductSpec, materialPrices: Map<String, Long>): ProductCost {
        val costs = product.recipe.map { it to lineCost(it, materialPrices) }
        val batch = costs.sumOf { it.second ?: 0L }
        return ProductCost(
            batchCostRupiah = batch,
            hppPerBaseUnitRupiah = hpp(batch, product.batchYield, BigDecimal.ONE),
            perSaleUnit = product.saleUnits.map { UnitCost(it, hpp(batch, product.batchYield, it.content)) },
            unpricedLines = costs.filter { it.second == null }.map { it.first.name },
            hasRecipe = product.recipe.isNotEmpty(),
        )
    }

    /** What [newPriceRupiah] for [materialId] does to every product whose recipe uses it (spec #20). */
    fun priceImpact(
        products: List<ProductSpec>,
        materialPrices: Map<String, Long>,
        materialId: String,
        newPriceRupiah: Long,
    ): List<HppChange> {
        val after = materialPrices + (materialId to newPriceRupiah)
        return products.filter { product -> product.recipe.any { it.materialId == materialId } }
            .map { HppChange(it, cost(it, materialPrices), cost(it, after)) }
            .filter { it.before.batchCostRupiah != it.after.batchCostRupiah }
    }

    /** Schema 12 / template v4 said "Rp [amountRupiah] covers [yieldQuantity] units"; see `LegacyProductShape`. */
    fun legacyComponentPerUnit(amountRupiah: Long, yieldQuantity: BigDecimal): Long =
        BigDecimal.valueOf(amountRupiah).divide(yieldQuantity, 0, RoundingMode.HALF_UP).longValueExact()
}

/** CONSIGN_OUT is read from consignment lines and CONSIGN_RETURN is stored (DEC-029 §8). */
enum class ProductMovementType { PRODUCTION, SALE, ADJUSTMENT, CONSIGN_OUT, CONSIGN_RETURN }

/** [quantity] in base units. */
data class ProductMovement(val type: ProductMovementType, val quantity: BigDecimal)

object ProductStock {
    /**
     * [movements] oldest first. Unlike material stock this may go negative: sales recorded before
     * any production are real sales, and hiding them at zero would make the count lie.
     */
    fun level(movements: List<ProductMovement>): BigDecimal = movements.fold(BigDecimal.ZERO, ::level)

    /** The stock after one [movement] applied to [level]. */
    fun level(level: BigDecimal, movement: ProductMovement): BigDecimal = when (movement.type) {
        ProductMovementType.PRODUCTION, ProductMovementType.CONSIGN_RETURN -> level + movement.quantity
        ProductMovementType.SALE, ProductMovementType.CONSIGN_OUT -> level - movement.quantity
        ProductMovementType.ADJUSTMENT -> movement.quantity
    }

    /** 410 bungkus with dus = 40 → (10 dus, 10 bungkus left). */
    fun split(level: BigDecimal, content: BigDecimal): Pair<BigDecimal, BigDecimal> {
        val whole = level.divide(content, 0, RoundingMode.DOWN)
        return whole to level - whole * content
    }
}
