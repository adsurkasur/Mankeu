package com.mankeu.presentation

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductMovementEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductSaleRow
import com.mankeu.data.ProductUnitEntity
import com.mankeu.presentation.screens.StockHistoryScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

/** Cleanup P1: Riwayat stok shows production, counts and sales; production and counts can be deleted after a confirmation. */
@RunWith(AndroidJUnit4::class)
class StockHistoryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val deleted = mutableListOf<String>()

    private val record = ProductRecord(
        ProductEntity(id = "k", projectId = "p", name = "Keripik", unit = "bungkus", batchYieldText = "400", createdAtEpochMillis = 0),
        units = listOf(ProductUnitEntity("u1", "k", "bungkus", "1", 8_000, 0), ProductUnitEntity("u2", "k", "dus", "40", 300_000, 1)),
        recipe = emptyList(),
        movements = listOf(
            ProductMovementEntity("m1", "k", "PRODUCTION", "400", "1", dateEpochDay = LocalDate.of(2026, 10, 1).toEpochDay(), createdAtEpochMillis = 1),
            ProductMovementEntity("m2", "k", "ADJUSTMENT", "300", dateEpochDay = LocalDate.of(2026, 10, 3).toEpochDay(), createdAtEpochMillis = 3),
        ),
        sales = listOf(ProductSaleRow("s1", "k", "2", "dus", dateEpochDay = LocalDate.of(2026, 10, 2).toEpochDay(), createdAtEpochMillis = 2)),
    )

    private fun show() = compose.setContent {
        val host = LocalContext.current
        val indonesian = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag("id")) }
        val localized = remember(host) { host.createConfigurationContext(indonesian) }
        val wrapped = remember(host) { object : ContextWrapper(host) { override fun getResources(): Resources = localized.resources } }
        CompositionLocalProvider(LocalContext provides wrapped, LocalConfiguration provides indonesian) {
            MankeuTheme { StockHistoryScreen(record = record, onDelete = { deleted += it }) }
        }
    }

    @Test
    fun historyListsEveryLineAndOnlyProductionAndCountsCanBeDeleted() {
        show()

        compose.onNodeWithText("Produksi 1 batch").assertExists()
        compose.onNodeWithText("+400 bungkus").assertExists()
        compose.onNodeWithText("Terjual 2 dus").assertExists()
        compose.onNodeWithText("-80 bungkus").assertExists()
        compose.onNodeWithText("Ubah atau hapus penjualan di Catatan.").assertExists()
        compose.onNodeWithText("Hitung stok").assertExists()
        compose.onNodeWithText("Dihitung 300 bungkus").assertExists()
        compose.onNodeWithText("Stok jadi 320 bungkus").assertExists()
        // Production and the count, not the sale.
        compose.onAllNodesWithContentDescription("Hapus catatan stok").assertCountEquals(2)
    }

    @Test
    fun deletingAsksFirstAndThenDeletesThatLine() {
        show()

        // Newest first: the count (3 Oct) is the first delete button.
        compose.onAllNodesWithContentDescription("Hapus catatan stok")[0].performClick()
        // A dialog is composed with the Activity's own resources, not the test's Indonesian override.
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        compose.onNodeWithText(resources.getString(R.string.action_cancel)).performClick()
        assertEquals(emptyList<String>(), deleted)

        compose.onAllNodesWithContentDescription("Hapus catatan stok")[1].performClick()
        compose.onNodeWithText(resources.getString(R.string.action_delete)).performClick()
        assertEquals(listOf("m1"), deleted)
    }
}
