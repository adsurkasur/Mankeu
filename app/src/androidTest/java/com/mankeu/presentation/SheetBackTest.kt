package com.mankeu.presentation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.data.BusinessEntity
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.screens.ManageScopeSheet
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cleanup P6: the system Back closes an open sheet before anything else (DEC-027 turned off sheet gestures). */
@RunWith(AndroidJUnit4::class)
class SheetBackTest {
    @get:Rule
    val compose = createComposeRule()

    private var dismissed = false

    @Test
    fun backClosesTheBusinessAndUnitSheet() {
        compose.setContent {
            MankeuTheme {
                ManageScopeSheet(
                    // With no business the sheet opens "Tambah usaha" on top, which Back would close first.
                    state = AppUiState(businesses = listOf(BusinessEntity(id = "b", name = "Keripik Bu Sri", createdAtEpochMillis = 0))),
                    onDismiss = { dismissed = true },
                    onSelectBusiness = {}, onSelectProject = {}, onCreateBusiness = {}, onCreateProject = {},
                    onRenameBusiness = {}, onRemoveBusiness = {}, onUpdateProject = { _, _, _, _, _ -> }, onRemoveProject = {},
                )
            }
        }
        compose.waitForIdle()

        Espresso.pressBackUnconditionally()
        compose.waitForIdle()

        assertTrue(dismissed)
    }

    @Test
    fun backClosesAShortSheet() {
        compose.setContent { MankeuTheme { MankeuSheet(title = "Produksi", onDismiss = { dismissed = true }) { Text("isi") } } }
        compose.waitForIdle()

        Espresso.pressBackUnconditionally()
        compose.waitForIdle()

        assertTrue(dismissed)
    }
}
