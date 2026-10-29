package com.mankeu.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecipeLineEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductUnitEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.presentation.screens.PlanScreen
import org.junit.Assert.assertTrue
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
        val project = ProjectEntity("p", "b", "Utama", isGeneral = true, createdAtEpochMillis = 0)
        val state = AppUiState(isLoading = false, projects = listOf(project), selectedProjectId = "p", products = listOf(record))
        compose.setContent {
            MankeuTheme {
                PlanScreen(state, {}, { _, _, _, _, _, _, _, _ -> }, {}, { _, _ -> }, {})
            }
        }
        // Composing the product row is what crashed; it must be built (a scroll may be needed to reach it).
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Keripik", substring = true))
        assertTrue(compose.onAllNodesWithText("Keripik", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }
}
