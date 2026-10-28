package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.ProductDraft
import com.mankeu.data.RecipeLineDraft
import com.mankeu.data.SaleUnitDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.Recurrence
import com.mankeu.importer.FinanceWorkbookImporter
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Screenshots for the owner-facing user guide (`docs/panduan/PANDUAN_PENGGUNAAN_MANKEU.md`), taken by using the app the way an
 * owner does: from an empty app to a coffee stall with a plan, a month of records, stock, reports,
 * import, backup and help. Every step goes through the real screens, so a broken flow fails here.
 *
 * Runs only with `-e moduleScreenshots true` (`scripts/capture_module_screenshots.ps1`). In-memory
 * database; the owner's display preferences are restored afterwards. Screens are captured with
 * UiAutomation so sheets and dialogs are included.
 */
@RunWith(AndroidJUnit4::class)
class ModuleScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var out: File
    private lateinit var database: AppDatabase

    @Test
    fun walkThroughTheAppAsAnOwner() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("moduleScreenshots") == "true")
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = prefs.all.toMap()
        prefs.edit().clear().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name).commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        out = File(context.filesDir, "module").apply { deleteRecursively(); mkdirs() }
        try {
            val repository = FinanceRepository(database)
            val vm = AppViewModel(
                repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "module.aabk")),
                GoogleIdentity(context), GoogleDriveBackupClient(),
            )
            // Indonesian everywhere, sheets and dialogs included: they read the Activity's resources.
            val indonesian = Locale.forLanguageTag("id")
            Locale.setDefault(indonesian)
            compose.runOnUiThread {
                val config = Configuration(compose.activity.resources.configuration).apply { setLocale(indonesian) }
                @Suppress("DEPRECATION")
                compose.activity.resources.updateConfiguration(config, compose.activity.resources.displayMetrics)
            }
            compose.setContent { MankeuApp(vm) }
            try {
                walk(vm, repository)
            } catch (failure: Throwable) {
                shot("zz_gagal")
                throw failure
            }
        } finally {
            database.close()
            prefs.edit().clear().apply {
                saved.forEach { (key, value) ->
                    when (value) {
                        is Boolean -> putBoolean(key, value)
                        is Int -> putInt(key, value)
                        is Long -> putLong(key, value)
                        is Float -> putFloat(key, value)
                        is String -> putString(key, value)
                    }
                }
            }.commit()
        }
    }

    private fun walk(vm: AppViewModel, repository: FinanceRepository) {
        val month = YearMonth.now()
        // --- 1. Memulai -------------------------------------------------------------------------
        shot("01_beranda_kosong")
        // With no business yet the sheet opens straight on its form; saving creates unit "Utama"
        // and closes the sheet (owner decision 2026-10-22).
        click("Belum ada usaha")
        type("Nama usaha", "Kedai Kopi Senja")
        shot("02_tambah_usaha")
        click("Simpan")
        settle()
        shot("03_beranda_unit_baru")

        // --- 2. Modal awal lewat tombol Catat -----------------------------------------------------
        clickDescription("Catat")
        shot("07_tombol_catat")
        click("Catat Pemasukan")
        type("Jumlah uang", "15000000")
        type("Keterangan", "Modal awal")
        pick("Kategori", "Setoran Modal Pemilik")
        shot("08_form_modal_awal")
        click("Simpan catatan")

        // --- 3. Rencana Usaha ---------------------------------------------------------------------
        click("Rencana")
        click("Catatan")
        shot("09_rencana_kosong")
        click("Ubah")
        shot("10_periode_rencana")
        click("Batal")
        vm.setPlanPeriod(month, month.plusMonths(11))
        settle()
        clickNear("Tambah", "Produk")
        // The full-page product editor (Fase 1): one batch of 6 cups, same Rp5.000 per cup as before.
        type("Nama produk", "Kopi Susu")
        type("Satuan terkecil", "cup")
        type("Satu batch menghasilkan", "6")
        click("Tambah biaya lain")
        type("Biaya (mis. gas, upah)", "Susu")
        type("Biaya per batch", "18000")
        click("Tambah biaya lain")
        type("Biaya (mis. gas, upah)", "Kopi dan cup", index = 1)
        type("Biaya per batch", "12000", index = 1)
        scrollTo("Harga jual")
        type("Harga jual", "10000")
        scrollTo("Untuk rencana usaha")
        click("Untuk rencana usaha")
        type("Target terjual per bulan", "700")
        field("Nama produk", 0).bringIntoView()
        shot("11_form_produk")
        click("Simpan")
        vm.saveProduct(
            null,
            ProductDraft(
                name = "Es Teh", baseUnit = "gelas", batchYieldText = "1",
                units = listOf(SaleUnitDraft(name = "gelas", contentText = "1", priceRupiah = 5_000)),
                recipe = listOf(RecipeLineDraft("Teh, gula, es", amountRupiah = 1_500)),
                targetPerMonthText = "400",
            ),
        )
        settle()
        clickNear("Tambah", "Biaya Tetap per Bulan")
        type("Nama", "Sewa lapak")
        pick("Kategori", "Sewa Tempat")
        type("Nominal per bulan", "1000000")
        shot("12_form_biaya_tetap")
        click("Simpan")
        settle()
        val categories = runBlocking { database.financeDao().getCategories(businessId(vm)) }
        fun category(name: String) = categories.first { it.name == name }.id
        vm.savePlanLine(null, category("Gaji & Upah"), "Gaji pembantu", 800_000, Recurrence.MONTHLY, CostNature.FIXED, null, null)
        settle()
        vm.savePlanLine(null, category("Listrik, Air & Internet"), "Listrik dan air", 200_000, Recurrence.MONTHLY, CostNature.FIXED, null, null)
        scrollTo("Aset dan Penyusutan")
        clickNear("Tambah", "Aset dan Penyusutan")
        type("Nama", "Mesin espresso")
        type("Nominal", "7200000")
        replace("Umur pakai (bulan)", "36")
        shot("13_form_aset")
        click("Simpan")
        vm.savePlanLine(null, category("Setoran Modal Pemilik"), "Modal awal", 15_000_000, Recurrence.ONCE, null, month, null)
        settle()
        settle()
        scrollTo("Rencana Usaha")
        shot("14_rencana_kelayakan")
        scrollTo("Produk")
        shot("15_rencana_produk")
        scrollTo("Uang pemilik yang disetor")
        shot("16_rencana_biaya")

        // --- 4. Mencatat harian ------------------------------------------------------------------
        click("Realisasi")
        clickDescription("Catat")
        click("Catat Pemasukan")
        pick("Kategori", "Penjualan & Jasa")
        pick("Produk (opsional)", "Kopi Susu")
        replace("Volume", "460")
        type("Keterangan", "Jual kopi susu minggu ini")
        shot("17_form_penjualan")
        click("Simpan catatan")
        clickDescription("Catat")
        click("Catat Pengeluaran")
        type("Jumlah uang", "2350000")
        type("Keterangan", "Belanja susu, kopi, cup")
        pick("Kategori", "Bahan Baku & Stok")
        shot("18_form_pengeluaran")
        click("Simpan catatan")
        clickDescription("Catat")
        click("Catat Pengeluaran")
        type("Jumlah uang", "7200000")
        type("Keterangan", "Mesin espresso")
        pick("Kategori", "Beli Peralatan & Aset")
        replace("Umur pakai (bulan)", "36")
        shot("19_form_beli_alat")
        click("Simpan catatan")
        val today = LocalDate.now()
        val esTeh = runBlocking { database.financeDao().getProducts(projectId(vm)) }.first { it.name == "Es Teh" }.id
        vm.record(FinancialEventType.CASH_INCOME, 900_000, "Jual es teh minggu ini", category("Penjualan & Jasa"), "Penjualan & Jasa", today,
            quantityText = "180", unit = "gelas", unitPriceRupiah = 5_000, productId = esTeh)
        settle()
        record(vm, FinancialEventType.CASH_EXPENSE, 1_000_000, "Sewa lapak", category("Sewa Tempat"), "Sewa Tempat", today)
        record(vm, FinancialEventType.CASH_EXPENSE, 800_000, "Gaji pembantu", category("Gaji & Upah"), "Gaji & Upah", today)
        record(vm, FinancialEventType.CASH_EXPENSE, 1_500_000, "Listrik bulan ini", category("Listrik, Air & Internet"), "Listrik, Air & Internet", today)
        click("Catatan")
        shot("20_daftar_catatan")
        // The newest card is the bill typed with an extra zero: Ubah opens it filled in (DEC-019).
        clickDescription("Ubah catatan")
        replace("Jumlah uang", "150000")
        shot("21_ubah_catatan")
        click("Simpan catatan")
        click("Beranda")
        shot("22_beranda_realisasi")
        scrollTo("Status BEP Bulan Ini")
        shot("23_status_bep")

        // --- 5. Stok (Bahan & barang on the Produk tab) ----------------------------------------
        click("Produk")
        scrollTo("Tambah Barang")
        shot("24_stok_kosong")
        click("Tambah Barang")
        type("Nama barang", "Susu UHT")
        type("Satuan (mis. kg, pcs, cup)", "liter")
        type("Harga beli per satuan", "18000")
        type("Stok awal", "24")
        type("Batas stok minimum", "6")
        shot("25_form_barang")
        click("Simpan")
        vm.createStockItem("Biji kopi", "kg", 120_000, BigDecimal("3"), BigDecimal("1"))
        settle()
        settle()
        shot("26_stok_daftar")
        click("Susu UHT")
        type("Jumlah", "12")
        type("Catatan (opsional)", "Beli 12 liter di pasar")
        shot("27_mutasi_stok")
        click("Batal")

        // --- 6. Laporan --------------------------------------------------------------------------
        click("Laporan")
        shot("28_laporan_laba_rugi")
        scrollTo("Pergerakan Kas Bulanan")
        shot("29_laporan_arus_kas")
        scrollTo("Kelayakan & BEP")
        click("Kelayakan & BEP")
        shot("30_laporan_kelayakan")
        click("Realisasi vs Target")
        shot("31_laporan_target")

        // --- 7. Berkas dan Data ------------------------------------------------------------------
        clickDescription("Berkas dan Data")
        shot("32_berkas_data")
        val file = File(context.cacheDir, "kedai-kopi-senja.xlsx").apply {
            writeBytes(runBlocking { repository.exportProjectWorkbook(projectId(vm)) })
        }
        vm.previewWorkbook(context.contentResolver, Uri.fromFile(file))
        settle(1500)
        shot("33_pratinjau_impor")
        vm.resetImportState()
        click("Cadangan terenkripsi")
        shot("35_dialog_cadangan")
        click("Tutup")
        clickDescription("Kembali")

        // --- 8. Bantuan --------------------------------------------------------------------------
        click("Beranda")
        clickDescription("Bantuan")
        shot("36_lembar_bantuan")
        click("Buka Pusat Bantuan")
        shot("37_pusat_bantuan")
        click("Mencatat penjualan")
        shot("38_artikel_bantuan")
        scrollTo("Tunjukkan di aplikasi")
        click("Tunjukkan di aplikasi")
        settle(1500)
        shot("39_tunjukkan")
        clickDescription("Pengaturan")
        scrollTo("Buka Pusat Bantuan")
        shot("40_pengaturan_panduan")
        scrollTo("Sinkronisasi antar-HP")
        shot("43_sinkronisasi")
        clickDescription("Kembali")

        // --- Lampiran A: usaha tani musiman ---------------------------------------------------------
        newBusiness(vm, "Kedai Kopi Senja", "Tani Cabai Makmur", "Kebun 1")
        click("Rencana")
        click("Catatan")
        vm.setPlanPeriod(month, month.plusMonths(11))
        settle()
        clickNear("Tambah", "Produk")
        type("Nama produk", "Cabai merah")
        type("Satuan (cup, porsi, kg)", "kg")
        type("Target per bulan", "400")
        type("Harga jual per satuan", "35000")
        replace("Mulai dijual bulan ke-", "4")
        type("Komponen", "Panen dan angkut")
        type("Biaya", "3000")
        field("Nama produk", 0).bringIntoView()
        shot("A1_form_produk_musiman")
        click("Simpan")
        val farm = runBlocking { database.financeDao().getCategories(businessId(vm)) }
        fun farmCategory(name: String) = farm.first { it.name == name }.id
        vm.savePlanLine(null, farmCategory("Gaji & Upah"), "Upah harian", 2_500_000, Recurrence.MONTHLY, CostNature.FIXED, null, null)
        settle()
        vm.savePlanLine(null, farmCategory("Sewa Tempat"), "Sewa lahan setahun", 12_000_000, Recurrence.ONCE, CostNature.FIXED, month, null)
        settle()
        vm.savePlanLine(null, farmCategory("Bahan Baku & Stok"), "Pupuk dan obat", 1_500_000, Recurrence.MONTHLY, CostNature.VARIABLE, null, null)
        settle()
        vm.savePlanLine(null, farmCategory("Beli Peralatan & Aset"), "Pompa dan sprayer", 3_600_000, Recurrence.ONCE, null, month, 24)
        settle()
        vm.savePlanLine(null, farmCategory("Setoran Modal Pemilik"), "Modal awal", 20_000_000, Recurrence.ONCE, null, month, null)
        settle()
        scrollTo("Rencana Usaha")
        shot("A2_kelayakan_musiman")
        scrollTo("Produk")
        shot("A3_produk_musiman")

        // --- Lampiran B: toko dua cabang dengan stok bersama -----------------------------------------
        newBusiness(vm, "Tani Cabai Makmur", "Toko Sembako Berkah", "Pusat")
        click("Toko Sembako Berkah")
        scrollTo("Tambah Unit Usaha")
        click("Tambah Unit Usaha")
        type("Nama proyek / unit usaha", "Cabang 2")
        click("Simpan")
        click("Pakai ini")
        closeScopeSheet()
        click("Realisasi")
        click("Produk")
        addStock("Gula", "kg", "15500", "50", "10", shared = false)
        addStock("Kantong plastik", "pak", "12000", "30", "5", shared = true, shotName = "B1_form_barang_bersama")
        shot("B2_stok_pusat")
        click("Toko Sembako Berkah")
        click("Pakai ini")
        closeScopeSheet()
        shot("B3_stok_cabang")
    }

    // --- helpers ---------------------------------------------------------------------------------

    /**
     * Opens the scope sheet from the header ([current] usaha) and adds an usaha. It starts with unit
     * "Utama", renamed to [unit] the way an owner with a named field would.
     */
    private fun newBusiness(vm: AppViewModel, current: String, business: String, unit: String) {
        click(current)
        click("Tambah Usaha")
        type("Nama usaha", business)
        click("Simpan")
        settle()
        closeScopeSheet()
        vm.updateProjectDetails(projectId(vm), unit, "", null, null)
        settle()
    }

    private fun closeScopeSheet() {
        compose.onAllNodesWithText("Usaha dan Cabang / Unit", useUnmergedTree = true)[0]
            .performTouchInput { swipeDown(startY = top, endY = top + 2000f, durationMillis = 300) }
        settle()
    }

    private fun addStock(name: String, unit: String, price: String, initial: String, min: String, shared: Boolean, shotName: String? = null) {
        scrollTo("Tambah Barang")
        click("Tambah Barang")
        type("Nama barang", name)
        type("Satuan (mis. kg, pcs, cup)", unit)
        type("Harga beli per satuan", price)
        type("Stok awal", initial)
        type("Batas stok minimum", min)
        if (shared) click("Semua unit")
        shotName?.let { shot(it) }
        click("Simpan")
    }

    // The selection on screen, not `currentWorkspace()`: that returns the first usaha, which after the
    // appendix creates more than one is not the one being photographed.
    private fun businessId(vm: AppViewModel) = vm.uiState.value.selectedBusinessId!!
    private fun projectId(vm: AppViewModel) = vm.uiState.value.selectedProjectId!!

    private fun record(vm: AppViewModel, type: FinancialEventType, amount: Long, description: String, categoryId: String, categoryName: String, date: LocalDate) {
        vm.record(type, amount, description, categoryId, categoryName, date)
        settle()
    }

    /** The system back key, to whichever window (sheet, dialog, page) has focus. */
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        settle()
    }

    private fun settle(extraMillis: Long = 600) {
        compose.waitForIdle()
        repeat(10) { compose.mainClock.advanceTimeBy(100) }
        compose.waitForIdle()
        SystemClock.sleep(extraMillis)
    }

    private fun shot(name: String) {
        settle()
        val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val width = 720
        val scaled = Bitmap.createScaledBitmap(screen, width, screen.height * width / screen.width, true)
        File(out, "$name.jpg").outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    }

    /** [index] -1 = the last match: the one on the top layer (a sheet or dialog over the page). */
    private fun node(text: String, index: Int = -1): SemanticsNodeInteraction {
        settle(200)
        val all = compose.onAllNodesWithText(text, useUnmergedTree = true)
        return all[if (index >= 0) index else all.fetchSemanticsNodes().size - 1]
    }

    /**
     * Presses the clickable element carrying [text] through its OnClick action, not a touch at its
     * last known position: content that shifts (a card collapsing, a list scrolling) made touches
     * land on whatever moved under them. [index] -1 = the top layer.
     */
    private fun click(text: String, index: Int = -1) {
        settle(200)
        var clickable = compose.onAllNodes(hasText(text) and hasClickAction())
        if (clickable.fetchSemanticsNodes().isEmpty()) clickable = compose.onAllNodes(hasText(text, substring = true) and hasClickAction())
        val count = clickable.fetchSemanticsNodes().size
        if (count > 0) {
            clickable[if (index >= 0) index else count - 1].bringIntoView().performSemanticsAction(SemanticsActions.OnClick)
            settle()
            return
        }
        node(text, index).bringIntoView().performClick()
        settle()
    }

    /** Inside a scrolling sheet a node can be composed but clipped; a click there lands elsewhere. */
    private fun SemanticsNodeInteraction.bringIntoView(): SemanticsNodeInteraction {
        runCatching { performScrollTo() }
        settle(200)
        return this
    }

    /** The [text] node closest to [anchor] on screen: the "Tambah" of one plan section. */
    private fun clickNear(text: String, anchor: String) {
        scrollTo(anchor)
        val top = compose.onAllNodesWithText(anchor, useUnmergedTree = true)[0].fetchSemanticsNode().boundsInRoot.top
        val all = compose.onAllNodesWithText(text, useUnmergedTree = true)
        val index = all.fetchSemanticsNodes().withIndex().minBy { kotlin.math.abs(it.value.boundsInRoot.top - top) }.index
        all[index].performClick()
        settle()
    }

    private fun clickDescription(description: String) {
        settle(200)
        compose.onAllNodes(hasContentDescription(description) and hasClickAction())[0].bringIntoView()
            .performSemanticsAction(SemanticsActions.OnClick)
        settle()
    }

    private fun field(label: String, index: Int) =
        compose.onAllNodes(hasSetTextAction() and hasText(label, substring = true))[index]

    /** Fills a field through its SetText action: no focus, so no keyboard over the screenshot. */
    private fun type(label: String, value: String, index: Int = 0) = replace(label, value, index)

    /** For fields that start with a value (a default yield, a default useful life). */
    private fun replace(label: String, value: String, index: Int = 0) {
        settle(200)
        field(label, index).performSemanticsActionTextReplace(value)
    }

    private fun SemanticsNodeInteraction.performSemanticsActionTextReplace(value: String) {
        fetchSemanticsNode().config[SemanticsActions.SetText].action?.invoke(androidx.compose.ui.text.AnnotatedString(value))
    }

    private fun pick(fieldLabel: String, option: String) {
        settle(200)
        compose.onAllNodes(hasText(fieldLabel, substring = true) and SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))[0].performClick()
        settle()
        compose.onAllNodesWithText(option, useUnmergedTree = true).let { it[it.fetchSemanticsNodes().size - 1] }.performClick()
        settle()
    }

    private fun scrollTo(text: String) {
        settle(200)
        // The top layer's list: a sheet's, when one is open over the page.
        val lists = compose.onAllNodes(hasScrollToIndexAction())
        lists[lists.fetchSemanticsNodes().size - 1].performScrollToNode(hasText(text, substring = true))
        settle()
    }
}
