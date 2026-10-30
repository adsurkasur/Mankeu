package com.mankeu.presentation

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.data.GoldenData
import com.mankeu.data.PlanAssembly
import com.mankeu.domain.ActualBepCalculator
import com.mankeu.domain.BookMode
import com.mankeu.domain.MonthlySummary
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
import java.io.ByteArrayInputStream
import java.io.File
import java.time.YearMonth
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * U2, audit P7 "satu angka": for one month, Beranda, Laporan, the BEP card and the export show the
 * numbers of one [MonthlySummary]; no screen names a different number "untung" or "selisih".
 * The repository is pinned to October 2026, the month of the data.
 */
@RunWith(AndroidJUnit4::class)
class OneNumberTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val october = YearMonth.of(2026, 10)
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var savedPrefs: Map<String, *>
    private val savedLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        savedPrefs = prefs.all.toMap()
        prefs.edit().clear().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name).commit()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = FinanceRepository(database, currentMonth = { october })
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
    fun goldenOctoberReadsOneSummaryOnEveryScreenAndInTheExport() {
        val ids = runBlocking { GoldenData.keripikOctober(repository, database) }
        val month = runBlocking { repository.monthlySummary(ids.bookId).first() }
        assertEquals(listOf(-310_000L, 4_440_000L, 5_200_000L, 760_000L), listOf(month.profitRupiah, month.hppRupiah, month.productionSpendRupiah, month.productionVarianceRupiah))
        assertEquals(listOf(2_050_000L, 6_200_000L, 2_250_000L, 3_380_000L, 200_000L), listOf(month.moneyInRupiah, month.moneyOutRupiah, month.cashOnHandRupiah, month.receivableRupiah, month.payableRupiah))

        // Export: the bundle carries the same summary and the xlsx prints its profit.
        val bundle = runBlocking { repository.buildProjectExportBundle(ids.projectId) }
        assertEquals(month, bundle.books.single { it.book.mode == BookMode.REALIZATION.name }.month)
        val sheet = laporanSheet(runBlocking { repository.exportProjectWorkbook(ids.projectId) })
        assertEquals(-310_000L, numberAfter(sheet, "Laba / rugi bulan ini"))
        assertEquals(760_000L, numberAfter(sheet, "Selisih belanja produksi vs HPP"))
        assertTrue("no all-months Realisasi profit in the export", sheet.split("<row").none { "Realisasi" in it && "Laba atau rugi" in it })

        // The BEP card is built from the same month.
        val products = runBlocking { repository.products(ids.projectId).first() }
        assertEquals(month.profitRupiah, ActualBepCalculator.analyse(PlanAssembly.products(products, emptyMap()), PlanAssembly.actualMonth(month.profit, products)).profitRupiah)

        start()
        checkScreens(month)
    }

    @Test
    fun keripikBuSriHasOneProfitAndOneGap() {
        val ids = runBlocking { GoldenData.buSri(repository, database) }
        val month = runBlocking { repository.monthlySummary(ids.bookId).first() }
        assertEquals(170_000L, month.profitRupiah) // 600.000 + 50.000 − HPP 480.000; the minyak is in HPP
        assertEquals(-220_000L, month.productionVarianceRupiah) // 260.000 − 480.000, at the note's HPP
        assertEquals(listOf(350_000L, 260_000L, 590_000L, 300_000L), listOf(month.moneyInRupiah, month.moneyOutRupiah, month.cashOnHandRupiah, month.receivableRupiah))

        start()
        checkScreens(month)
        // Today's recipe cost (Rp6.050 a bungkus) must not give a second gap.
        assertTrue(compose.onAllNodesWithText("224.000", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        // Komparasi is money, never "untung".
        click("Uang vs rencana")
        exists("Uang masuk/keluar vs rencana")
        assertTrue(compose.onAllNodesWithText("Laba / Rugi kas", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText("Untung", substring = true, useUnmergedTree = true).fetchSemanticsNodes().none { text(it).startsWith("Untung") })
    }

    /** Every profit and selisih figure on Beranda, Laporan and the BEP card equals [month]'s. */
    private fun checkScreens(month: MonthlySummary) {
        val profit = month.profitRupiah.toRupiah()
        val gap = month.productionVarianceRupiah.toRupiah()
        val sentence = "lebih ${if (month.productionVarianceRupiah > 0) "besar" else "kecil"} ${kotlin.math.abs(month.productionVarianceRupiah).toRupiah()}"

        // Beranda: the tile at the top, the profit detail card and the BEP card further down.
        assertBeside("Untung bulan ini", profit)
        absent("semua bulan")
        assertBeside("Selisih belanja produksi vs HPP", gap)
        scrollTo("Status BEP")
        assertBeside("Untung bulan ini", profit)
        exists(sentence)

        // Laporan: the income statement names the month and shows the same profit, gap and bills.
        click("Laporan")
        exists("Laporan Laba Rugi · Oktober 2026")
        assertBeside("Untung / Rugi bulan ini", profit)
        assertBeside("Selisih belanja produksi vs HPP", gap)
        assertBeside("Tagihan belum dibayar pelanggan", month.receivableRupiah.toRupiah())
        absent("semua bulan")
        click("Kelayakan & BEP")
        assertBeside("Untung bulan ini", profit)
        exists(sentence)
    }

    private fun assertBeside(label: String, expected: String) {
        scrollTo(label)
        val values = valuesBeside(label)
        assertTrue("\"$label\" not shown", values.isNotEmpty())
        assertEquals("\"$label\"", values.map { expected }, values)
    }

    private fun laporanSheet(xlsx: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(xlsx)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                if (entry.name.startsWith("xl/worksheets/")) {
                    val xml = zip.readBytes().decodeToString()
                    if ("Laba / rugi bulan ini" in xml) return xml
                }
            }
        }
        error("No Laporan sheet with the month's lines")
    }

    /** The first number cell after the cell holding [label]. */
    private fun numberAfter(xml: String, label: String): Long =
        Regex("<v>(-?\\d+)</v>").find(xml, xml.indexOf(label))!!.groupValues[1].toLong()

    private fun text(node: SemanticsNode) = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    /**
     * The amount shown with each composed [label]: on its row to the right (ReportRow) or just under it
     * (metric tile).
     */
    private fun valuesBeside(label: String): List<String> {
        settle()
        val amounts = compose.onAllNodes(hasText("Rp", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { text(it).matches(Regex("^[-−]?Rp.?[\\d.]+$")) }
        return compose.onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes().map { l ->
            val b = l.boundsInRoot
            val beside = amounts.filter { it.boundsInRoot.center.y in b.top..b.bottom && it.boundsInRoot.left >= b.right - 1 }
            val below = amounts.filter { it.boundsInRoot.top >= b.bottom - 1 && it.boundsInRoot.left < b.right && it.boundsInRoot.right > b.left }
            text(beside.minByOrNull { it.boundsInRoot.left } ?: below.minBy { it.boundsInRoot.top })
        }
    }

    private fun start() {
        val vm = AppViewModel(
            repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "one-number.aabk")),
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
        exists("Untung bulan ini")
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

    private fun absent(text: String) =
        assertTrue("\"$text\" is shown", compose.onAllNodesWithText(text, substring = true, ignoreCase = true, useUnmergedTree = true).fetchSemanticsNodes().isEmpty())

    private fun click(text: String) {
        settle()
        val clickable = compose.onAllNodes(hasText(text) and hasClickAction())
        clickable[clickable.fetchSemanticsNodes().size - 1].performSemanticsAction(SemanticsActions.OnClick)
        settle()
        // Each screen starts at its top.
        compose.onAllNodes(hasScrollToIndexAction()).fetchSemanticsNodes().indices.forEach {
            runCatching { compose.onAllNodes(hasScrollToIndexAction())[it].performScrollToIndex(0) }
        }
        settle()
    }

    private fun scrollTo(text: String) {
        val lists = compose.onAllNodes(hasScrollToIndexAction())
        val count = lists.fetchSemanticsNodes().size
        if (count > 0) runCatching { lists[count - 1].performScrollToNode(hasText(text, substring = true)) }
        settle()
    }
}
