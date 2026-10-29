package com.mankeu.data

import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.domain.BookMode
import com.mankeu.importer.FinanceWorkbookImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Pre-release scale simulations (2026-10-22): a year of a micro warung, a small bengkel and the
 * three units of a medium rice mill, imported through the real XLSX path. Every figure must equal
 * what `docs/evidence/2026-10-22_simulasi_skala_umkm/make_sims.py` computed from the rows, and the
 * slowest steps must stay within what an owner would tolerate on a phone. Timings are logged under
 * the `ScaleSimulation` tag.
 */
@RunWith(AndroidJUnit4::class)
class ScaleSimulationTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private val dao get() = database.financeDao()
    private val assets get() = InstrumentationRegistry.getInstrumentation().context.assets

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repository = FinanceRepository(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun everyScaleImportsExactlyAndFastEnough() = runBlocking {
        val expected = JSONObject(assets.open("simulasi/expected.json").bufferedReader().readText())
        val businesses = mutableMapOf<String, String>()
        expected.keys().forEach { name ->
            val sim = expected.getJSONObject(name)
            // M1/K1/N1: the three rice-mill files are units of one business.
            val businessName = name.substringBefore('_')
            val businessId = businesses.getOrPut(businessName) { repository.createBusiness(businessName) }
            val projectId = repository.createProject(businessId, sim.getString("unit"))
            val bytes = assets.open("simulasi/$name.xlsx").readBytes()

            lateinit var preview: com.mankeu.importer.WorkbookImportPreview
            val parseMs = measureTimeMillis { preview = FinanceWorkbookImporter().preview("$name.xlsx", bytes) }
            assertEquals("$name preview errors", emptyList<String>(), preview.errors)
            val commitMs = measureTimeMillis {
                repository.reviewImport(businessId, projectId, preview)
                repository.commitImport(businessId, projectId, preview)
            }
            val book = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
            lateinit var summary: com.mankeu.domain.FinancialSummary
            val summaryMs = measureTimeMillis { summary = repository.accountingSummary(book).first() }
            val cashFlowMs = measureTimeMillis { repository.cashFlowRecords(book).first() }
            val exportMs = measureTimeMillis { repository.buildProjectExportBundle(projectId) }
            Log.i(TAG, "$name rows=${sim.getInt("rows")} parse=${parseMs}ms commit=${commitMs}ms summary=${summaryMs}ms cashflow=${cashFlowMs}ms export=${exportMs}ms")

            assertEquals("$name rows", sim.getInt("rows"), dao.getEventsForBook(book).size)
            assertEquals("$name closing cash", sim.getLong("closing_cash"), summary.cashBalanceRupiah)
            // Interim until Task 4 (DEC-029): imported product sales are paid notes whose payments post
            // Kas / Piutang, so the posting summary misses exactly the notes' totals.
            val noteRevenue = repository.sales(projectId).first().sumOf { it.total }
            assertEquals("$name revenue", sim.getLong("revenue"), summary.operatingIncomeRupiah + noteRevenue)
            assertEquals("$name operating cost", sim.getLong("operating_cost"), summary.operatingExpenseRupiah)
            assertEquals("$name cash profit", sim.getLong("cash_profit"), summary.profitRupiah + noteRevenue)
            // Budget on a mid-range phone, generous for an emulator: a year of a medium unit (~9k
            // rows) must import within a minute and every screen query must answer within 2 s.
            assertTrue("$name import took ${parseMs + commitMs} ms", parseMs + commitMs < 60_000)
            assertTrue("$name summary took $summaryMs ms", summaryMs < 2_000)
            assertTrue("$name cash flow took $cashFlowMs ms", cashFlowMs < 2_000)
        }
    }

    private companion object {
        const val TAG = "ScaleSimulation"
    }
}
