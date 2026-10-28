package com.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure domain calculation and reconciliation functions for money amounts, quantities/volumes,
 * and unit prices.
 *
 * All money calculations adhere strictly to the exact integer rupiah invariant (never floating-point).
 * Quantities are handled with [BigDecimal] and standard half-up rounding.
 */
object TransactionCalculation {

    /**
     * Parses a decimal number string from user input or storage.
     * Supports both Indonesian comma ('2,5') and standard decimal point ('2.5').
     * Handles whitespace, commas, dots, and trailing separators gracefully without throwing.
     * Returns null if blank, zero, negative, or invalid.
     */
    fun parseQuantity(raw: String?): BigDecimal? {
        if (raw.isNullOrBlank()) return null
        val clean = raw.trim().replace(',', '.')
        return runCatching { BigDecimal(clean) }.getOrNull()?.takeIf { it > BigDecimal.ZERO }
    }

    /**
     * Calculates the total nominal amount (in exact integer rupiah) from quantity and unit price.
     * Formula: round(quantity * unitPriceRupiah) with half-up rounding.
     */
    fun computeTotalRupiah(quantity: BigDecimal, unitPriceRupiah: Long): Long {
        if (unitPriceRupiah <= 0L || quantity <= BigDecimal.ZERO) return 0L
        return quantity.multiply(BigDecimal.valueOf(unitPriceRupiah))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * Calculates the unit price (in exact integer rupiah) from total amount and quantity.
     * Formula: round(totalRupiah / quantity) with half-up rounding.
     */
    fun computeUnitPriceRupiah(totalRupiah: Long, quantity: BigDecimal): Long {
        if (totalRupiah <= 0L || quantity <= BigDecimal.ZERO) return 0L
        return BigDecimal.valueOf(totalRupiah)
            .divide(quantity, 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /** [computeUnitPriceRupiah] for a purchase; a result that rounds to Rp0 is no price (null), so it cannot zero a material. */
    fun derivedUnitPriceRupiah(totalRupiah: Long, quantity: BigDecimal): Long? =
        computeUnitPriceRupiah(totalRupiah, quantity).takeIf { it > 0 }

    /**
     * Validates whether a total amount is mathematically consistent with quantity and unit price.
     * Returns true if either quantity or unit price is absent (lump-sum), or if total == round(quantity * unitPrice).
     */
    fun isConsistent(totalRupiah: Long, quantityText: String?, unitPriceRupiah: Long?): Boolean {
        val qty = parseQuantity(quantityText) ?: return true
        val price = unitPriceRupiah?.takeIf { it > 0 } ?: return true
        return totalRupiah == computeTotalRupiah(qty, price)
    }

    /**
     * Reconciles transaction calculation inputs into a guaranteed non-contradictory triplet:
     * (reconciledTotalRupiah, sanitizedQuantityText, reconciledUnitPriceRupiah).
     *
     * Rules:
     * 1. If quantity is absent or invalid -> treated as lump-sum (quantity = null, unitPrice = null).
     * 2. If quantity and unitPrice are present -> total is authoritative or aligned to round(qty * unitPrice).
     * 3. If quantity and total are present without unitPrice -> unitPrice is auto-derived as round(total / qty).
     */
    fun reconcile(
        amountRupiah: Long,
        quantityText: String?,
        unitPriceRupiah: Long?,
    ): ReconciledCalculation {
        val qty = parseQuantity(quantityText)
        if (qty == null) {
            return ReconciledCalculation(
                totalRupiah = amountRupiah,
                quantityText = null,
                unitPriceRupiah = null,
            )
        }

        val price = unitPriceRupiah?.takeIf { it > 0 }
        return if (price != null) {
            val formulaTotal = computeTotalRupiah(qty, price)
            if (formulaTotal == amountRupiah) {
                ReconciledCalculation(
                    totalRupiah = amountRupiah,
                    quantityText = quantityText?.trim()?.ifBlank { null },
                    unitPriceRupiah = price,
                )
            } else if (amountRupiah > 0) {
                // If user entered total explicitly, reconcile unit price to match total / qty
                val derivedPrice = computeUnitPriceRupiah(amountRupiah, qty)
                ReconciledCalculation(
                    totalRupiah = amountRupiah,
                    quantityText = quantityText?.trim()?.ifBlank { null },
                    unitPriceRupiah = derivedPrice,
                )
            } else {
                ReconciledCalculation(
                    totalRupiah = formulaTotal,
                    quantityText = quantityText?.trim()?.ifBlank { null },
                    unitPriceRupiah = price,
                )
            }
        } else if (amountRupiah > 0) {
            val derivedPrice = computeUnitPriceRupiah(amountRupiah, qty)
            ReconciledCalculation(
                totalRupiah = amountRupiah,
                quantityText = quantityText?.trim()?.ifBlank { null },
                unitPriceRupiah = derivedPrice,
            )
        } else {
            ReconciledCalculation(
                totalRupiah = 0L,
                quantityText = quantityText?.trim()?.ifBlank { null },
                unitPriceRupiah = null,
            )
        }
    }
}

data class ReconciledCalculation(
    val totalRupiah: Long,
    val quantityText: String?,
    val unitPriceRupiah: Long?,
)
