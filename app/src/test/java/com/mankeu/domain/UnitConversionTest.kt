package com.mankeu.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class UnitConversionTest {
    private fun d(s: String) = BigDecimal(s)
    @Test fun conversionsAndUnknownUnits() {
        fun convert(q: String, from: String, to: String, expected: String) =
            assertEquals(0, d(expected).compareTo(UnitConversion.convert(d(q), from, to)))
        convert("250", "gram", "kg", "0.25")
        convert("1.5", "ons", "Kg ", "0.15")
        convert("500", "gr", "KG", "0.5")
        convert("500", "ml", "liter", "0.5")
        convert("1", "l", "ml", "1000")
        assertNull(UnitConversion.convert(d("1"), "sendok", "kg"))
        assertNull(UnitConversion.convert(d("1"), "kg", "liter"))
        convert("4", " butir ", "BUTIR", "4")
        convert("1.5", "sendok", "SENDOK", "1.5")
        assertEquals(3000L, RecipeCalculator.ingredientCost(12000, d("1"), UnitConversion.convert(d("250"), "g", "kg")!!))
        assertEquals(1800L, RecipeCalculator.ingredientCost(12000, d("1"), UnitConversion.convert(d("1.5"), "ons", "kg")!!))
        assertEquals(6750L, RecipeCalculator.ingredientCost(13500, d("1"), UnitConversion.convert(d("500"), "gram", "kg")!!))
        assertEquals(12500L, RecipeCalculator.ingredientCost(25000, d("1"), UnitConversion.convert(d("500"), "ml", "lt")!!))
    }
}
