package com.mankeu.usability

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TapCounterTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun start() {
        compose.setContent {
            var clicks by remember { mutableStateOf(0) }
            var text by remember { mutableStateOf("") }
            Column {
                Button(onClick = { clicks++ }, modifier = Modifier.testTag("button")) { Text("Click") }
                Text("$clicks", Modifier.testTag("result"))
                OutlinedTextField(text, { text = it }, modifier = Modifier.testTag("field"))
            }
        }
    }

    @Test fun delegatesRealUiActionsAndCountsClicksSeparatelyFromTypedInputs() {
        start()
        val counter = TapCounter()
        assertCounts(counter, 0, 0)
        counter.performClick(compose.onNodeWithTag("button"))
        counter.performClick(compose.onNodeWithTag("button"))
        counter.performTextInput(compose.onNodeWithTag("field"), "synthetic")
        compose.onNodeWithTag("result").assertTextEquals("2")
        compose.onNodeWithTag("field").assertTextEquals("synthetic")
        assertCounts(counter, 2, 1)
    }

    @Test fun failedActionsAreNotCounted() {
        start()
        val counter = TapCounter()
        assertThrows(AssertionError::class.java) { counter.performClick(compose.onNodeWithTag("missing")) }
        assertThrows(AssertionError::class.java) { counter.performTextInput(compose.onNodeWithTag("missing"), "x") }
        assertCounts(counter, 0, 0)
    }

    @Test fun resetStartsANewMeasurementWithoutResettingTheUi() {
        start()
        val counter = TapCounter()
        counter.performClick(compose.onNodeWithTag("button"))
        counter.performTextInput(compose.onNodeWithTag("field"), "x")
        counter.reset()
        assertCounts(counter, 0, 0)
        compose.onNodeWithTag("result").assertTextEquals("1")
        compose.onNodeWithTag("field").assertTextEquals("x")
        counter.performClick(compose.onNodeWithTag("button"))
        assertCounts(counter, 1, 0)
    }

    @Test fun countersAreIndependentAndEachTextInputCallCountsOnce() {
        start()
        val first = TapCounter()
        val second = TapCounter()
        first.performClick(compose.onNodeWithTag("button"))
        second.performTextInput(compose.onNodeWithTag("field"), "a")
        second.performTextInput(compose.onNodeWithTag("field"), "b")
        assertCounts(first, 1, 0)
        assertCounts(second, 0, 2)
        first.reset()
        assertCounts(second, 0, 2)
        compose.onNodeWithTag("field").assertTextEquals("ab")
    }

    private fun assertCounts(counter: TapCounter, clicks: Int, inputs: Int) {
        assertEquals(clicks, counter.tapCount)
        assertEquals(inputs, counter.typedFieldCount)
        assertEquals(clicks + inputs, counter.interactionCount)
    }
}
