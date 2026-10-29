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
    /** Sale-note lines of this product (DEC-029 §8); lines of a consignment sell-through note carry its id. */
    val sales: List<ProductSaleRow> = emptyList(),
    /** Lines of consignments that sent this product out (CONSIGN_OUT, DEC-029 §8). */
    val consignmentSends: List<ConsignmentSendRow> = emptyList(),
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

    /** Base units on hand ("di gudang"): production, counts and returns as stored, sales read from note lines, sends from consignments. */
    val stock: BigDecimal get() = history.lastOrNull()?.stockAfter ?: BigDecimal.ZERO

    /**
     * Production, counts and sales oldest first (date, then time recorded), each with the stock after
     * it: the Riwayat stok page (cleanup P1) and [stock] read the same list.
     */
    val history: List<StockHistoryEntry>
        get() {
            // parseQuantity returns null for "0", which a count may legitimately be. A type this
            // version does not know (from a newer phone or backup) is skipped rather than crashing.
            val stored = movements.mapNotNull { m ->
                ProductMovementType.entries.firstOrNull { it.name == m.movementType }?.let { type ->
                    Triple(m.dateEpochDay, m.createdAtEpochMillis, StockHistoryEntry(m.id, type, m.dateEpochDay, TransactionCalculation.parseQuantity(m.quantityText) ?: BigDecimal.ZERO, BigDecimal.ZERO, m.batchesText, m.notes))
                }
            }
            val mainName = mainUnit?.name ?: product.unit
            // A consignment sell-through left the stock when it was sent, so it does not count again.
            val sold = sales.filter { it.consignmentId == null }.mapNotNull { s ->
                TransactionCalculation.parseQuantity(s.quantityText)?.let { q ->
                    val soldText = "${q.stripTrailingZeros().toPlainString().replace('.', ',')} ${s.unit?.trim()?.ifBlank { null } ?: mainName}"
                    Triple(s.dateEpochDay, s.createdAtEpochMillis, StockHistoryEntry(s.id, ProductMovementType.SALE, s.dateEpochDay, q * s.content(), BigDecimal.ZERO, soldText = soldText))
                }
            }
            val sent = consignmentSends.mapNotNull { c ->
                TransactionCalculation.parseQuantity(c.quantityText)?.let { q ->
                    Triple(c.dateEpochDay, c.createdAtEpochMillis, StockHistoryEntry(c.id, ProductMovementType.CONSIGN_OUT, c.dateEpochDay, q * c.content, BigDecimal.ZERO))
                }
            }
            var level = BigDecimal.ZERO
            return (stored + sold + sent).sortedWith(compareBy({ it.first }, { it.second })).map { (_, _, entry) ->
                level = ProductStock.level(level, ProductMovement(entry.type, entry.quantity))
                entry.copy(stockAfter = level)
            }
        }

    /** Base units at stores on consignment: sent − sold through notes − returned (DEC-029 §8). */
    val consigned: BigDecimal
        get() {
            val sent = consignmentSends.sumOf { (TransactionCalculation.parseQuantity(it.quantityText) ?: BigDecimal.ZERO) * it.content }
            val sold = sales.filter { it.consignmentId != null }.sumOf { (TransactionCalculation.parseQuantity(it.quantityText) ?: BigDecimal.ZERO) * it.content() }
            val returned = movements.filter { it.movementType == ProductMovementType.CONSIGN_RETURN.name }.sumOf { TransactionCalculation.parseQuantity(it.quantityText) ?: BigDecimal.ZERO }
            return sent - sold - returned
        }

    /** Main sale units sold in [month] by note lines, sell-through included (ActualBep, DEC-029 §4). */
    fun unitsSoldIn(month: java.time.YearMonth): BigDecimal {
        val main = contentOf(null)
        return sales.filter { java.time.YearMonth.from(java.time.LocalDate.ofEpochDay(it.dateEpochDay)) == month }
            .fold(BigDecimal.ZERO) { sum, row ->
                sum + (TransactionCalculation.parseQuantity(row.quantityText) ?: BigDecimal.ZERO) * row.content().divide(main, java.math.MathContext.DECIMAL64)
            }
    }

    /** A note line's content snapshot; rows without one (tests, old shapes) read the product's unit. */
    private fun ProductSaleRow.content(): BigDecimal = TransactionCalculation.parseQuantity(contentText) ?: contentOf(unit)
}

/** One consignment line as the stock reads it: [quantityText] of a unit holding [contentText] base units. */
data class ConsignmentSendRow(
    val id: String,
    val productId: String,
    val quantityText: String,
    val contentText: String,
    val dateEpochDay: Long,
    val createdAtEpochMillis: Long,
) {
    val content: BigDecimal get() = TransactionCalculation.parseQuantity(contentText) ?: BigDecimal.ONE
}

/**
 * One line of a product's stock history: [id] is the production/count/return row, the note line of a
 * [ProductMovementType.SALE] or the consignment line of a CONSIGN_OUT (both changed elsewhere, not here). [quantity] and [stockAfter] are base units;
 * [soldText] is the sale as recorded ("2 dus").
 */
data class StockHistoryEntry(
    val id: String,
    val type: ProductMovementType,
    val dateEpochDay: Long,
    val quantity: BigDecimal,
    val stockAfter: BigDecimal,
    val batchesText: String? = null,
    val notes: String = "",
    val soldText: String? = null,
)
