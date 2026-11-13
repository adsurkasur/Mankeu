package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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

    /**
     * U5, audit table "jual 2 dus ke Toko A, belum bayar, jatuh tempo 14 hari": the note opens with
     * no box to type in (product line, customer chips and payment segment only), starts on the last
     * product and unit sold, and the whole sale is chip, +, segment, chip, Simpan.
     */
    @Test
    fun theDailySaleIsFiveTapsWithNoTyping() {
        runBlocking {
            val tokoA = repository.saveContact(businessId, null, ContactDraft("Toko A"))
            repository.saveSale(projectId, null, SaleDraft(tokoA, LocalDate.now().minusDays(3), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "1", 300_000)), paidNowRupiah = 300_000))
        }
        start()
        click("Jual")
        exists("Nota penjualan baru")
        // Field count on open (P2: at most 4 visible, outlined boxes only for typing).
        assertEquals(0, compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        exists("1 dus Keripik · Rp300.000")
        assertEquals(0, compose.onAllNodesWithText("Jatuh tempo", substring = true).fetchSemanticsNodes().size)

        click("Toko A")
        clickDescription("Tambah 1 dus Keripik")
        exists("2 dus Keripik · Rp600.000")
        click("Belum dibayar")
        click("14 hari")
        exists("Total Rp600.000")
        exists("untung Rp120.000")
        click("Simpan")

        compose.waitUntil(5_000) { runBlocking { repository.sales(projectId).first() }.size == 2 }
        val note = runBlocking { repository.sales(projectId).first() }.single { it.sale.dateEpochDay == LocalDate.now().toEpochDay() }
        assertEquals("Toko A", note.contact?.name)
        assertEquals(600_000L, note.total)
        assertEquals(0L, note.paid)
        assertEquals(LocalDate.now().plusDays(14), note.dueDate)
    }

    @Test
    fun aNoteWithTwoProductsShowsTotalDiscountAndProfitAndAnUnpaidNoteNeedsACustomer() {
        start()
        // U4: the speed dial lives on Catatan.
        click("Catatan")
        clickDescription("Catat")
        click("Nota penjualan")
        exists("Nota penjualan baru")

        // No note yet: the first product, its main unit. The line sheet changes unit and quantity.
        click("1 bungkus Keripik")
        click("dus")
        clickDescription("Tambah Jumlah")
        click("Simpan")
        // 2 dus: Rp600.000, HPP 2 × 240.000.
        exists("2 dus Keripik · Rp600.000")
        exists("untung Rp120.000")

        click("Tambah produk")
        click("Kopi")
        clickDescription("Tambah 1 gelas Kopi")
        clickDescription("Tambah 2 gelas Kopi")
        exists("3 gelas Kopi · Rp30.000")

        click("+ Tambah detail")
        type("Diskon", "30000")
        // Total 600.000; untung 600.000 − 489.000.
        exists("Total Rp600.000")
        exists("untung Rp111.000")

        click("Belum dibayar")
        exists("Belum lunas? Pilih pelanggannya.")
        click("Simpan")
        exists("Pilih pelanggan untuk nota yang belum lunas.")
        // Refused: the page stays open with the owner's input.
        exists("Nota penjualan baru")
        exists("untung Rp111.000")
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
        type("Jumlah dibayar", "200000")
        click("Simpan")
        exists("Sebagian")
        exists("Sisa Rp400.000")

        click("Bayar")
        // The amount starts on what is left.
        click("Simpan")
        exists("Lunas")
        assertEquals(SaleStatus.PAID, runBlocking { repository.sales(projectId).first() }.single().status)
    }

    private fun seedUnpaidNote(projectId: String = this.projectId, product: String = keripik): String = runBlocking {
        val sari = repository.saveContact(businessId, null, ContactDraft("Bu Sari"))
        repository.saveSale(
            projectId, null,
            SaleDraft(sari, LocalDate.now(), null, 0, "", listOf(SaleLineDraft(product, "dus", "2", 300_000)), paidNowRupiah = 0),
        )
    }

    /** Review I-1 (DEC-029 §7): an edit recomputes HPP at today's prices, and the owner is told. */
    @Test
    fun savingAnEditedNoteSaysItsHppWasRecomputed() {
        seedUnpaidNote()
        start()
        click("Catatan")
        click("Bu Sari")
        click("Ubah")
        exists("Ubah nota")
        click("Simpan")
        exists("Modal barang dihitung ulang dengan harga bahan sekarang")
    }

    /** Review I-2: the pay sheet explains its bound and keeps Simpan off above what is left. */
    @Test
    fun thePaySheetRefusesMoreThanIsLeftInsideTheSheet() {
        seedUnpaidNote()
        start()
        click("Catatan")
        click("Bu Sari")
        click("Bayar")
        type("Jumlah dibayar", "999999999")
        exists("Maks Rp600.000")
        saveButton().assertIsNotEnabled()
        type("Jumlah dibayar", "600000")
        saveButton().assertIsEnabled()
    }

    /** Review I-2 (U5): a name already in the business is offered, not added twice: no "Tambah" row for it. */
    @Test
    fun aDuplicateContactNameIsNotOfferedAsNew() {
        seedUnpaidNote()
        start()
        click("Jual")
        click("Cari / tambah")
        exists("Pilih pelanggan")
        search("bu sari")
        exists("Bu Sari")
        assertEquals(0, compose.onAllNodesWithText("Tambah 'bu sari'", substring = true).fetchSemanticsNodes().size)
        search("Toko Baru")
        click("Tambah 'Toko Baru'")
        // Created and picked: the chip is selected on the note.
        exists("Toko Baru")
        compose.waitUntil(5_000) { runBlocking { repository.contacts(businessId).first() }.any { it.name == "Toko Baru" } }
    }

    /**
     * U5, audit table "terima bayar dari Toko A": Beranda's Tagihan row opens the customer page;
     * Terima bayar starts on everything left and pays the open notes oldest first.
     */
    @Test
    fun terimaBayarFromTheCustomerPagePaysOldestNotesFirst() {
        val (older, newer) = runBlocking {
            val tokoA = repository.saveContact(businessId, null, ContactDraft("Toko A", phone = "0812 3456 789"))
            val older = repository.saveSale(projectId, null, SaleDraft(tokoA, LocalDate.now().minusDays(5), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "1", 300_000)), paidNowRupiah = 0))
            val newer = repository.saveSale(projectId, null, SaleDraft(tokoA, LocalDate.now(), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "2", 300_000)), paidNowRupiah = 0))
            older to newer
        }
        start()
        click("Toko A")
        exists("Sisa tagihan")
        exists("Rp900.000")
        click("Terima bayar")
        exists("Terima bayar dari Toko A")
        type("Jumlah dibayar", "400000")
        click("Simpan")
        compose.waitUntil(5_000) { runBlocking { repository.sales(projectId).first() }.sumOf { it.paid } == 400_000L }
        val sales = runBlocking { repository.sales(projectId).first() }
        assertEquals(300_000L, sales.single { it.sale.id == older }.paid)
        assertEquals(100_000L, sales.single { it.sale.id == newer }.paid)
        exists("Rp500.000")
    }

    /** Review I-3: contacts are per business, so what a customer owes counts every unit. */
    @Test
    fun theContactsPageCountsWhatIsOwedInEveryUnit() {
        runBlocking {
            val branch = repository.createProject(businessId, "Cabang Dua")
            val chips = repository.saveProduct(
                branch, null,
                ProductDraft("Kerupuk", "bungkus", "10", listOf(SaleUnitDraft(name = "dus", contentText = "10", priceRupiah = 300_000)), listOf(RecipeLineDraft("Bahan", amountRupiah = 10_000))),
            )
            seedUnpaidNote(branch, chips)
        }
        start()
        exists("Utama")
        // U4: Kontak is an entry of the labelled Lainnya menu.
        click("Lainnya")
        click("Kontak")
        exists("Belum bayar Rp600.000")
    }

    private fun saveButton() = compose.onAllNodes(hasText("Simpan") and hasClickAction()).let { it[it.fetchSemanticsNodes().size - 1] }

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

    /** The picker's search box: the only text field while the picker is open. */
    private fun search(value: String) {
        settle()
        compose.onAllNodes(hasSetTextAction())[0].fetchSemanticsNode().config[SemanticsActions.SetText].action?.invoke(AnnotatedString(value))
        settle()
    }

}
