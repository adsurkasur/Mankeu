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

    /** Base units on hand. Task 3 adds the sales. */
    val stock: BigDecimal
        get() = ProductStock.level(
            movements.sortedWith(compareBy({ it.dateEpochDay }, { it.createdAtEpochMillis })).map {
                // parseQuantity returns null for "0", which a count may legitimately be.
                ProductMovement(ProductMovementType.valueOf(it.movementType), TransactionCalculation.parseQuantity(it.quantityText) ?: BigDecimal.ZERO)
            },
        )
}
