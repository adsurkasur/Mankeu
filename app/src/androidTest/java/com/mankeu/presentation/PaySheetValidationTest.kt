package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.presentation.screens.PaySheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PaySheetValidationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun t(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
    @Test fun emptyPaymentShowsFieldErrorOnSave() {
        compose.setContent { MankeuTheme { PaySheet(600_000, LocalDate.now(), {}, { _, _, _, _ -> }) } }
        val amount = compose.onNode(hasText(t(R.string.sale_pay_amount), substring = true) and hasSetTextAction())
        amount.performTextClearance()
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        amount.assertIsFocused()
        compose.onNodeWithText(t(R.string.form_need_amount)).assertExists()
    }
    @Test fun invalidPaymentShowsErrorAfterBlurAndValidPaymentSaves() {
        val saved = mutableListOf<Long>()
        compose.setContent { MankeuTheme { PaySheet(600_000, LocalDate.now(), {}, { amount, _, _, _ -> saved += amount }) } }
        val amount = compose.onNode(hasText(t(R.string.sale_pay_amount), substring = true) and hasSetTextAction())
        amount.performClick().performTextReplacement("700000")
        amount.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        compose.onNodeWithText(t(R.string.sale_pay_date), substring = true).performClick()
        compose.onAllNodesWithText(t(R.string.action_cancel)).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        amount.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        amount.assertIsFocused().performTextReplacement("600000")
        amount.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        assertEquals(listOf(600_000L), saved)
    }
}
