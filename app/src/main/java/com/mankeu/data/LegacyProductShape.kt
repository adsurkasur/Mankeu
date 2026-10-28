package com.mankeu.data

import com.mankeu.domain.ProductCosting
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal
import java.math.MathContext

/** One recipe line as schema 12 and template v4 wrote it: Rp [amountRupiah] covers [yieldText] units. */
data class ComponentDraft(val name: String, val amountRupiah: Long, val yieldText: String = "1")

/**
 * The schema-12 / template-v4 product shape, carried forward per spec #1: one unit at one price and
 * components "Rp X covers N units". It becomes a batch of 1 base unit, that unit as the first sale
 * unit (content 1) and each component a free line of round(X ÷ N) (DEC-028 §6). The migration, a
 * schema-12 backup, and until Task 4/5 the v4 import and the plan's product sheet all go through here.
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

    fun draft(name: String, unit: String, priceRupiah: Long, targetPerMonthText: String?, components: List<ComponentDraft>, sellFromMonth: Int) =
        ProductDraft(
            name = name,
            baseUnit = unit,
            batchYieldText = "1",
            units = listOf(SaleUnitDraft(name = unit, contentText = "1", priceRupiah = priceRupiah)),
            recipe = components.filter { it.name.isNotBlank() }.map { c ->
                RecipeLineDraft(c.name, amountRupiah = ProductCosting.legacyComponentPerUnit(c.amountRupiah, TransactionCalculation.parseQuantity(c.yieldText) ?: BigDecimal.ONE))
            },
            targetPerMonthText = targetPerMonthText,
            sellFromMonth = sellFromMonth,
        )

    /**
     * The other direction, for the v4 export and the plan sheet until Task 4/5: each line's cost
     * covers batchYield ÷ main content units. Lossless for products of the legacy shape only.
     */
    fun components(record: ProductRecord, materialPrices: Map<String, Long>): List<ComponentDraft> {
        val spec = record.spec
        val content = spec.saleUnits.firstOrNull()?.content ?: BigDecimal.ONE
        val covers = spec.batchYield.divide(content, MathContext.DECIMAL64).stripTrailingZeros().toPlainString()
        return spec.recipe.map { ComponentDraft(it.name, ProductCosting.lineCost(it, materialPrices) ?: 0L, covers) }
    }
}
