package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mankeu.data.ContactEntity
import com.mankeu.data.SaleEntity
import com.mankeu.data.SaleLineEntity
import com.mankeu.data.SaleRecord
import com.mankeu.presentation.screens.EntryScreen
import com.mankeu.presentation.screens.NoteFilter
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.FinancialEventType
import com.mankeu.R
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class LedgerEntryScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val today = LocalDate.now()
    private fun note(id: String, due: LocalDate) = SaleRecord(
        SaleEntity(id, "b", "p", id, today.toEpochDay(), dueDateEpochDay = due.toEpochDay(), createdAtEpochMillis = 0),
        listOf(SaleLineEntity("line$id", id, "product", "unit", "1", "1", 1_000, 1_000, 0)),
        emptyList(), ContactEntity(id, "b", id, id, createdAtEpochMillis = 0),
    )

    @Test fun incomingOverdueIntentKeepsOnlyOverdueNotes() {
        compose.setContent {
            MankeuTheme {
                EntryScreen(AppUiState(selectedProjectId = "p", sales = listOf(note("Terlambat", today.minusDays(1)), note("Besok", today.plusDays(1)))),
                    onEdit = {}, onDelete = {}, onManageScope = {}, noteFilter = NoteFilter.OVERDUE)
            }
        }
        compose.onNodeWithText("Terlambat").assertExists()
        compose.onNodeWithText("Besok").assertDoesNotExist()
    }

    @Test fun loadEarlierIsAvailableWhenFilterHasNoMatches() {
        compose.setContent {
            MankeuTheme {
                EntryScreen(AppUiState(selectedProjectId = "p", ledgerHasEarlier = true, sales = listOf(note("Besok", today.plusDays(1)))),
                    onEdit = {}, onDelete = {}, onManageScope = {}, filter = CatatanFilter.EXPENSE)
            }
        }
        compose.onNodeWithText(compose.activity.getString(com.mankeu.R.string.ledger_load_earlier)).assertExists()
    }

    @Test fun overdueChipKeepsOnlyLateNotes() {
        var filter by mutableStateOf(CatatanFilter.ALL)
        compose.setContent { MankeuTheme {
            EntryScreen(AppUiState(selectedProjectId = "p", sales = listOf(note("Terlambat", today.minusDays(1)), note("Besok", today.plusDays(1)))),
                onEdit = {}, onDelete = {}, onManageScope = {}, filter = filter, onFilter = { filter = it })
        } }
        val label = compose.activity.getString(R.string.notes_filter_overdue)
        compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().let { nodes ->
            for (i in nodes.indices) runCatching { compose.onAllNodes(hasScrollToIndexAction())[i].performScrollToNode(hasText(label)) }
        }
        compose.onAllNodesWithText(label)[0].performClick()
        compose.onNodeWithText("Terlambat").assertExists()
        compose.onNodeWithText("Besok").assertDoesNotExist()
    }

    @Test fun searchingAbsorbedPaymentReferenceShowsItsNote() {
        val payment = MoneyEvent("pay", "b", "p", FinancialEventType.CASH_INCOME, 1_000, today,
            "Bayar nota", "Penjualan", reference = "REF-ABSORBED", saleId = "Pelanggan")
        val record = note("Pelanggan", today.plusDays(1)).copy(payments = listOf(payment))
        compose.setContent { MankeuTheme {
            EntryScreen(AppUiState(selectedProjectId = "p", sales = listOf(record), events = listOf(payment)),
                onEdit = {}, onDelete = {}, onManageScope = {}, filter = CatatanFilter.INCOME)
        } }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("REF-ABSORBED")
        compose.onNodeWithText("Pelanggan").assertExists()
    }
}
