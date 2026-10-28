package com.mankeu.presentation

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.remember
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.data.WorkspaceSelection
import com.mankeu.domain.CostNature
import com.mankeu.domain.Recurrence
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.importer.ImportBudgetLine
import com.mankeu.importer.ImportRecipeLine
import com.mankeu.importer.ImportSaleUnit
import com.mankeu.importer.ImportProduct
import com.mankeu.importer.ImportStockItem
import com.mankeu.importer.ImportTransaction
import com.mankeu.importer.WorkbookImportPreview
import com.mankeu.presentation.guide.HelpArticle
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Makes the Pusat Bantuan screenshots (owner decision Q12): fictional demo data, Indonesian, light
 * theme, each place highlighted the way "Tunjukkan di aplikasi" highlights it, without the card.
 *
 * Not a check: it only runs when `scripts/capture_help_screenshots.ps1` passes
 * `-e helpScreenshots true`, writes `files/help/help_*.webp` in the app's storage, and restores the
 * owner's display preferences afterwards.
 */
@RunWith(AndroidJUnit4::class)
class HelpScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureHelpScreenshots() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("helpScreenshots") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("mankeu_ui_prefs", Context.MODE_PRIVATE)
        val saved = prefs.all.toMap()
        prefs.edit().putString("theme_mode", ThemeMode.LIGHT.name).putString("motion_mode", MotionMode.OFF.name).commit()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository = FinanceRepository(database)
            val selection = runBlocking { seed(repository) }
            val viewModel = AppViewModel(
                repository, FinanceWorkbookImporter(), EncryptedBackupManager(database, File(context.cacheDir, "help-shot.aabk")),
                GoogleIdentity(context), GoogleDriveBackupClient(), rememberedSelection = selection,
            )
            val indonesian = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("id")) }
            val localized = context.createConfigurationContext(indonesian)
            var current by mutableStateOf<HelpArticle?>(null)
            compose.setContent {
                // Wraps the host Activity (file launchers look it up) and only swaps in Indonesian resources.
                val host = LocalContext.current
                val wrapped = remember(host) { object : ContextWrapper(host) { override fun getResources(): Resources = localized.resources } }
                CompositionLocalProvider(LocalContext provides wrapped, LocalConfiguration provides indonesian) {
                    MankeuApp(viewModel, captureShow = current)
                }
            }
            compose.waitForIdle()
            val out = File(context.filesDir, "help").apply { deleteRecursively(); mkdirs() }
            HelpArticle.entries.distinctBy { it.show.imageName }.forEach { article ->
                compose.runOnIdle { current = article }
                // The page transition runs on the test's virtual clock: advance it, sleeping would not.
                repeat(30) { compose.mainClock.advanceTimeBy(100) }
                compose.waitForIdle()
                save(compose.onRoot().captureToImage().asAndroidBitmap(), File(out, article.show.imageName + ".webp"))
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

    /** A coffee stall with two products, a plan, this month's sales, stock and one import batch. */
    private suspend fun seed(repository: FinanceRepository): WorkspaceSelection {
        val business = repository.createBusiness("Kedai Kopi Senja")
        val unit = repository.createProject(business, "Utama")
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        repository.commitImport(
            business, unit,
            WorkbookImportPreview(
                sourceName = "kedai-kopi-senja.xlsx",
                sourceSha256 = "help-screenshots",
                transactions = listOf(
                    ImportTransaction("T!A2", month.atDay(1), "Modal awal", "Setoran Modal Pemilik", incoming = true, amountRupiah = 15_000_000),
                    ImportTransaction("T!A3", month.atDay(1), "Mesin espresso", "Beli Peralatan & Aset", incoming = false, amountRupiah = 7_200_000, usefulLifeMonths = 36),
                    ImportTransaction("T!A4", month.atDay(1), "Sewa lapak", "Sewa Tempat", incoming = false, amountRupiah = 1_000_000),
                    ImportTransaction("T!A5", month.atDay(1), "Belanja susu, kopi, cup", "Bahan Baku & Stok", incoming = false, amountRupiah = 2_350_000),
                    ImportTransaction("T!A6", today, "Jual kopi susu", "Penjualan & Jasa", incoming = true, amountRupiah = 4_600_000, productName = "Kopi Susu", quantityText = "460"),
                    ImportTransaction("T!A7", today, "Jual es teh", "Penjualan & Jasa", incoming = true, amountRupiah = 900_000, productName = "Es Teh", quantityText = "180"),
                    ImportTransaction("T!A8", today, "Gaji pembantu", "Gaji & Upah", incoming = false, amountRupiah = 800_000),
                ),
                budgetLines = listOf(
                    ImportBudgetLine("R!A2", null, "Sewa Tempat", "Sewa lapak", null, null, null, 1_000_000, Recurrence.MONTHLY, CostNature.FIXED),
                    ImportBudgetLine("R!A3", null, "Gaji & Upah", "Gaji pembantu", null, null, null, 800_000, Recurrence.MONTHLY, CostNature.FIXED),
                    ImportBudgetLine("R!A4", null, "Listrik, Air & Internet", "Listrik dan air", null, null, null, 200_000, Recurrence.MONTHLY, CostNature.FIXED),
                    ImportBudgetLine("R!A5", month, "Beli Peralatan & Aset", "Mesin espresso", null, null, null, 7_200_000, Recurrence.ONCE, usefulLifeMonths = 36),
                    ImportBudgetLine("R!A6", month, "Setoran Modal Pemilik", "Modal awal", null, null, null, 15_000_000),
                ),
                products = listOf(
                    ImportProduct(
                        "P!A2", "Kopi Susu", "cup", "6", "700", 1,
                        units = listOf(ImportSaleUnit("cup", "1", 10_000)),
                        recipe = listOf(ImportRecipeLine("R!A2", "Susu", null, null, 18_000), ImportRecipeLine("R!A3", "Kopi dan cup", null, null, 12_000)),
                    ),
                    ImportProduct(
                        "P!A3", "Es Teh", "gelas", "1", "400", 1,
                        units = listOf(ImportSaleUnit("gelas", "1", 5_000)),
                        recipe = listOf(ImportRecipeLine("R!A4", "Teh, gula, es", null, null, 1_500)),
                    ),
                ),
                stockItems = listOf(
                    ImportStockItem("S!A2", "Susu UHT", "liter", 18_000, "24", "6"),
                    ImportStockItem("S!A3", "Biji kopi", "kg", 120_000, "3", "1"),
                ),
                planPeriod = month to month.plusMonths(11),
            ),
        )
        return WorkspaceSelection(business, unit)
    }

    private fun save(bitmap: Bitmap, file: File) {
        val width = 540
        val scaled = Bitmap.createScaledBitmap(bitmap, width, bitmap.height * width / bitmap.width, true)
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        file.outputStream().use { scaled.compress(format, 82, it) }
    }
}
