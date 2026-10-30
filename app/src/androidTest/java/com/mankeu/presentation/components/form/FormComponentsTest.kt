package com.mankeu.presentation.components.form

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.presentation.MankeuTheme
import com.mankeu.presentation.toRupiah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UX phase U1 (DEC-030): one test per shared form component. */
@RunWith(AndroidJUnit4::class)
class FormComponentsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun field() = compose.onNode(hasSetTextAction())

    private fun fieldText(): String = field().fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    @Test
    fun moneyInputGroupsThousandsWhileTypingAndYieldsALong() {
        var value: Long? = -1
        compose.setContent {
            MankeuTheme {
                var current by remember { mutableStateOf<Long?>(null) }
                MoneyInput(
                    label = "Harga",
                    value = current,
                    onValueChange = { current = it; value = it },
                    chips = listOf(MoneyChip("Sisa", 600_000)),
                )
            }
        }
        // One key at a time, so the cursor mapping is exercised after every separator appears.
        "1250000".forEach { field().performTextInput(it.toString()) }
        assertEquals("1.250.000", fieldText())
        assertEquals(1_250_000L, value)

        field().performTextClearance()
        assertNull(value)
        field().performTextInput("0")
        assertNull(value)
        field().performTextInput("5")
        assertEquals(5L, value)

        compose.onNodeWithText("Sisa", substring = true).performClick()
        assertEquals(600_000L, value)
        assertEquals("600.000", fieldText())
    }

    @Test
    fun stepperNeverGoesBelowItsMinimum() {
        var value = 0
        compose.setContent {
            MankeuTheme {
                var current by remember { mutableStateOf(1) }
                Stepper(value = current, onValueChange = { current = it; value = it }, label = "Batch", min = 1)
            }
        }
        // Named after the stepper: "Kurangi Batch".
        val minus = compose.onNodeWithContentDescription(text(R.string.stepper_decrease) + " Batch")
        val plus = compose.onNodeWithContentDescription(text(R.string.stepper_increase) + " Batch")
        compose.onNodeWithContentDescription("Batch").assert(hasSetTextAction())
        minus.assertIsNotEnabled()
        plus.performClick()
        assertEquals(2, value)
        minus.performClick()
        assertEquals(1, value)
        minus.assertIsNotEnabled()
        // Typing under the minimum is clamped, not stored.
        field().performTextReplacement("0")
        assertEquals(1, value)
        // The field can be emptied to retype; nothing is stored until a number is typed.
        field().performTextClearance()
        assertEquals("", fieldText())
        assertEquals(1, value)
        field().performTextInput("7")
        assertEquals(7, value)
    }

    @Test
    fun pickerPageSearchesAndOffersToAddTheTypedName() {
        var picked: String? = null
        var added: String? = null
        compose.setContent {
            MankeuTheme {
                PickerPage(
                    title = "Pilih bahan",
                    items = listOf("Singkong", "Minyak goreng", "Garam"),
                    itemLabel = { it },
                    onPick = { picked = it },
                    onClose = {},
                    onAdd = { added = it },
                )
            }
        }
        field().performTextInput("minyak")
        compose.onNodeWithText("Minyak goreng").assertExists()
        assertEquals(0, onAllNodesWithTextCount("Singkong"))

        // An exact match does not offer to add a duplicate.
        field().performTextReplacement("singkong")
        assertEquals(0, onAllNodesWithTextCount(text(R.string.picker_add, "singkong")))
        compose.onNodeWithText("Singkong").performClick()
        assertEquals("Singkong", picked)

        field().performTextReplacement("Gula")
        compose.onNodeWithText(text(R.string.picker_add, "Gula")).performClick()
        assertEquals("Gula", added)
    }

    @Test
    fun tappingALineRowOpensItsSheetAndSaveUpdatesTheRow() {
        compose.setContent {
            MankeuTheme {
                var amount by remember { mutableLongStateOf(1_500_000) }
                var draft by remember { mutableStateOf<Long?>(null) }
                var editing by remember { mutableStateOf(false) }
                Column {
                    LineRow(title = "Singkong", detail = "50 kg", amount = amount.toRupiah(), onClick = { draft = amount; editing = true })
                }
                if (editing) {
                    LineEditSheet(
                        title = "Ubah bahan",
                        onDismiss = { editing = false },
                        onSave = { amount = draft ?: 0; editing = false },
                    ) {
                        MoneyInput(label = "Harga", value = draft, onValueChange = { draft = it })
                    }
                }
            }
        }
        compose.onNodeWithText("50 kg", substring = true).performClick()
        compose.onNodeWithText("Ubah bahan").assertExists()
        field().performTextReplacement("2000000")
        compose.onNodeWithText(text(R.string.action_save)).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(2_000_000L.toRupiah(), substring = true).assertExists()
        assertEquals(0, onAllNodesWithTextCount("Ubah bahan"))
    }

    @Test
    fun lineEditSheetAsksBeforeDeleting() {
        var deleted = 0
        compose.setContent {
            MankeuTheme {
                LineEditSheet(title = "Singkong", onDismiss = {}, onSave = {}, onDelete = { deleted++ }) { }
            }
        }
        compose.onNodeWithText(text(R.string.action_delete)).performClick()
        compose.waitForIdle()
        assertEquals(0, deleted)
        compose.onNodeWithText(text(R.string.line_delete_title)).assertExists()
        // Two "Hapus" now: the one in the first sheet and the confirmation; the confirmation is last.
        compose.onAllNodes(hasText(text(R.string.action_delete)) and hasClickAction()).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        compose.waitForIdle()
        assertEquals(1, deleted)
    }

    /**
     * Back goes through the Activity's OnBackPressedDispatcher, the path a real Back key or gesture
     * takes once delivered. Not Espresso.pressBack(): on emulator-5554 the only enabled input
     * method is Google voice typing (Gboard disabled), and it swallows every KEYCODE_BACK
     * ("#switchToLastInputMethod … Showing picker as fallback"), in system Settings too. See the
     * U1 report.
     */
    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun fullPageFormBackClosesWhenCleanAndAsksWhenDirty() {
        var closed = 0
        var dirty by mutableStateOf(false)
        compose.setContent {
            MankeuTheme {
                FullPageForm(title = "Belanja", dirty = dirty, onClose = { closed++ }, onSave = {}) {
                    ListRowField(label = "Bahan", value = "Singkong", onClick = {})
                }
            }
        }
        pressBack()
        assertEquals(1, closed)

        dirty = true
        compose.waitForIdle()
        pressBack()
        assertEquals(1, closed)
        compose.onNodeWithText(text(R.string.form_discard_title)).assertExists()
        compose.onNodeWithText(text(R.string.form_discard_confirm)).performClick()
        compose.waitForIdle()
        assertEquals(2, closed)
    }

    @Test
    fun listRowFieldIsAButtonForTalkBack() {
        var clicks = 0
        compose.setContent {
            MankeuTheme { ListRowField(label = "Pelanggan", value = "Bu Ani", onClick = { clicks++ }) }
        }
        val row = compose.onNodeWithText("Pelanggan", substring = true)
        row.assert(hasClickAction())
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        row.performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(clicks == 1)
    }


    private fun onAllNodesWithTextCount(value: String): Int =
        compose.onAllNodes(hasText(value)).fetchSemanticsNodes().size
}
