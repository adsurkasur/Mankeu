package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

        // Belanja, filled: the material's hint, its unit and the amount 10 × Rp25.000.
        exists("Jumlahnya menambah stok bahan ini")
        exists("Dihitung otomatis: 10 liter × Rp")
        // Nothing is recorded until the purchase is saved: no stock-only movement as well.
        assertEquals("0", stock())
    }

    @Test
    fun stockOnlyRecordsTheMoveWithoutMoney() {
        start()
        addTen()

        click("Tidak, tambah stok saja")

        compose.waitUntil(5_000) { stock() == "10" }
    }

    @Test
    fun minusStockSaysWhatToDo() {
        runBlocking { repository.recordStockMovement(minyak, StockMovementType.OUT, BigDecimal(3), LocalDate.now(), "") }
        start()
        click("Produk")
        exists("Stok minus: catat belanja atau hitung ulang stok.")
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
