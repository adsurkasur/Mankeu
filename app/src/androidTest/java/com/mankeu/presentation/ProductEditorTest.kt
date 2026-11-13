package com.mankeu.presentation

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.data.ProductDraft
import com.mankeu.domain.StockItem
import com.mankeu.presentation.screens.ProductEditorScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Locale

/**
 * The full-page product editor (spec #5 #6 #13, U5 audit L08): recipe and sale units as compact
 * rows edited in short sheets, what one batch makes, live HPP above Simpan, plan fields only from
 * Rencana usaha.
 */
@RunWith(AndroidJUnit4::class)
class ProductEditorTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var saved: ProductDraft? = null
    private val oil = StockItem("minyak", "b", "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
    private val cassava = StockItem("singkong", "b", "Singkong", "kg", 30_000, BigDecimal.ZERO, BigDecimal.ZERO)
    private val savedLocale: Locale = Locale.getDefault()

    /** Indonesian resources for the page and its sheets (sheets read the Activity's own resources). */
    private fun indonesian(locale: Locale) {
        Locale.setDefault(locale)
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply { setLocale(locale) }
            @Suppress("DEPRECATION")
            compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        }
    }

    @Before fun setUp() = indonesian(Locale.forLanguageTag("id"))

    @After fun tearDown() = indonesian(savedLocale)

    private fun open(materials: List<StockItem> = listOf(oil), showPlan: Boolean = false) {
        compose.setContent {
            MankeuTheme { ProductEditorScreen(existing = null, materials = materials, onSave = { saved = it }, onDelete = null, title = "Produk baru", showPlan = showPlan) }
        }
        compose.waitForIdle()
    }

    private fun box(label: String, index: Int = 0) = compose.onAllNodes(hasSetTextAction() and hasText(label, substring = true))[index]

    private fun click(text: String) {
        compose.waitForIdle()
        val nodes = compose.onAllNodes(hasText(text, substring = true) and hasClickAction())
        nodes[nodes.fetchSemanticsNodes().size - 1].performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun exists(text: String) = assertEquals(text, true, compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty())

    private fun saveButton() = compose.onAllNodes(hasText("Simpan") and hasClickAction())[0]

    private fun fillKeripik() {
        box("Nama produk").performTextInput("Keripik")
        // Zero yield must not reach ProductCosting.hpp (division by zero): no preview, no crash.
        box("Satu kali produksi menghasilkan").performTextInput("0")
        assertEquals(0, compose.onAllNodesWithText("Modal barang Rp", substring = true).fetchSemanticsNodes().size)
        box("Satu kali produksi menghasilkan").performTextClearance()
        box("Satu kali produksi menghasilkan").performTextInput("400")

        click("+ Bahan")
        box("Jumlah (liter)").performTextInput("20")
        click("Simpan")
        click("+ Biaya lain")
        box("Biaya (mis. gas, upah)").performTextInput("Lain-lain")
        box("Biaya untuk sekali produksi").performTextInput("1900000")
        click("Simpan")

        click("bungkus")
        box("Harga jual").performTextInput("8000")
        click("Simpan")
        click("+ Satuan jual")
        box("Satuan").performTextInput("dus")
        box("Isi").performTextClearance()
        box("Isi").performTextInput("40")
        box("Harga jual").performTextInput("300000")
        click("Simpan")
    }

    @Test fun recipePerBatchGivesHppPerSaleUnitAndSaves() {
        open()
        // On open: name and yield are the only boxes; recipe and units are rows and buttons.
        assertEquals(2, compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        saveButton().assertIsEnabled()
        fillKeripik()

        exists("Minyak · 20 liter")
        exists("Rp500.000")
        exists("Modal barang Rp6.000 per bungkus")
        exists("Biaya satu kali produksi Rp2.400.000")
        exists("dus · isi 40 bungkus · untung Rp60.000")
        exists("bungkus · untung Rp2.000")
        saveButton().assertIsEnabled()
        saveButton().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        val draft = saved!!
        assertEquals(listOf("1", "40"), draft.units.map { it.contentText })
        assertEquals(listOf("bungkus", "dus"), draft.units.map { it.name })
        assertEquals("minyak", draft.recipe.first().stockItemId)
        assertEquals("20", draft.recipe.first().quantityText)
        assertEquals(1_900_000L, draft.recipe[1].amountRupiah)
        assertEquals("400", draft.batchYieldText)
        assertEquals("bungkus", draft.baseUnit)
    }

    @Test fun customBaseUnitAlsoNamesTheFirstSaleUnit() {
        open()
        box("Nama produk").performTextInput("Keripik")
        box("Satu kali produksi menghasilkan").performTextInput("10")
        click("Tulis satuan sendiri")
        box("Satuan terkecil").performTextInput("karung")
        click("karung")
        box("Harga jual").performTextInput("8000")
        click("Simpan")
        saveButton().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals("karung", saved!!.baseUnit)
        assertEquals("karung", saved!!.units.first().name)
    }

    /** Final review M-6: a new material line starts on a material the recipe does not use yet. */
    @Test fun aNewMaterialLineStartsOnAnUnusedMaterial() {
        open(listOf(oil, cassava))
        box("Nama produk").performTextInput("Keripik")
        box("Satu kali produksi menghasilkan").performTextInput("400")
        click("+ Bahan")
        box("Jumlah (liter)").performTextInput("20")
        click("Simpan")
        click("+ Bahan")
        box("Jumlah (kg)").performTextInput("50")
        click("Simpan")
        click("bungkus")
        box("Harga jual").performTextInput("8000")
        click("Simpan")
        saveButton().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals(listOf("minyak", "singkong"), saved!!.recipe.map { it.stockItemId })
    }

    /** U5: target and sell-from month are Rencana usaha's; the editor shows them only when opened from there. */
    @Test fun planFieldsOnlyFromRencanaUsaha() {
        open()
        assertEquals(0, compose.onAllNodesWithText("Target terjual", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun planFieldsShowWhenOpenedFromThePlan() {
        open(showPlan = true)
        exists("Target terjual")
    }
}
