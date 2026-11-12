package com.mankeu.usability

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

/**
 * Appendix C §1.4: taps are successful performClick calls, typed fields are successful
 * performTextInput calls (including repeated input to the same field, not characters).
 * interactionCount includes both. Counts are per instance and advance only after the action succeeds.
 */
class TapCounter {
    var tapCount: Int = 0
        private set
    var typedFieldCount: Int = 0
        private set
    val interactionCount: Int get() = tapCount + typedFieldCount

    fun performClick(node: SemanticsNodeInteraction): SemanticsNodeInteraction {
        val result = node.performClick()
        tapCount++
        return result
    }

    fun performTextInput(node: SemanticsNodeInteraction, text: String) {
        node.performTextInput(text)
        typedFieldCount++
    }

    fun reset() {
        tapCount = 0
        typedFieldCount = 0
    }
}
