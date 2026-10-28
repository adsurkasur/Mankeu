package com.mankeu.data

import com.mankeu.domain.ProductCosting
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal

/**
 * The schema-12 / template-v4 product shape, carried forward per spec #1: one unit at one price and
 * components "Rp X covers N units". It becomes a batch of 1 base unit, that unit as the first sale
 * unit (content 1) and each component a free line of round(X ÷ N) (DEC-028 §6). The migration and a
 * schema-12 backup go through here.
 * Ids are derived from the old ids, so two devices migrating on their own produce the same rows.
 */
object LegacyProductShape {
    fun saleUnitId(productId: String) = "$productId-u0"

    fun saleUnit(productId: String, unit: String, priceRupiah: Long) =
        ProductUnitEntity(saleUnitId(productId), productId, unit, "1", priceRupiah, 0)

    fun recipeLine(id: String, productId: String, name: String, amountRupiah: Long, yieldText: String?, sortOrder: Int) =
        ProductRecipeLineEntity(
            id = id,
            productId = productId,
            name = name,
            amountRupiah = ProductCosting.legacyComponentPerUnit(amountRupiah, TransactionCalculation.parseQuantity(yieldText) ?: BigDecimal.ONE),
            sortOrder = sortOrder,
        )
}
