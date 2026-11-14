package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.CategoryEntity
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.StockItem
import com.mankeu.presentation.screens.MoneyEntry
import com.mankeu.presentation.screens.MoneyFormMode
import com.mankeu.presentation.screens.MoneyFormScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * U5, audit L16 L17: the four money pages. Each opens with at most four fields (P2), Belanja starts
 * on the material last bought with its price filled and says what the stock becomes, there is no
 * "Sifat biaya", and Back with something typed asks "Buang perubahan?".
 */
@RunWith(AndroidJUnit4::class)
class MoneyFormsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun t(id: Int, vararg args: Any) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun category(id: String, role: CategoryRole, name: String) =
        CategoryEntity(id = id, businessId = "b", role = role.name, name = name, normalizedName = name.lowercase(), createdAtEpochMillis = 0)

    private val bahan = category("c-bahan", CategoryRole.EXPENSE, "Bahan Baku & Stok")
    private val gas = category("c-gas", CategoryRole.EXPENSE, "Gas")
    private val lain = category("c-lain", CategoryRole.REVENUE, "Pendapatan Lain")
    private val modal = category("c-modal", CategoryRole.CAPITAL_IN, "Setoran Modal Pemilik")
    private val prive = category("c-prive", CategoryRole.DRAWING, "Ambil untuk Pribadi")
    private val singkong = StockItem("singkong", "b", "Singkong", "kg", 3_000, BigDecimal.ZERO, BigDecimal.ZERO)
    private val minyak = StockItem("minyak", "b", "Minyak", "liter", 26_000, BigDecimal.ZERO, BigDecimal.ZERO)
    private val lastMinyak = MoneyEvent(
        id = "e1", businessId = "b", projectId = "p", type = FinancialEventType.CASH_EXPENSE, amountRupiah = 52_000,
        date = LocalDate.now().minusDays(2), description = "Minyak 2 liter", category = bahan.name, categoryId = bahan.id,
        quantityText = "2", unit = "liter", unitPriceRupiah = 26_000, stockItemId = minyak.id,
    )
    private val state = AppUiState(
        categories = listOf(bahan, gas, lain, modal, prive),
        stockItems = listOf(singkong, minyak),
        events = listOf(lastMinyak),
    )

    private val saved = mutableListOf<MoneyEntry>()
    private var closed = 0

    private fun open(mode: MoneyFormMode, existing: MoneyEvent? = null) {
        compose.setContent {
            MankeuTheme {
                MoneyFormScreen(mode = mode, state = state, existing = existing, prefill = null, onSave = { saved += it }, onClose = { closed++ }, onCreateCategory = { _, _ -> })
            }
        }
        compose.waitForIdle()
    }

    private fun boxes() = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size

    private fun box(label: String) = compose.onNode(hasSetTextAction() and hasText(label, substring = true))

    private fun save() = compose.onAllNodes(hasText(t(R.string.action_save)) and hasClickAction()).let { it[0] }.performClick()

    @Test
    fun belanjaStartsOnTheLastMaterialWithItsPriceAndSaysWhatTheStockBecomes() {
        open(MoneyFormMode.PURCHASE)
        compose.onNodeWithText("Minyak", substring = true).assertExists()
        // Beli apa? (row) + quantity + price: two boxes, nothing about cost nature.
        assertEquals(2, boxes())
        assertEquals(0, compose.onAllNodesWithText("Sifat biaya", substring = true).fetchSemanticsNodes().size)

        box(t(R.string.purchase_quantity)).performTextInput("10")
        compose.onNodeWithText(t(R.string.sale_result_total, 260_000L.toRupiah())).assertExists()
        compose.onNodeWithText(t(R.string.purchase_stock_up, "Minyak", "10", "liter")).assertExists()
        save()
        compose.waitForIdle()

        val entry = saved.single()
        assertEquals(minyak.id, entry.stockItemId)
        assertEquals(260_000L, entry.amountRupiah)
        assertEquals("10", entry.quantityText)
        assertEquals(26_000L, entry.unitPriceRupiah)
        assertEquals(bahan.id, entry.category.id)
        assertEquals(FinancialEventType.CASH_EXPENSE, entry.type)
        assertEquals("Minyak 10 liter", entry.description)
    }

    @Test
    fun legacyMaterialPurchaseRequiresExplicitCategoryWithoutChangingAmounts() {
        open(MoneyFormMode.PURCHASE, lastMinyak.copy(categoryId = null, category = "Kategori lama"))
        save()
        compose.waitForIdle()
        assertTrue(saved.isEmpty())
        compose.onNodeWithText("Gas").performClick()
        save()
        compose.waitForIdle()
        assertEquals(gas.id, saved.single().category.id)
        assertEquals(52_000L, saved.single().amountRupiah)
        assertEquals("2", saved.single().quantityText)
        assertEquals(minyak.id, saved.single().stockItemId)
    }

    @Test
    fun legacyExpenseWithoutMaterialRequiresExplicitCategory() {
        open(MoneyFormMode.OTHER_OUT, lastMinyak.copy(categoryId = null, category = "Kategori lama", stockItemId = null))
        save()
        compose.waitForIdle()
        assertTrue(saved.isEmpty())
        compose.onNodeWithText("Gas").performClick()
        save()
        compose.waitForIdle()
        assertEquals(gas.id, saved.single().category.id)
        assertEquals(52_000L, saved.single().amountRupiah)
    }

    @Test
    fun belanjaOfSomethingElseIsOneAmountAndACategoryChip() {
        open(MoneyFormMode.PURCHASE)
        compose.onAllNodes(hasText(t(R.string.purchase_what)) and hasClickAction())[0].performSemanticsAction(SemanticsActions.OnClick)
        compose.onAllNodes(hasText(t(R.string.purchase_not_material)) and hasClickAction()).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        compose.waitForIdle()
        assertEquals(1, boxes())
        compose.onNodeWithText("Gas").performClick()
        box(t(R.string.entry_amount)).performTextInput("100000")
        save()
        compose.waitForIdle()
        val entry = saved.single()
        assertEquals(null, entry.stockItemId)
        assertEquals(gas.id, entry.category.id)
        assertEquals(100_000L, entry.amountRupiah)
    }

    @Test
    fun uangMasukLainHasFourFieldsAtMost() {
        open(MoneyFormMode.OTHER_IN)
        // Amount and note are boxes; category chips and the date row are picked.
        assertEquals(2, boxes())
        compose.onNodeWithText(t(R.string.entry_date)).assertExists()
        box(t(R.string.entry_amount)).performTextInput("50000")
        save()
        compose.waitForIdle()
        assertEquals(lain.id, saved.single().category.id)
        assertEquals(FinancialEventType.CASH_INCOME, saved.single().type)
    }

    @Test
    fun ambilPribadiIsASegmentNotACategory() {
        open(MoneyFormMode.CAPITAL)
        assertEquals(2, boxes())
        assertEquals(0, compose.onAllNodesWithText(prive.name).fetchSemanticsNodes().size)
        compose.onNodeWithText(t(R.string.capital_out)).performClick()
        box(t(R.string.entry_amount)).performTextInput("200000")
        save()
        compose.waitForIdle()
        assertEquals(prive.id, saved.single().category.id)
        assertEquals(FinancialEventType.OWNER_DRAWING, saved.single().type)
    }

    @Test
    fun backClosesWhenCleanAndAsksWhenSomethingWasTyped() {
        open(MoneyFormMode.OTHER_OUT)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(1, closed)
    }

    @Test
    fun backAsksBeforeDroppingWhatWasTyped() {
        open(MoneyFormMode.OTHER_OUT)
        box(t(R.string.entry_amount)).performTextReplacement("75000")
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(0, closed)
        compose.onNodeWithText(t(R.string.form_discard_title)).assertExists()
        compose.onNodeWithText(t(R.string.form_discard_confirm)).performClick()
        compose.waitForIdle()
        assertTrue(closed == 1)
    }
}
