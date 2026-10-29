package com.mankeu.data

import com.mankeu.domain.ProductMovement
import com.mankeu.domain.ProductMovementType
import com.mankeu.domain.ProductSpec
import com.mankeu.domain.ProductStock
import com.mankeu.domain.RecipeLine
import com.mankeu.domain.SaleUnit
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal

/** A product with everything stored about it; [spec] and [stock] are what screens and reports use. */
data class ProductRecord(
    val product: ProductEntity,
    val units: List<ProductUnitEntity>,
    val recipe: List<ProductRecipeLineEntity>,
    val movements: List<ProductMovementEntity> = emptyList(),
    val sales: List<ProductSaleRow> = emptyList(),
) {
    val spec: ProductSpec
        get() = ProductSpec(
            id = product.id,
            name = product.name,
            baseUnit = product.unit,
            batchYield = TransactionCalculation.parseQuantity(product.batchYieldText) ?: BigDecimal.ONE,
            saleUnits = units.sortedBy { it.sortOrder }.map {
                SaleUnit(it.id, it.name, TransactionCalculation.parseQuantity(it.contentText) ?: BigDecimal.ONE, it.priceRupiah)
            },
            recipe = recipe.sortedBy { it.sortOrder }.map {
                RecipeLine(it.name, it.stockItemId, TransactionCalculation.parseQuantity(it.quantityText), it.amountRupiah)
            },
        )

    /** The first sale unit (DEC-028 §8): the plan's unit and the sale form's default. */
    val mainUnit: ProductUnitEntity? get() = units.minByOrNull { it.sortOrder }

    /**
     * Base units in one [unitName]. Null (sales from before sale units, or from a template without
     * the column) means the main unit; a name that no longer exists, e.g. a unit renamed since, also
     * falls back to the main unit rather than dropping the sale from the stock.
     */
    fun contentOf(unitName: String?): BigDecimal {
        val main = spec.saleUnits.firstOrNull()?.content ?: BigDecimal.ONE
        val name = unitName?.trim()?.ifBlank { null } ?: return main
        if (name.equals(product.unit, ignoreCase = true)) return BigDecimal.ONE
        return spec.saleUnits.firstOrNull { it.name.equals(name, ignoreCase = true) }?.content ?: main
    }

    /** Base units on hand: production and counts as stored, sales read from the sale events. */
    val stock: BigDecimal
        get() {
            // parseQuantity returns null for "0", which a count may legitimately be. A type this
            // version does not know (from a newer phone or backup) is skipped rather than crashing.
            val stored = movements.mapNotNull { m ->
                ProductMovementType.entries.firstOrNull { it.name == m.movementType }?.let { type ->
                    Triple(m.dateEpochDay, m.createdAtEpochMillis, ProductMovement(type, TransactionCalculation.parseQuantity(m.quantityText) ?: BigDecimal.ZERO))
                }
            }
            val sold = sales.mapNotNull { s ->
                TransactionCalculation.parseQuantity(s.quantityText)?.let { q ->
                    Triple(s.dateEpochDay, s.createdAtEpochMillis, ProductMovement(ProductMovementType.SALE, q * contentOf(s.unit)))
                }
            }
            return ProductStock.level((stored + sold).sortedWith(compareBy({ it.first }, { it.second })).map { it.third })
        }
}
