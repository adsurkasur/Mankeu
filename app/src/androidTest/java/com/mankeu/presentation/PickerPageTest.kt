package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mankeu.presentation.components.form.PickerPage
import com.mankeu.presentation.screens.SaleNoteEditorScreen
import com.mankeu.data.*
import com.mankeu.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PickerPageTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private data class Choice(val id: String, val name: String)

    @Test fun freeLineNoteShowsExplicitReasonAndCannotBeSaved() {
        val record = SaleRecord(SaleEntity("s", "b", "p", null, 20000, createdAtEpochMillis = 0),
            listOf(SaleLineEntity("l", "s", null, "", "1", "1", 1000, 1000, 0, name = "Jasa", costKnown = false)),
            emptyList(), null)
        var saves = 0
        compose.setContent { MankeuTheme {
            SaleNoteEditorScreen("Nota", record, emptyList(), emptyList(), emptyList(), emptyList(),
                onSave = { saves++ }, onSaveContact = { _, _, _, _ -> }, onClose = {})
        } }
        compose.onNodeWithText(compose.activity.getString(R.string.sale_edit_unsupported)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.action_save)).assertIsNotEnabled()
        assertEquals(0, saves)
    }

    @Test fun duplicateLabelsRenderAndSelectByIdentity() {
        var selected = ""
        compose.setContent {
            MankeuTheme {
                PickerPage(title = "Pilih", items = listOf(Choice("one", "Sama"), Choice("two", "Sama")),
                    itemLabel = { it.name }, itemKey = { it.id }, onPick = { selected = it.id }, onClose = {})
            }
        }
        compose.onAllNodesWithText("Sama").assertCountEquals(2)
        compose.onAllNodesWithText("Sama")[1].performClick()
        assertEquals("two", selected)
    }

    @Test fun keysSurviveRenameReorderSearchAndCannotCollideWithAddAction() {
        var choices by mutableStateOf(listOf(Choice("picker_add", "Sama unik"), Choice("two", "Sama unik")))
        var selected = ""
        compose.setContent {
            MankeuTheme {
                PickerPage("Pilih", choices, { it.name }, { it.id }, { selected = it.id }, {}, onAdd = {})
            }
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Sama")
        compose.onAllNodesWithText("Sama unik").assertCountEquals(2)
        compose.onAllNodesWithText("Sama unik")[0].performClick()
        assertEquals("picker_add", selected)
        compose.runOnIdle { choices = choices.reversed().map { if (it.id == "two") it.copy(name = "Nama baru") else it } }
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("baru")
        compose.onNodeWithText("Nama baru").performClick()
        assertEquals("two", selected)
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("")
        compose.onNodeWithText("Sama unik").performClick()
        assertEquals("picker_add", selected)
    }

    @Test fun saleEditorProductAndCustomerCallersUseStableEntityIds() {
        val products = listOf("one", "two").map { id -> ProductRecord(
            ProductEntity(id, "b", "Produk sama", "unit", createdAtEpochMillis = 0),
            listOf(ProductUnitEntity("u$id", id, "unit", "1", 1_000, 0)), emptyList()) }
        val contacts = listOf("customer-one", "customer-two").map { ContactEntity(it, "b", "Nama sama", it, createdAtEpochMillis = 0) }
        var saved: SaleDraft? = null
        compose.setContent {
            MankeuTheme {
                SaleNoteEditorScreen("Nota uji", null, products, emptyList(), contacts, emptyList(),
                    onSave = { saved = it }, onSaveContact = { _, _, _, _ -> }, onClose = {})
            }
        }
        fun clickResource(id: Int) {
            val label = compose.activity.getString(id)
            compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().let { nodes ->
                if (nodes.isNotEmpty()) runCatching { compose.onAllNodes(hasScrollToIndexAction())[0].performScrollToNode(hasText(label)) }
            }
            compose.onNodeWithText(label).performClick()
        }
        clickResource(R.string.sale_add_line)
        compose.onAllNodesWithText("Produk sama").assertCountEquals(2)
        compose.onAllNodesWithText("Produk sama")[1].performClick()
        clickResource(R.string.sale_customer_search)
        compose.onAllNodesWithText("Nama sama").assertCountEquals(2)
        compose.onAllNodesWithText("Nama sama")[1].performClick()
        clickResource(R.string.action_save)
        val result = requireNotNull(saved)
        assertEquals("customer-two", result.contactId)
        assertEquals(listOf("one", "two"), result.lines.map { it.productId })
    }
}
