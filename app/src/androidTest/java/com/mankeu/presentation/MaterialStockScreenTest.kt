package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.domain.StockMovementType
import com.mankeu.importer.FinanceWorkbookImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

/**
 * U3, audit L07: adding material stock by hand asks "Catat juga uangnya?"; yes opens Belanja filled with
 * the material and quantity (the purchase then adds the stock), no records the stock only. Minus stock says what to do.
 */
@RunWith(AndroidJUnit4::class)
class MaterialStockScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var savedPrefs: Map<String, *>
    private val savedLocale: Locale = Locale.getDefault()
    private lateinit var businessId: String
    private lateinit var projectId: String
    private lateinit var minyak: String

    @Before
    fun setUp() {
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        savedPrefs = prefs.all.toMap()
        prefs.edit().clear().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name).commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        runBlocking {
            val (b, p) = repository.createBusinessWithUnit("Keripik Bu Sri", "Umum")
            businessId = b
            projectId = p
            minyak = repository.createStockItem(b, p, false, "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }

    @After
    fun tearDown() {
        Locale.setDefault(savedLocale)
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply { setLocale(savedLocale) }
            @Suppress("DEPRECATION")
            compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        }
        database.close()
        context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE).edit().clear().apply {
            savedPrefs.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                }
            }
        }.commit()
    }

    @Test
    fun addingStockByHandOffersTheMoneyAndOpensAFilledPurchase() {
        start()
        addTen()

        exists("Catat juga uangnya?")
        click("Ya, catat belanja")

        // U5: the Belanja page, filled: Minyak, 10 liter at Rp25.000, the total and the stock it adds.
        exists("Beli apa?")
        exists("Total Rp250.000")
        exists("Stok Minyak +10 liter")
        // Nothing is recorded until the purchase is saved: no stock-only movement as well.
        assertEquals("0", stock())
        click("Simpan")
        compose.waitUntil(5_000) { stock() == "10" }
        exists("Belanja dicatat. Stok Minyak +10 liter")
    }

    @Test
    fun stockQuantityShowsErrorOnBlurAndClearsWhenValid() {
        start()
        click("Produk")
        click("Minyak")
        val quantity = compose.onAllNodes(hasSetTextAction() and hasText("Jumlah"))[0]
        quantity.performClick()
        compose.onAllNodes(hasSetTextAction() and hasText("Catatan (opsional)"))[0].performClick()
        exists("Isi jumlah lebih dari 0 dulu.")
        quantity.performTextInput("3")
        org.junit.Assert.assertTrue(compose.onAllNodesWithText("Isi jumlah lebih dari 0 dulu.", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertEquals("0", stock())
    }

    @Test
    fun emptyStockQuantityIsExplainedOnlyAfterSave() {
        start()
        click("Produk")
        click("Minyak")
        org.junit.Assert.assertTrue(compose.onAllNodesWithText("Isi jumlah lebih dari 0 dulu.", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        click("Simpan")
        exists("Isi jumlah lebih dari 0 dulu.")
        compose.onAllNodes(hasSetTextAction() and hasText("Jumlah"))[0].assertIsFocused().performTextInput("10")
        org.junit.Assert.assertTrue(compose.onAllNodesWithText("Isi jumlah lebih dari 0 dulu.", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertEquals("0", stock())
    }

    @Test
    fun stockCountAndUseChoicesSaveTheirOwnMovements() {
        start()
        click("Produk")
        click("Minyak")
        click("Hitung ulang")
        compose.onAllNodes(hasSetTextAction() and hasText("Jumlah fisik hasil hitung"))[0].performTextInput("7")
        click("Simpan")
        compose.waitUntil(5_000) { stock() == "7" }
        click("Minyak")
        click("Pakai")
        compose.onAllNodes(hasSetTextAction() and hasText("Jumlah"))[0].performTextInput("2")
        click("Simpan")
        compose.waitUntil(5_000) { stock() == "5" }
    }

    @Test
    fun stockQuestionKeepsHorizontalMarginOnPhone() {
        start()
        addTen()
        val dialog = compose.onNode(androidx.compose.ui.test.hasTestTag("stock_money_dialog"), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInWindow
        val metrics = context.resources.displayMetrics
        org.junit.Assert.assertTrue("Dialog width ${dialog.width}, screen ${metrics.widthPixels}",
            dialog.width <= metrics.widthPixels - 32 * metrics.density)
    }

    @Test
    fun stockQuestionPlacesChoicesAndBackInSeparateRows() {
        start()
        addTen()
        val buy = compose.onAllNodesWithText("Ya, catat belanja")[0].fetchSemanticsNode().boundsInRoot
        val stock = compose.onAllNodesWithText("Tidak, tambah stok saja")[0].fetchSemanticsNode().boundsInRoot
        val back = compose.onAllNodesWithText("Kembali")[0].fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(buy.bottom <= stock.top && stock.bottom <= back.top)
    }

    @Test
    fun stockOnlyRecordsTheMoveWithoutMoney() {
        start()
        addTen()

        click("Tidak, tambah stok saja")

        compose.waitUntil(5_000) { stock() == "10" }
    }

    /** U3 review M-6: going back from "Catat juga uangnya?" returns to the sheet with the typed quantity. */
    @Test
    fun goingBackFromTheMoneyQuestionKeepsTheTypedQuantity() {
        start()
        addTen()
        exists("Catat juga uangnya?")
        click("Kembali")
        exists("Catat Stok: Minyak")
        val field = compose.onAllNodes(hasSetTextAction() and hasText("Jumlah"))[0]
        assertEquals("10", field.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
        assertEquals("0", stock())
    }

    /** U5, audit L06: Tambah bahan shows name and price as boxes, with a unit selector and optional stock fields. */
    @Test
    fun tambahBahanTypesTwoBoxesAndPicksTheUnit() {
        start()
        click("Produk")
        click("Tambah Barang")
        exists("Satuan")
        assertEquals(2, compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        compose.onAllNodes(hasSetTextAction() and hasText("Nama barang"))[0].performTextInput("Gula")
        click("Satuan")
        click("kg")
        compose.onAllNodes(hasSetTextAction() and hasText("Harga beli per kg"))[0].performTextInput("15.000")
        click("Simpan")
        compose.waitUntil(5_000) { runBlocking { repository.stockItems(businessId, projectId).first() }.any { it.name == "Gula" } }
        val gula = runBlocking { repository.stockItems(businessId, projectId).first() }.single { it.name == "Gula" }
        assertEquals("kg", gula.unit)
        assertEquals(15_000L, gula.basePriceRupiah)
    }

    @Test
    fun customMaterialUnitIsClearlyOfferedAndSaved() {
        start()
        click("Produk")
        click("Tambah Barang")
        compose.onAllNodes(hasSetTextAction() and hasText("Nama barang"))[0].performTextInput("Gula")
        compose.onNodeWithContentDescription("Tulis satuan sendiri").performClick()
        compose.onAllNodes(hasSetTextAction() and hasText("Nama satuan"))[0].performTextInput("karung")
        click("Simpan")
        compose.waitUntil(5_000) { runBlocking { repository.stockItems(businessId, projectId).first() }.any { it.name == "Gula" } }
        assertEquals("karung", runBlocking { repository.stockItems(businessId, projectId).first() }.single { it.name == "Gula" }.unit)
    }

    @Test
    fun minusStockSaysWhatToDo() {
        runBlocking { repository.recordStockMovement(minyak, StockMovementType.OUT, BigDecimal(3), LocalDate.now(), "") }
        start()
        click("Produk")
        exists("Stok minus: catat belanja atau hitung ulang stok.")
    }

    /** Controller ruling (U3 review): existing stock is not back-filled, so the Bahan section says once to check it. */
    @Test
    fun theOneTimeNoticeIsShownUntilDismissed() {
        start()
        click("Produk")
        exists("Stok bahan sekarang dihitung otomatis")
        click("Mengerti")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Stok bahan sekarang dihitung otomatis", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        assertEquals(true, context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE).getBoolean("material_stock_notice_seen", false))
    }

    private fun addTen() {
        click("Produk")
        click("Minyak")
        exists("Catat Stok: Minyak")
        compose.onAllNodes(hasSetTextAction() and hasText("Jumlah"))[0].performTextInput("10")
        click("Simpan")
    }

    private fun stock() = runBlocking { repository.stockItems(businessId, projectId).first().single().currentStock.stripTrailingZeros().toPlainString() }

    private fun start() {
        val vm = AppViewModel(
            repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "material-stock.aabk")),
            GoogleIdentity(context), GoogleDriveBackupClient(),
        )
        val indonesian = Locale.forLanguageTag("id")
        Locale.setDefault(indonesian)
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply { setLocale(indonesian) }
            @Suppress("DEPRECATION")
            compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        }
        compose.setContent { MankeuApp(vm) }
        settle()
    }

    private fun settle() {
        compose.waitForIdle()
        SystemClock.sleep(300)
        compose.waitForIdle()
    }

    private fun exists(text: String) {
        scrollTo(text)
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(text: String) {
        settle()
        scrollTo(text)
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        val clickable = compose.onAllNodes(hasText(text) and hasClickAction())
        clickable[clickable.fetchSemanticsNodes().size - 1].performSemanticsAction(SemanticsActions.OnClick)
        settle()
    }

    private fun scrollTo(text: String) {
        val lists = compose.onAllNodes(hasScrollToIndexAction())
        val count = lists.fetchSemanticsNodes().size
        if (count > 0) runCatching { lists[count - 1].performScrollToNode(hasText(text, substring = true)) }
        settle()
    }
}
