package com.mankeu.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Moving weighted average for physical traded inventory, in base units and integer rupiah. */
data class TradeInventoryCost private constructor(
    val quantity: BigDecimal,
    private val average: BigDecimal?,
) {
    companion object {
        private val precision = MathContext.DECIMAL128
        fun empty() = TradeInventoryCost(BigDecimal.ZERO, null)
    }

    /** A purchase adds its total cost, keeping unknown opening inventory unknown. */
    fun purchase(amount: BigDecimal, totalRupiah: Long): TradeInventoryCost {
        require(amount.signum() > 0 && totalRupiah >= 0)
        val nextQuantity = quantity.add(amount)
        val nextAverage = if (quantity.signum() == 0) {
            BigDecimal.valueOf(totalRupiah).divide(amount, precision)
        } else average?.multiply(quantity, precision)?.add(BigDecimal.valueOf(totalRupiah))?.divide(nextQuantity, precision)
        return TradeInventoryCost(nextQuantity, nextAverage)
    }

    /** A count preserves the known average; a positive count with no cost remains unknown. */
    fun count(amount: BigDecimal): TradeInventoryCost {
        require(amount.signum() >= 0)
        return if (amount.signum() == 0) empty() else TradeInventoryCost(amount, average)
    }

    fun unitCostRupiah(): Long? = average?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()

    /** Cost is rounded once for the entire line, and null when any stock has unknown cost. */
    fun sell(amount: BigDecimal): TradeSaleCost {
        require(amount.signum() > 0 && amount <= quantity)
        val cost = average?.multiply(amount, precision)?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
        val left = quantity.subtract(amount)
        return TradeSaleCost(cost, if (left.signum() == 0) empty() else TradeInventoryCost(left, average))
    }
}

data class TradeSaleCost(val costRupiah: Long?, val remaining: TradeInventoryCost) {
    val known: Boolean get() = costRupiah != null
}
