package com.mankeu.domain

import java.math.BigDecimal

object UnitConversion {
    private data class UnitScale(val dimension: Int, val factor: BigDecimal)
    private fun scale(name: String): UnitScale? = when (name.trim().lowercase()) {
        "g", "gr", "gram" -> UnitScale(0, BigDecimal.ONE)
        "ons" -> UnitScale(0, BigDecimal("100"))
        "kg", "kilogram" -> UnitScale(0, BigDecimal("1000"))
        "ml", "mililiter" -> UnitScale(1, BigDecimal.ONE)
        "l", "lt", "liter" -> UnitScale(1, BigDecimal("1000"))
        else -> null
    }
    fun convert(quantity: BigDecimal, from: String, to: String): BigDecimal? {
        if (from.trim().isNotEmpty() && from.trim().equals(to.trim(), ignoreCase = true)) return quantity
        val source = scale(from) ?: return null
        val target = scale(to) ?: return null
        if (source.dimension != target.dimension) return null
        return quantity.multiply(source.factor).divide(target.factor)
    }
}
