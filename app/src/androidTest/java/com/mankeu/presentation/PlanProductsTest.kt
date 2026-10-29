package com.mankeu.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecipeLineEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductUnitEntity
import com.mankeu.presentation.screens.PlanScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Regression (Task 7, CP7): Rencana Usaha crashed for any product with two or more sale units. */
@RunWith(AndroidJUnit4::class)
class PlanProductsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun productWithTwoSaleUnitsRendersInThePlan() {
        val record = ProductRecord(
            product = ProductEntity("k", "p", "Keripik", "bungkus", batchYieldText = "400", createdAtEpochMillis = 0),
            units = listOf(
                ProductUnitEntity("k-u0", "k", "bungkus", "1", 8_000, 0),
                ProductUnitEntity("k-u1", "k", "dus", "40", 300_000, 1),
            ),
            recipe = listOf(ProductRecipeLineEntity("k-r0", "k", "Bumbu", amountRupiah = 2_400_000)),
        )
        compose.setContent {
            MankeuTheme {
                PlanScreen(AppUiState(isLoading = false, products = listOf(record)), {}, { _, _, _, _, _, _, _, _ -> }, {}, { _, _ -> }, {})
            }
        }
        compose.onNodeWithText("Keripik").assertIsDisplayed()
    }
}
