package com.mankeu.presentation

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.data.ProductDraft
import com.mankeu.domain.StockItem
import com.mankeu.presentation.screens.ProductEditorScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

/** The full-page product editor (spec #5 #6 #13): recipe per batch, sale units, live HPP. */
@RunWith(AndroidJUnit4::class)
class ProductEditorTest {
    @get:Rule
    val compose = createComposeRule()

    private var saved: ProductDraft? = null
    private val oil = StockItem("minyak", "b", "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)

    /** Indonesian resources whatever the emulator's language, as the screen text is what is asserted. */
    private fun editor(materials: List<StockItem> = listOf(oil)): @Composable () -> Unit = {
        val host = LocalContext.current
        val indonesian = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag("id")) }
        val localized = remember(host) { host.createConfigurationContext(indonesian) }
        val wrapped = remember(host) { object : ContextWrapper(host) { override fun getResources(): Resources = localized.resources } }
        CompositionLocalProvider(LocalContext provides wrapped, LocalConfiguration provides indonesian) {
            MankeuTheme { ProductEditorScreen(existing = null, materials = materials, onSave = { saved = it }, onDelete = null) }
        }
    }

    /** Lazy rows below the fold are not composed until scrolled to. */
    private fun node(text: String, index: Int = 0, substring: Boolean = false): SemanticsNodeInteraction {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = substring))
        return compose.onAllNodesWithText(text, substring = substring)[index]
    }

    private fun fillKeripik() {
        node("Nama produk").performTextInput("Keripik")
        node("Satuan terkecil").performTextInput("bungkus")
        // Zero yield must not reach ProductCosting.hpp (division by zero): no preview, no crash.
        node("Satu batch menghasilkan berapa bungkus?").performTextInput("0")
        compose.onAllNodesWithText("Biaya 1 batch", substring = true).assertCountEquals0()
        node("Satu batch menghasilkan berapa bungkus?").performTextClearance()
        node("Satu batch menghasilkan berapa bungkus?").performTextInput("400")

        node("Tambah bahan").performClick()
        node("Jumlah (liter)").performTextInput("20")
        node("Tambah biaya lain").performClick()
        node("Biaya (mis. gas, upah)").performTextInput("Lain-lain")
        node("Biaya per batch").performTextInput("1900000")

        node("Harga jual").performTextInput("8000")
        node("Tambah satuan jual").performClick()
        node("Satuan", index = 1).performTextInput("dus")
        node("Isi (bungkus)", index = 1).performTextInput("40")
        node("Harga jual", index = 1).performTextInput("300000")
    }

    @Test fun recipePerBatchGivesHppPerSaleUnitAndSaves() {
        compose.setContent(editor())
        compose.onNodeWithText("Simpan").assertIsNotEnabled()
        fillKeripik()

        node("Biaya 1 batch Rp2.400.000", substring = true)
        node("HPP Rp240.000", substring = true)
        node("HPP Rp6.000 · untung Rp2.000", substring = true)
        compose.onNodeWithText("Simpan").assertIsEnabled().performClick()

        val draft = saved!!
        assertEquals(listOf("1", "40"), draft.units.map { it.contentText })
        assertEquals(listOf("bungkus", "dus"), draft.units.map { it.name })
        assertEquals("minyak", draft.recipe.first().stockItemId)
        assertEquals("20", draft.recipe.first().quantityText)
        assertEquals(1_900_000L, draft.recipe[1].amountRupiah)
        assertEquals("400", draft.batchYieldText)
    }

    @Test fun rowsSurviveRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent(editor())
        fillKeripik()
        restoration.emulateSavedInstanceStateRestore()

        node("Biaya 1 batch Rp2.400.000", substring = true)
        node("HPP Rp240.000", substring = true)
        compose.onNodeWithText("Simpan").performClick()
        assertEquals(listOf("bungkus", "dus"), saved!!.units.map { it.name })
        assertEquals("minyak", saved!!.recipe.first().stockItemId)
    }

    /** Final review M-6: a new material line starts on a material the recipe does not use yet. */
    @Test fun aNewMaterialLineStartsOnAnUnusedMaterial() {
        val cassava = StockItem("singkong", "b", "Singkong", "kg", 30_000, BigDecimal.ZERO, BigDecimal.ZERO)
        compose.setContent(editor(listOf(oil, cassava)))
        node("Nama produk").performTextInput("Keripik")
        node("Satuan terkecil").performTextInput("bungkus")
        node("Satu batch menghasilkan berapa bungkus?").performTextInput("400")
        node("Tambah bahan").performClick()
        node("Jumlah (liter)").performTextInput("20")
        node("Tambah bahan").performClick()
        node("Jumlah (kg)").performTextInput("50")
        node("Harga jual").performTextInput("8000")
        compose.onNodeWithText("Simpan").performClick()

        assertEquals(listOf("minyak", "singkong"), saved!!.recipe.map { it.stockItemId })
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals0() =
        assertEquals(0, fetchSemanticsNodes().size)
}
