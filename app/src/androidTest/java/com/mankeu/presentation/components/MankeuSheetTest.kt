package com.mankeu.presentation.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class MankeuSheetTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun swipingContentBackToTopDoesNotDismiss() {
        var dismissed = false
        rule.setContent {
            MankeuSheet(title = "Form", onDismiss = { dismissed = true }, onConfirm = {}) {
                repeat(30) { Text("Baris $it", Modifier.height(48.dp)) }
            }
        }
        rule.onNodeWithText("Baris 5").performTouchInput { swipeUp() }
        rule.onNodeWithText("Baris 10").performTouchInput { swipeDown() }
        repeat(3) { rule.onNodeWithText("Baris 0").performTouchInput { swipe(center, center.copy(y = center.y + 900f), durationMillis = 100) } }
        rule.waitForIdle()
        assertFalse("sheet ditutup oleh geser isi", dismissed)
        rule.onNodeWithText("Form").assertIsDisplayed()
    }
}
