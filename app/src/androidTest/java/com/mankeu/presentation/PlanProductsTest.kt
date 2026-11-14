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
    fun missingCostIsNotDisplayedAsZeroInProductRow() {
        val record = ProductRecord(
            product = ProductEntity("k", "p", "Keripik", "bungkus", batchYieldText = "1", createdAtEpochMillis = 0),
            units = listOf(ProductUnitEntity("k-u0", "k", "bungkus", "1", 8_000, 0)),
            recipe = emptyList(),
        )
        val project = ProjectEntity("p", "b", "Utama", isGeneral = true, createdAtEpochMillis = 0)
        val state = AppUiState(isLoading = false, projects = listOf(project), selectedProjectId = "p", products = listOf(record))
        compose.setContent { MankeuTheme { PlanScreen(state, {}, { _, _, _, _, _, _, _, _ -> }, {}, { _, _ -> }, {}) } }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Keripik", substring = true))
        assertTrue(compose.onAllNodesWithText("—", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Rp0", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun incompletePlanShowsWarningInsteadOfCalculatedFeasibility() {
        val record = ProductRecord(
            ProductEntity("k", "p", "Keripik", "bungkus", batchYieldText = "1", targetPerMonthText = "10", createdAtEpochMillis = 0),
            listOf(ProductUnitEntity("ku", "k", "bungkus", "1", 8_000)), emptyList(),
        )
        val project = ProjectEntity("p", "b", "Utama", isGeneral = true, createdAtEpochMillis = 0)
        val plan = com.mankeu.domain.PlanCalculator.analyse(com.mankeu.data.PlanAssembly.planInputs(project, listOf(record), emptyList(), emptyList(), java.time.LocalDate.now(), emptyMap()))
        compose.setContent { MankeuTheme { com.mankeu.presentation.components.PlanFeasibilityCard(plan) } }
        val warning = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(com.mankeu.R.string.plan_cost_incomplete)
        assertTrue(compose.onAllNodesWithText(warning, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Rp80.000", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun actualBreakEvenWarnsAboutMissingCurrentCostWithoutHidingActualProfit() {
        val record = ProductRecord(ProductEntity("k", "p", "Keripik", "bungkus", batchYieldText = "1", createdAtEpochMillis = 0), listOf(ProductUnitEntity("ku", "k", "bungkus", "1", 8_000)), emptyList())
        val products = com.mankeu.data.PlanAssembly.products(listOf(record), emptyMap())
        val input = com.mankeu.domain.ActualMonthInputs(java.time.YearMonth.now(), mapOf("k" to java.math.BigDecimal.TEN), 80_000, 0, 10_000, 0, 0, hppRupiah = 40_000)
        val actual = com.mankeu.domain.ActualBepCalculator.analyse(products, input)
        compose.setContent { MankeuTheme { com.mankeu.presentation.components.ActualMonthCard(actual, listOf(record), null) } }
        val warning = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(com.mankeu.R.string.actual_bep_cost_incomplete)
        assertTrue(compose.onAllNodesWithText(warning, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Rp30.000", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun knownManualCostAppearsInPlanRow() {
        val record = ProductRecord(ProductEntity("k", "p", "Keripik", "bungkus", batchYieldText = "1", manualCostRupiah = 3_000, createdAtEpochMillis = 0), listOf(ProductUnitEntity("ku", "k", "paket", "2", 8_000)), emptyList())
        val project = ProjectEntity("p", "b", "Utama", isGeneral = true, createdAtEpochMillis = 0)
        val state = AppUiState(isLoading = false, projects = listOf(project), selectedProjectId = "p", products = listOf(record))
        compose.setContent { MankeuTheme { PlanScreen(state, {}, { _, _, _, _, _, _, _, _ -> }, {}, { _, _ -> }, {}) } }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Keripik", substring = true))
        assertTrue(compose.onAllNodesWithText("Rp6.000", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

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
        assertTrue(compose.onAllNodesWithText("Rp6.000", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }
}
