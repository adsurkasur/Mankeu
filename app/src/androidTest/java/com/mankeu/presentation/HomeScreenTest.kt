package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.BusinessEntity
import com.mankeu.data.ContactDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.data.GoldenData
import com.mankeu.data.ProjectEntity
import com.mankeu.data.SaleDraft
import com.mankeu.data.SaleLineDraft
import com.mankeu.domain.MonthlySummary
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.presentation.screens.HomeScreen
import com.mankeu.presentation.screens.PlanScreen
import com.mankeu.presentation.screens.ReportsScreen
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * U4: the new Beranda and header, with no Rencana/Realisasi switch. Golden data is Keripik Bu Sri
 * (October 2026) plus three more customers with open notes; every money figure on Beranda comes
 * from the month's [MonthlySummary].
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val october = YearMonth.of(2026, 10)
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var savedPrefs: Map<String, *>
    private var activeVm: AppViewModel? = null
    private val savedLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        savedPrefs = prefs.all.toMap()
        prefs.edit().clear().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name).commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = FinanceRepository(database, currentMonth = { october })
        indonesian()
    }

    @After
    fun tearDown() {
        Locale.setDefault(savedLocale)
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply { setLocale(savedLocale) }
            @Suppress("DEPRECATION")
            compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        }
        runBlocking { activeVm?.viewModelScope?.coroutineContext?.get(Job)?.cancelAndJoin() }
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
    fun berandaShowsOneProfitNamedBillsAndQuickActions() {
        val ids = runBlocking { GoldenData.buSri(repository, database) }
        runBlocking {
            // Toko A owes 300.000 from the golden note; three more customers owe 600.000, 80.000 and 40.000.
            fun owe(name: String, unit: String, qty: String, price: Long) = runBlocking {
                val contact = repository.saveContact(ids.businessId, null, ContactDraft(name))
                repository.saveSale(ids.projectId, null, SaleDraft(contact, LocalDate.of(2026, 10, 9), null, 0, "", listOf(SaleLineDraft(ids.keripik, unit, qty, price)), 0))
            }
            owe("Toko B", "dus", "2", 300_000)
            owe("Warung C", "bungkus", "10", 8_000)
            owe("Pak D", "bungkus", "5", 8_000)
            // Dated after today: not owed yet, so neither in the rows nor in the total (review I-1).
            val tokoB = repository.contacts(ids.businessId).first().single { it.name == "Toko B" }.id
            repository.saveSale(ids.projectId, null, SaleDraft(tokoB, LocalDate.now().plusDays(5), null, 0, "", listOf(SaleLineDraft(ids.keripik, "dus", "1", 300_000)), 0))
            repository.createStockItem(ids.businessId, ids.projectId, false, "Minyak", "liter", 26_000, BigDecimal(2), BigDecimal(5))
        }
        val month = runBlocking { repository.monthlySummary(ids.bookId).first() }
        start()

        // One main number, the month's profit, and no technical words or switch.
        val texts = berandaTexts()
        assertEquals("one \"Untung bulan ini\"", 1, texts.count { it == "Untung bulan ini" })
        assertBeside("Untung bulan ini", month.profitRupiah.toRupiah())
        assertBeside("Uang di tangan", month.cashOnHandRupiah.toRupiah())
        listOf("Realisasi", "Operasional", "R/C", "Surplus", "Umum", "semua bulan", "Selisih").forEach { word ->
            assertTrue("\"$word\" on Beranda: $texts", texts.none { it.contains(word, ignoreCase = true) })
        }
        assertTrue("a Rencana switch on Beranda", texts.none { it == "Rencana" || it == "Realisasi" })
        assertTrue("the word unit on Beranda: $texts", texts.none { Regex("\\bunit\\b", RegexOption.IGNORE_CASE).containsMatchIn(it) })
        assertTrue("no plan figures on Beranda", texts.none { it.startsWith("Uang kas") })

        // Bills: the three customers owing most, by name, and the total from the summary.
        listOf("Toko B", "Toko A", "Warung C").forEach { assertTrue("$it not in Tagihan", it in texts) }
        assertTrue("Pak D is the fourth and not shown", "Pak D" !in texts)
        assertTrue("Terima bayar" in texts)
        // One source: the total is the sum of every open note dated up to today (300.000 + 600.000 + 80.000 + 40.000).
        assertBeside("Belum dibayar pelanggan", 1_020_000L.toRupiah())
        assertTrue("Toko B's future note is not in its row: $texts", texts.any { it.startsWith("Rp600.000 · ") })
        // Real low-stock warning.
        assertTrue(texts.any { it.startsWith("Minyak tinggal") })

        // Quick actions.
        listOf("Jual", "Belanja", "Produksi").forEach { label ->
            assertTrue("$label is not a button", compose.onAllNodes(hasText(label) and hasClickAction()).fetchSemanticsNodes().isNotEmpty())
        }

        // Tap the profit: four lines from the same summary.
        click("Untung bulan ini")
        assertBeside("Penjualan", month.salesRupiah.toRupiah())
        assertBeside("Pemasukan lain", month.otherIncomeRupiah.toRupiah())
        assertBeside("Modal barang terjual (HPP)", month.hppRupiah.toRupiah())
        assertBeside("Biaya lain", month.otherCostRupiah.toRupiah())
        assertBeside("Untung", month.profitRupiah.toRupiah())

        // Header: business name only, and one labelled Lainnya menu.
        exists("Keripik Bu Sri")
        click("Lainnya")
        listOf("Rencana usaha", "Kontak", "Kategori", "Bantuan", "Cadangan", "Ambil dari / Simpan ke Excel", "Pengaturan").forEach { exists(it) }
        click("Kontak")
        exists("Toko B")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        settle()

        // U5: a customer's name opens that customer's page: what is left, Terima bayar, Kirim pengingat.
        click("Toko B")
        exists("Sisa tagihan")
        exists("Kirim pengingat")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        settle()

        // Produksi: pick the product, then the production sheet with its result bar.
        click("Produksi")
        exists("Produksi apa?")
        click("Keripik")
        exists("Produksi Keripik")
        exists("400 bungkus masuk stok")
    }

    @Test
    fun rencanaUsahaIsAPageOpenedFromBeranda() {
        runBlocking { GoldenData.buSri(repository, database) }
        start()
        click("Rencana usaha")
        exists("Penjualan minimum agar tidak rugi dihitung otomatis")
        assertTrue("one title on the plan page", compose.onAllNodesWithText("Rencana Usaha", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        exists("Untung bulan ini")
    }

    @Test
    fun emptyBusinessShowsThreeFirstSteps() {
        runBlocking { repository.createBusinessWithUnit("Warung Baru", "Umum") }
        start(waitFor = "Buat produk")
        val texts = berandaTexts()
        listOf("Buat produk", "Catat penjualan", "Lihat untung").forEach { step -> assertTrue("$step missing: $texts", texts.any { it.contains(step) }) }
        assertTrue("no zero figures before any data: $texts", texts.none { it == "Rp0" || it == "Untung bulan ini" })
        // Every step is a button that leads somewhere (review M-3, M-8).
        click("Buat produk dan resepnya")
        exists("Produk baru")
        back()
        click("Catat penjualan pertama")
        exists("Nota penjualan baru")
        back()
        click("Lihat untung bulan ini")
        exists("Laporan dan Analisis")
    }

    /** Review I-2: Lainnya → Kategori / Cadangan returns to Beranda when the dialog closes, and the focus does not stick. */
    @Test
    fun kategoriAndCadanganFromLainnyaReturnWhereTheyCameFrom() {
        runBlocking { GoldenData.buSri(repository, database) }
        start()
        click("Lainnya")
        click("Kategori")
        exists("Tambah kategori")
        click("Selesai")
        exists("Untung bulan ini")
        absent("Berkas dan Data")
        click("Lainnya")
        click("Cadangan")
        exists("Cadangan terenkripsi")
        click("Tutup")
        exists("Untung bulan ini")
        absent("Berkas dan Data")
        // A later visit to Berkas dan Data opens on the page, not on a stale dialog.
        click("Lainnya")
        click("Ambil dari / Simpan ke Excel")
        exists("Berkas dan Data")
        absent("Tambah kategori")
    }

    @Test
    fun categoryNameErrorAppearsAfterBlurAndClearsWhenValid() {
        runBlocking { GoldenData.buSri(repository, database) }
        start()
        click("Lainnya")
        click("Kategori")
        absent("Isi namanya dulu.")
        val field = compose.onAllNodes(hasSetTextAction() and hasText("Nama kategori"))[0]
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        click("Pengeluaran")
        exists("Isi namanya dulu.")
        field.performTextInput("Biaya kemasan")
        absent("Isi namanya dulu.")
    }

    @Test
    fun categoryNameErrorAppearsOnlyAfterAttemptAndCategoryCanBeCreated() {
        runBlocking { GoldenData.buSri(repository, database) }
        start()
        click("Lainnya")
        click("Kategori")
        absent("Isi namanya dulu.")
        click("Tambah kategori")
        exists("Isi namanya dulu.")
        compose.onAllNodes(hasSetTextAction() and hasText("Nama kategori"))[0].performTextInput("Biaya kemasan")
        absent("Isi namanya dulu.")
        click("Tambah kategori")
        exists("Biaya kemasan")
    }

    @Test
    fun categoryCostControlsExplainAndPersistProductionCostSelection() {
        val ids = runBlocking { GoldenData.buSri(repository, database) }
        start()
        click("Lainnya")
        click("Kategori")
        exists("Variabel")
        exists("agar tidak dihitung dua kali")
        val options = compose.onAllNodes(hasText("Sudah masuk modal barang", substring = true) and hasClickAction())
        options[0].performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(5_000) {
            runBlocking { database.financeDao().getCategories(ids.businessId).single { it.name == "Bahan Baku & Stok" }.includedInHpp.not() }
        }
    }

    /** Review M-4: Produksi with no product says why the product editor opens. */
    @Test
    fun produksiWithoutAProductSaysToCreateOneFirst() {
        runBlocking {
            val (b, p) = repository.createBusinessWithUnit("Warung Baru", "Umum")
            val book = repository.books(p).first().single { it.mode == com.mankeu.domain.BookMode.REALIZATION.name }.id
            val rent = database.financeDao().getCategories(b).single { it.name == "Sewa Tempat" }
            repository.recordEvent(com.mankeu.data.EventDraft(b, p, book, com.mankeu.domain.FinancialEventType.CASH_EXPENSE, 100_000, LocalDate.of(2026, 10, 2), rent.name, rent.name, categoryId = rent.id))
        }
        start()
        click("Produksi")
        exists("Buat produk dulu")
        exists("Produk baru")
    }

    /** U2 re-review N-1: before the month arrives there are no zeros under any label, on Beranda or Laporan. */
    @Test
    fun noZerosWhileTheMonthLoads() {
        val state = AppUiState(
            businesses = listOf(BusinessEntity("b", "Keripik", createdAtEpochMillis = 0)),
            projects = listOf(ProjectEntity("p", "b", "Umum", isGeneral = true, createdAtEpochMillis = 0)),
            selectedBusinessId = "b",
            selectedProjectId = "p",
            eventCount = 4,
            isLoading = false,
            thisMonth = null,
        )
        var reports by mutableStateOf(false)
        var plan by mutableStateOf(false)
        compose.setContent {
            MankeuTheme(darkTheme = false) {
                when {
                    plan -> PlanScreen(AppUiState(isLoading = true), onOpenProduct = {}, onSavePlanLine = { _, _, _, _, _, _, _, _ -> }, onArchivePlanLine = {}, onSetPeriod = { _, _ -> }, onManageScope = {})
                    reports -> ReportsScreen(state = state)
                    else -> HomeScreen(state = state, onNavigate = {}, onManageScope = {})
                }
            }
        }
        settle()
        assertTrue("Beranda shows a figure while loading", compose.onAllNodes(hasText("Rp", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        compose.runOnUiThread { reports = true }
        settle()
        assertTrue("Laporan shows Rp0 while loading", compose.onAllNodesWithText("Rp0", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        // Rencana usaha during the first load (review M-5).
        compose.runOnUiThread { plan = true }
        settle()
        assertTrue("Rencana usaha shows a figure while loading", compose.onAllNodes(hasText("Rp", substring = true), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    // ---- helpers ------------------------------------------------------------------------------

    private fun indonesian() {
        val id = Locale.forLanguageTag("id")
        Locale.setDefault(id)
        compose.runOnUiThread {
            val config = Configuration(compose.activity.resources.configuration).apply { setLocale(id) }
            @Suppress("DEPRECATION")
            compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
        }
    }

    private fun start(waitFor: String = "Untung bulan ini") {
        val vm = AppViewModel(
            repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "home-screen.aabk")),
            GoogleIdentity(context), GoogleDriveBackupClient(),
        )
        activeVm = vm
        compose.setContent { MankeuApp(vm) }
        settle()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(waitFor, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        settle()
    }

    private fun text(node: SemanticsNode) = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    /** Every text on Beranda, scrolling the list item by item, then back to the top. */
    private fun berandaTexts(): List<String> {
        val seen = mutableListOf<String>()
        fun collect() = compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes().forEach { seen += text(it) }
        collect()
        val list = compose.onAllNodes(hasScrollToIndexAction())[0]
        var index = 0
        while (runCatching { list.performScrollToIndex(index) }.isSuccess) {
            settle()
            collect()
            index++
        }
        list.performScrollToIndex(0)
        settle()
        return seen.distinct()
    }

    private fun assertBeside(label: String, expected: String) {
        scrollTo(label)
        val amounts = compose.onAllNodes(hasText("Rp", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { text(it).matches(Regex("^[-−]?Rp.?[\\d.]+$")) }
        val labels = compose.onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("\"$label\" not shown", labels.isNotEmpty())
        labels.forEach { l ->
            val b = l.boundsInRoot
            val beside = amounts.filter { it.boundsInRoot.center.y in b.top..b.bottom && it.boundsInRoot.left >= b.right - 1 }
            val below = amounts.filter { it.boundsInRoot.top >= b.bottom - 1 && it.boundsInRoot.left < b.right && it.boundsInRoot.right > b.left }
            val value = beside.minByOrNull { it.boundsInRoot.left } ?: below.minByOrNull { it.boundsInRoot.top }
            assertEquals("\"$label\"", expected, value?.let(::text))
        }
    }

    private fun back() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
    }

    private fun absent(text: String) =
        assertTrue("\"$text\" is shown", compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())

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
