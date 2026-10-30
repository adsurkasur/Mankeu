package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.ContactDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.data.ProductDraft
import com.mankeu.data.RecipeLineDraft
import com.mankeu.data.SaleDraft
import com.mankeu.data.SaleLineDraft
import com.mankeu.data.SaleUnitDraft
import com.mankeu.domain.SaleStatus
import com.mankeu.importer.FinanceWorkbookImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * Task 5 (Fase 2): the sales note through the real app on a real (in-memory) database. The
 * repository validates; the screens show its message and stay open.
 */
@RunWith(AndroidJUnit4::class)
class SaleNoteFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var projectId: String
    private lateinit var businessId: String
    private lateinit var keripik: String
    private lateinit var savedPrefs: Map<String, *>
    private val savedLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        savedPrefs = prefs.all.toMap()
        prefs.edit().clear().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name).commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        runBlocking {
            val (b, p) = repository.createBusinessWithUnit("Kedai Senja", "Utama")
            businessId = b
            projectId = p
            // HPP 6.000 per bungkus (2.400.000 / 400), 240.000 per dus of 40.
            keripik = repository.saveProduct(
                p, null,
                ProductDraft(
                    "Keripik", "bungkus", "400",
                    listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000), SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000)),
                    listOf(RecipeLineDraft("Bahan", amountRupiah = 2_400_000)),
                ),
            )
            // HPP 3.000 per gelas.
            repository.saveProduct(
                p, null,
                ProductDraft("Kopi", "gelas", "100", listOf(SaleUnitDraft(name = "gelas", contentText = "1", priceRupiah = 10_000)), listOf(RecipeLineDraft("Bahan", amountRupiah = 300_000))),
            )
        }
    }

    @After
    fun tearDown() {
        // Later tests in the same process read English labels from the target context.
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

    private fun start() {
        val vm = AppViewModel(
            repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "sale-ui.aabk")),
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

    @Test
    fun aNoteWithTwoProductsShowsTotalDiscountAndProfitAndAnUnpaidNoteNeedsACustomer() {
        start()
        clickDescription("Catat")
        click("Nota penjualan")
        exists("Nota penjualan baru")

        pick("Produk", "Keripik")
        pick("Satuan", "dus")
        type("Jumlah", "2")
        // 2 dus: Rp600.000, HPP 2 × 240.000.
        exists("Rp600.000 · HPP Rp480.000 · untung Rp120.000")

        click("Tambah produk")
        pick("Produk", "Kopi", index = 1)
        type("Jumlah", "3", index = 1)
        exists("Rp30.000 · HPP Rp9.000 · untung Rp21.000")

        type("Diskon", "30000")
        exists("Rp630.000")
        exists("Rp30.000")
        // Total 600.000; untung 600.000 − 489.000.
        exists("Rp111.000")

        click("Belum dibayar")
        click("Simpan")
        exists("Pilih pelanggan untuk nota yang belum lunas.")
        // Refused: the page stays open with the owner's input.
        exists("Nota penjualan baru")
        exists("Rp111.000")
        assertTrue(runBlocking { repository.sales(projectId).first() }.isEmpty())
    }

    @Test
    fun payingFromTheDetailChangesTheStatus() {
        runBlocking {
            val sari = repository.saveContact(businessId, null, ContactDraft("Bu Sari"))
            repository.saveSale(
                projectId, null,
                SaleDraft(sari, LocalDate.now(), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "2", 300_000)), paidNowRupiah = 0),
            )
        }
        start()
        click("Catatan")
        click("Bu Sari")
        exists("Belum dibayar")
        click("Bayar")
        exists("Bayar nota")
        type("Nominal", "200000")
        click("Simpan")
        exists("Sebagian")
        exists("Sisa Rp400.000")

        click("Bayar")
        // The amount starts on what is left.
        click("Simpan")
        exists("Lunas")
        assertEquals(SaleStatus.PAID, runBlocking { repository.sales(projectId).first() }.single().status)
    }

    // ---- helpers (same approach as ModuleScreenshotTest) ----------------------------------------

    private fun settle() {
        compose.waitForIdle()
        SystemClock.sleep(300)
        compose.waitForIdle()
    }

    private fun exists(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun click(text: String) {
        settle()
        var clickable = compose.onAllNodes(hasText(text) and hasClickAction())
        if (clickable.fetchSemanticsNodes().isEmpty()) clickable = compose.onAllNodes(hasText(text, substring = true) and hasClickAction())
        val count = clickable.fetchSemanticsNodes().size
        if (count > 0) {
            clickable[count - 1].performSemanticsAction(SemanticsActions.OnClick)
        } else {
            scrollTo(text)
            compose.onAllNodesWithText(text, useUnmergedTree = true).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        }
        settle()
    }

    private fun clickDescription(description: String) {
        settle()
        compose.onAllNodes(hasContentDescription(description) and hasClickAction())[0].performSemanticsAction(SemanticsActions.OnClick)
        settle()
    }

    private fun scrollTo(text: String) {
        val lists = compose.onAllNodes(hasScrollToIndexAction())
        val count = lists.fetchSemanticsNodes().size
        if (count > 0) runCatching { lists[count - 1].performScrollToNode(hasText(text, substring = true)) }
        settle()
    }

    private fun type(label: String, value: String, index: Int = 0) {
        settle()
        scrollTo(label)
        val node = compose.onAllNodes(hasSetTextAction() and hasText(label, substring = true))[index]
        node.fetchSemanticsNode().config[SemanticsActions.SetText].action?.invoke(AnnotatedString(value))
        settle()
    }

    private fun pick(fieldLabel: String, option: String, index: Int = 0) {
        settle()
        scrollTo(fieldLabel)
        compose.onAllNodes(hasText(fieldLabel, substring = true) and SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))[index].performClick()
        settle()
        compose.onAllNodesWithText(option, useUnmergedTree = true).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        settle()
    }
}
