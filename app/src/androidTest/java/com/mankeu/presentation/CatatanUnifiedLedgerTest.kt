package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.ContactDraft
import com.mankeu.data.EventDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.data.ProductDraft
import com.mankeu.data.RecipeLineDraft
import com.mankeu.data.SaleDraft
import com.mankeu.data.SaleLineDraft
import com.mankeu.data.SaleUnitDraft
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
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
import java.time.LocalDate
import java.util.Locale

/**
 * Paket B (U6): Tests for the unified Catatan ledger, filter chips, single FAB choice sheet,
 * and L14 audit bug resolution.
 */
@RunWith(AndroidJUnit4::class)
class CatatanUnifiedLedgerTest {
    @Test fun productionPickerAllowsDuplicateProductLabelsAndRecordsTheSelectedId() {
        var second = ""
        runBlocking {
            second = repository.saveProduct(projectId, null, ProductDraft("Keripik", "bungkus", "1",
                listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 1_000)), emptyList()))
            val book = repository.books(projectId).first().first { it.mode == BookMode.REALIZATION.name }.id
            repository.recordEvent(EventDraft(businessId, projectId, book, FinancialEventType.CASH_INCOME,
                1_000, LocalDate.now(), "Pemasukan uji", "Pendapatan"))
        }
        start()
        click("Produksi")
        exists("Produksi apa?")
        val records = runBlocking { repository.products(projectId).first() }
        val secondIndex = records.indexOfFirst { it.product.id == second }
        assertEquals(2, compose.onAllNodesWithText("Keripik").fetchSemanticsNodes().size)
        compose.onAllNodesWithText("Keripik")[secondIndex].performClick()
        settle()
        click("Simpan")
        compose.waitUntil(5_000) { runBlocking { repository.products(projectId).first().single { it.product.id == second }.movements.isNotEmpty() } }
        runBlocking { assertEquals(0, repository.products(projectId).first().single { it.product.id == keripik }.movements.size) }
    }
    @Test fun oldFilteredCashRowLoadsAndSavesTheOriginalEventId() {
        var oldId = ""
        runBlocking {
            val book = repository.books(projectId).first().first { it.mode == BookMode.REALIZATION.name }.id
            val cats = database.financeDao().getCategories(businessId)
            val income = cats.first { it.role == "REVENUE" }
            val expense = cats.first { it.role == "EXPENSE" }
            repository.inTransaction {
                repeat(200) { repository.recordEvent(EventDraft(businessId, projectId, book, FinancialEventType.CASH_INCOME,
                    1_000, LocalDate.now(), "Pemasukan baru", income.name, income.id)) }
                oldId = repository.recordEvent(EventDraft(businessId, projectId, book, FinancialEventType.CASH_EXPENSE,
                    2_000, LocalDate.now().minusDays(1), "Biaya riwayat unik", expense.name, expense.id))
            }
        }
        start()
        click("Catatan")
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Biaya riwayat unik")
        settle()
        click("Muat catatan lebih lama")
        click("Biaya riwayat unik")
        compose.onAllNodes(hasText("Jumlah uang") and hasSetTextAction())[0].performTextReplacement("3500")
        click("Simpan")
        compose.waitUntil(5_000) { runBlocking { database.financeDao().getEvent(oldId)?.amountRupiah == 3_500L } }
        runBlocking {
            assertEquals(201, database.financeDao().countEventsForProject(projectId))
            assertEquals("Biaya riwayat unik", database.financeDao().getEvent(oldId)!!.description)
        }
    }
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
            keripik = repository.saveProduct(
                p, null,
                ProductDraft(
                    "Keripik", "bungkus", "400",
                    listOf(
                        SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000),
                        SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000),
                    ),
                    listOf(RecipeLineDraft("Bahan", amountRupiah = 2_400_000)),
                ),
            )
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

    private fun start() {
        val vm = AppViewModel(
            repository,
            FinanceWorkbookImporter(),
            EncryptedBackupManager(database, File(context.cacheDir, "catatan-ui.aabk")),
            GoogleIdentity(context),
            GoogleDriveBackupClient(),
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
    fun unifiedCatatanDisplaysNotesAndMoneyEventsTogether() {
        runBlocking {
            val tokoA = repository.saveContact(businessId, null, ContactDraft("Toko A"))
            repository.saveSale(
                projectId, null,
                SaleDraft(tokoA, LocalDate.now(), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "2", 300_000)), paidNowRupiah = 0),
            )
            val book = repository.books(projectId).first().first { it.mode == BookMode.REALIZATION.name }
            val cats = database.financeDao().getCategories(businessId)
            val expenseCat = cats.first { it.name.contains("Bahan", ignoreCase = true) }
            repository.recordEvent(
                EventDraft(
                    businessId = businessId,
                    projectId = projectId,
                    bookId = book.id,
                    type = FinancialEventType.CASH_EXPENSE,
                    amountRupiah = 260_000,
                    date = LocalDate.now(),
                    description = "Minyak 10 liter",
                    category = expenseCat.name,
                    categoryId = expenseCat.id,
                ),
            )
        }

        start()
        click("Catatan")

        // Unpaid sale note row
        exists("Toko A")
        exists("Rp600.000")
        exists("Belum dibayar")

        // Money expense row
        exists("Minyak 10 liter")
        exists("260.000")
    }

    @Test
    fun l14AuditFix_havingOnlyNotesDoesNotShowEmptyCard() {
        runBlocking {
            val tokoA = repository.saveContact(businessId, null, ContactDraft("Toko A"))
            repository.saveSale(
                projectId, null,
                SaleDraft(tokoA, LocalDate.now(), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "1", 300_000)), paidNowRupiah = 0),
            )
        }

        start()
        click("Catatan")

        // Should NOT display "Belum ada catatan" empty state
        settle()
        val emptyNodes = compose.onAllNodesWithText("Belum ada catatan", substring = true).fetchSemanticsNodes()
        assertEquals("Audit L14 regression: having notes must not show empty state", 0, emptyNodes.size)

        // Note is displayed
        exists("Toko A")
        exists("Rp300.000")
    }

    @Test
    fun recordFabOpensChoiceSheetWithAllFiveOptions() {
        start()
        click("Catatan")
        clickDescription("Catat")

        // 5 options present in sheet
        exists("Nota penjualan")
        exists("Belanja")
        exists("Uang masuk lain")
        exists("Uang keluar lain")
        exists("Modal & ambil pribadi")

        // Clicking "Nota penjualan" opens sale note editor
        click("Nota penjualan")
        exists("Nota penjualan baru")
    }

    @Test
    fun filterChipsFilterTimelineCorrectly() {
        runBlocking {
            val tokoA = repository.saveContact(businessId, null, ContactDraft("Toko A"))
            repository.saveSale(
                projectId, null,
                SaleDraft(tokoA, LocalDate.now(), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "2", 300_000)), paidNowRupiah = 0),
            )
            val book = repository.books(projectId).first().first { it.mode == BookMode.REALIZATION.name }
            val cats = database.financeDao().getCategories(businessId)
            val expenseCat = cats.first { it.name.contains("Bahan", ignoreCase = true) }
            repository.recordEvent(
                EventDraft(
                    businessId = businessId,
                    projectId = projectId,
                    bookId = book.id,
                    type = FinancialEventType.CASH_EXPENSE,
                    amountRupiah = 260_000,
                    date = LocalDate.now(),
                    description = "Minyak 10 liter",
                    category = expenseCat.name,
                    categoryId = expenseCat.id,
                ),
            )
        }

        start()
        click("Catatan")

        // Filter: Belum lunas -> only note visible
        click("Belum lunas")
        exists("Toko A")
        val expenseNodesUnderUnpaid = compose.onAllNodesWithText("Minyak 10 liter").fetchSemanticsNodes()
        assertEquals(0, expenseNodesUnderUnpaid.size)

        // Filter: Uang keluar -> only expense visible
        click("Uang keluar")
        exists("Minyak 10 liter")
        val noteNodesUnderExpense = compose.onAllNodesWithText("Toko A").fetchSemanticsNodes()
        assertEquals(0, noteNodesUnderExpense.size)

        // Filter: Semua -> both visible
        click("Semua")
        exists("Toko A")
        exists("Minyak 10 liter")
    }

    private fun settle() {
        compose.waitForIdle()
        SystemClock.sleep(300)
        compose.waitForIdle()
    }

    private fun exists(text: String) {
        scrollTo(text)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
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
}
