package com.mankeu.presentation.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.domain.FinancialEventType
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.screens.EntryScreen
import org.junit.Rule
import org.junit.Test

/** The transaction form is the sheet the owner reported: a long downward swipe must not close it. */
class EntrySheetSwipeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun longSwipeDownDoesNotCloseEntryForm() {
        rule.setContent {
            EntryScreen(
                state = AppUiState(),
                onRecord = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                onDelete = {},
                onManageScope = {},
                requestedKind = FinancialEventType.CASH_INCOME,
            )
        }
        rule.waitForIdle()
        val amountLabel = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.entry_amount)
        repeat(3) {
            rule.onNodeWithText(amountLabel)
                .performTouchInput { swipe(center, center.copy(y = center.y + 900f), durationMillis = 100) }
        }
        rule.waitForIdle()
        rule.onNodeWithText(amountLabel).assertIsDisplayed()
    }
}
