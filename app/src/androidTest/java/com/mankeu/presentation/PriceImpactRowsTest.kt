package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class PriceImpactRowsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun incompleteCostIsExplainedWithoutPretendingZeroIsKnown() {
        val product = ProductSpec("p", "Produk uji", "pcs", BigDecimal.ONE,
            listOf(SaleUnit("u", "pcs", BigDecimal.ONE, 10)),
            listOf(RecipeLine("Bahan", "a", BigDecimal("0.001"))))
        val change = ProductCosting.priceImpact(listOf(product), emptyMap(), "a", 1).single()
        compose.setContent { PriceImpactRows(change) }
        compose.onNodeWithText("Produk uji", substring = true).assertExists()
        compose.onNodeWithText(compose.activity.getString(com.mankeu.R.string.price_alert_incomplete), substring = true).assertExists()
    }
    @Test fun completeCostsShowAmounts() {
        val product = ProductSpec("p", "Produk uji", "pcs", BigDecimal.ONE,
            listOf(SaleUnit("u", "pcs", BigDecimal.ONE, 100)),
            listOf(RecipeLine("Bahan", "a", BigDecimal.ONE)))
        val change = ProductCosting.priceImpact(listOf(product), mapOf("a" to 10L), "a", 20).single()
        compose.setContent { PriceImpactRows(change) }
        compose.onNodeWithText("Produk uji", substring = true).assertExists()
        compose.onNodeWithText(compose.activity.getString(com.mankeu.R.string.price_alert_incomplete), substring = true).assertDoesNotExist()
    }
}
