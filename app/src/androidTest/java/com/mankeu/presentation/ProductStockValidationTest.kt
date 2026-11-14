package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductUnitEntity
import com.mankeu.presentation.screens.ProductionSheet
import com.mankeu.presentation.screens.CountStockSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class ProductStockValidationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun t(id: Int, vararg args: Any) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
    private val record = ProductRecord(ProductEntity("p", "u", "Keripik", "bungkus", batchYieldText = "1", createdAtEpochMillis = 0), listOf(ProductUnitEntity("pu", "p", "bungkus", "1", 8000)), emptyList())
    @Test fun invalidProductionFocusesQuantityAndValidInputSaves() {
        val saved = mutableListOf<BigDecimal>()
        compose.setContent { MankeuTheme { ProductionSheet(record, emptyList(), {}, { q, _ -> saved += q }) } }
        val quantity = compose.onNode(hasContentDescription(t(R.string.production_batches)) and hasSetTextAction())
        quantity.performClick().performTextReplacement("0")
        compose.onNodeWithText(t(R.string.entry_date), substring = true).performClick()
        compose.onAllNodesWithText(t(R.string.action_cancel)).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        compose.onNodeWithText(t(R.string.form_need_quantity)).assertExists()
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        quantity.assertIsFocused().performTextReplacement("2")
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        assertEquals(0, BigDecimal("2").compareTo(saved.single()))
    }
    @Test fun countAcceptsZeroAndFocusesAnEmptyField() {
        val saved = mutableListOf<BigDecimal>()
        compose.setContent { MankeuTheme { CountStockSheet(record, {}, { q, _ -> saved += q }) } }
        val field = compose.onNode(hasSetTextAction() and hasText(t(R.string.count_stock_field, "bungkus"), substring = true))
        field.performTextClearance()
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        field.assertIsFocused().performTextInput("0")
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        assertEquals(0, saved.single().signum())
    }
}
