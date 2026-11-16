package com.mankeu.domain

import java.math.BigDecimal
import java.math.BigInteger

/** Exact rational arithmetic prevents a repeating average from changing half-up rupiah rounding. */
private data class ExactRatio private constructor(val numerator: BigInteger, val denominator: BigInteger) {
    companion object {
        fun of(numerator: BigInteger, denominator: BigInteger = BigInteger.ONE): ExactRatio {
            require(numerator.signum() >= 0 && denominator.signum() > 0)
            val gcd = numerator.gcd(denominator)
            return ExactRatio(numerator / gcd, denominator / gcd)
        }
        fun of(value: Long) = of(BigInteger.valueOf(value))
        fun of(value: BigDecimal): ExactRatio {
            require(value.signum() >= 0)
            val decimal = value.stripTrailingZeros()
            return if (decimal.scale() >= 0) of(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()))
            else of(decimal.unscaledValue() * BigInteger.TEN.pow(-decimal.scale()))
        }
    }
    operator fun plus(other: ExactRatio) = of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)
    operator fun times(other: ExactRatio) = of(numerator * other.numerator, denominator * other.denominator)
    operator fun div(other: ExactRatio): ExactRatio {
        require(other.numerator.signum() > 0)
        return of(numerator * other.denominator, denominator * other.numerator)
    }
    fun roundRupiah(): Long {
        val (whole, remainder) = numerator.divideAndRemainder(denominator)
        val rounded = if (remainder * BigInteger.valueOf(2L) >= denominator) whole + BigInteger.ONE else whole
        require(rounded <= BigInteger.valueOf(Long.MAX_VALUE)) { "Modal barang terlalu besar." }
        return rounded.toLong()
    }
}

/** Moving weighted average for physical traded inventory, in base units and integer rupiah. */
class TradeInventoryCost private constructor(
    val quantity: BigDecimal,
    private val average: ExactRatio?,
) {
    companion object {
        fun empty() = TradeInventoryCost(BigDecimal.ZERO, null)
    }

    /** A purchase adds its total cost, keeping unknown opening inventory unknown. */
    fun purchase(amount: BigDecimal, totalRupiah: Long): TradeInventoryCost {
        require(amount.signum() > 0 && totalRupiah >= 0)
        val nextQuantity = quantity.add(amount)
        val nextAverage = if (quantity.signum() == 0) {
            ExactRatio.of(totalRupiah) / ExactRatio.of(amount)
        } else average?.let { (it * ExactRatio.of(quantity) + ExactRatio.of(totalRupiah)) / ExactRatio.of(nextQuantity) }
        return TradeInventoryCost(nextQuantity, nextAverage)
    }

    /** A count preserves the known average; a positive count with no cost remains unknown. */
    fun count(amount: BigDecimal): TradeInventoryCost {
        require(amount.signum() >= 0)
        return if (amount.signum() == 0) empty() else TradeInventoryCost(amount, average)
    }

    fun unitCostRupiah(): Long? = average?.roundRupiah()

    /** Cost is rounded once for the entire line, and null when any stock has unknown cost. */
    fun sell(amount: BigDecimal): TradeSaleCost {
        require(amount.signum() > 0 && amount <= quantity)
        val cost = average?.let { (it * ExactRatio.of(amount)).roundRupiah() }
        val left = quantity.subtract(amount)
        return TradeSaleCost(cost, if (left.signum() == 0) empty() else TradeInventoryCost(left, average))
    }
}

data class TradeSaleCost(val costRupiah: Long?, val remaining: TradeInventoryCost) {
    val known: Boolean get() = costRupiah != null
}
