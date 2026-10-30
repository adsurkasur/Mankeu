package com.mankeu.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.CategoryEntity
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductUnitEntity
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.StockItem
import com.mankeu.presentation.screens.EntryScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * Cleanup P7: entry-form behaviour DEC-028 left without an automated test (the product fallback
 * gave way to the sales-note link in Fase 2). The form is a
 * sheet, composed with the Activity's resources, so labels come from the target context.
 */
@RunWith(AndroidJUnit4::class)
class EntryFormFallbackTest {
    @get:Rule
    val compose = createComposeRule()

    private fun text(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun category(id: String, role: CategoryRole, name: String) =
        CategoryEntity(id = id, businessId = "b", role = role.name, name = name, normalizedName = name.lowercase(), createdAtEpochMillis = 0)

    private fun product(id: String, name: String, baseUnit: String, units: List<ProductUnitEntity>) =
        ProductRecord(ProductEntity(id = id, projectId = "p", name = name, unit = baseUnit, batchYieldText = "1", createdAtEpochMillis = 0), units, emptyList())

    private val state = AppUiState(
        categories = listOf(category("c-jual", CategoryRole.REVENUE, "Penjualan"), category("c-bahan", CategoryRole.EXPENSE, "Bahan baku")),
        products = listOf(
            product("kopi", "Kopi", "cup", listOf(ProductUnitEntity("u1", "kopi", "cup", "1", 10_000, 0))),
            // Synced from an old phone: no sale unit at all.
            product("teh", "Teh", "gelas", emptyList()),
        ),
        stockItems = listOf(StockItem("minyak", "b", "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)),
    )

    private fun open(kind: FinancialEventType, onNewSale: () -> Unit = {}) {
        compose.setContent {
            MankeuTheme {
                EntryScreen(
                    state = state,
                    onRecord = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                    onDelete = {},
                    onManageScope = {},
                    requestedKind = kind,
                    onNewSale = onNewSale,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun field(label: String): SemanticsNodeInteraction = compose.onNode(hasText(label) and hasSetTextAction())

    private fun SemanticsNodeInteraction.value(): String = fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun choose(fieldLabel: String, option: String) {
        // A read-only menu field: no SetText action, so found by its label alone.
        compose.onNodeWithText(fieldLabel).performScrollTo().performClick()
        compose.onNodeWithText(option).performClick()
        compose.waitForIdle()
    }

    /**
     * Fase 2 (DEC-029 §11): product sales go through the note. The income form no longer offers a
     * product (the old B-7 picker and its unit fallback are gone) but links to the note instead.
     */
    @Test
    fun theIncomeFormOffersTheSalesNoteInsteadOfAProduct() {
        var opened = 0
        open(FinancialEventType.CASH_INCOME) { opened++ }

        assertEquals(0, compose.onAllNodesWithText("Kopi").fetchSemanticsNodes().size)
        compose.onNodeWithText(text(R.string.entry_use_sale_note)).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, opened)
    }

    /** A material locks the unit only while the form is a purchase; switching to Uang masuk opens it again. */
    @Test
    fun aChosenMaterialStopsLockingTheUnitOnceTheFormIsMoneyIn() {
        open(FinancialEventType.CASH_EXPENSE)
        choose(text(R.string.entry_material), "Minyak")
        // Locked: a read-only field has no SetText action.
        val unitField = compose.onNode(hasText(text(R.string.entry_unit)))
        unitField.assert(!hasSetTextAction())
        assertEquals("liter", unitField.value())

        compose.onNodeWithText(text(R.string.kind_cash_in)).performScrollTo().performClick()
        compose.waitForIdle()
        field(text(R.string.entry_unit)).performScrollTo().performTextReplacement("ikat")

        assertEquals("ikat", field(text(R.string.entry_unit)).value())
    }
}
