package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.ContactDraft
import com.mankeu.data.ContactEntity
import com.mankeu.presentation.screens.ContactSheet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactValidationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val saved = mutableListOf<ContactDraft>()
    private fun t(id: Int, vararg args: Any) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
    private fun open() {
        val other = ContactEntity("c", "b", "Toko A", "toko a", createdAtEpochMillis = 0)
        compose.setContent { MankeuTheme { ContactSheet(null, listOf(other), null, "", {}, { saved += it }, null) } }
    }
    private fun field(id: Int) = compose.onNode(hasSetTextAction() and hasText(t(id), substring = true))
    @Test fun nameErrorAppearsAfterBlurAndClears() {
        open()
        field(R.string.contact_name).performClick()
        field(R.string.contact_phone).performClick()
        compose.onNodeWithText(t(R.string.form_need_name)).assertExists()
        field(R.string.contact_name).performTextInput("Baru")
        compose.onNodeWithText(t(R.string.form_need_name)).assertDoesNotExist()
        assertEquals(0, saved.size)
    }
    @Test fun duplicateIsReportedAfterBlurAndSaveFocusesName() {
        open()
        field(R.string.contact_name).performTextInput("Toko A")
        compose.onNodeWithText(t(R.string.contact_duplicate, "Toko A")).assertDoesNotExist()
        field(R.string.contact_phone).performClick()
        compose.onNodeWithText(t(R.string.contact_duplicate, "Toko A")).assertExists()
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        field(R.string.contact_name).assertIsFocused().performTextReplacement("Toko B")
        compose.onNodeWithText(t(R.string.contact_duplicate, "Toko A")).assertDoesNotExist()
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        assertEquals("Toko B", saved.single().name)
    }
    @Test fun saveEmptyFocusesName() {
        open()
        compose.onNodeWithText(t(R.string.action_save)).performClick()
        field(R.string.contact_name).assertIsFocused()
        compose.onNodeWithText(t(R.string.form_need_name)).assertExists()
        assertEquals(0, saved.size)
    }
}
