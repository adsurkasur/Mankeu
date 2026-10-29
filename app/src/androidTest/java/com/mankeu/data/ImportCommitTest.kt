package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.ProductMovementType
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.importer.ImportBudgetLine
import com.mankeu.importer.ImportProduct
import com.mankeu.importer.ImportProductMovement
import com.mankeu.importer.ImportRecipeLine
import com.mankeu.importer.ImportSaleUnit
import com.mankeu.importer.ImportStockItem
import com.mankeu.importer.ImportTransaction
import com.mankeu.importer.WorkbookImportPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * The template commit path (DEC-012) against a real Room database: rows land in the right book with
 * the type their category implies, unknown categories become custom ones, the batch undoes
 * cleanly, and an identical file may be imported again after a warning.
 */
@RunWith(AndroidJUnit4::class)
class ImportCommitTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var businessId: String
    private lateinit var projectId: String
    private val dao get() = database.financeDao()

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = FinanceRepository(database)
        businessId = repository.createBusiness("Kedai Kopi")
        projectId = repository.createProject(businessId, "Gerai Utama")
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun templateRowsLandInTheirBooksWithCategoryDrivenTypesAndUndoCleanly() = runBlocking {
        val review = repository.reviewImport(businessId, projectId, preview())
        assertEquals(listOf("Sewa Gerobak" to CategoryRole.EXPENSE), review.newCategories)
        assertEquals(emptyList<String>(), review.conflicts)
        assertFalse(review.previouslyImported)

        assertEquals(6, repository.commitImport(businessId, projectId, preview()))

        val books = dao.getBooks(projectId).associateBy { BookMode.valueOf(it.mode) }
        val actual = dao.getEventsForBook(books.getValue(BookMode.REALIZATION).id).associate { it.description to it.type }
        assertEquals(
            mapOf(
                "Setoran modal" to FinancialEventType.OWNER_CAPITAL.name,
                "Jual kopi" to FinancialEventType.CASH_INCOME.name,
                "Sewa gerobak Agustus" to FinancialEventType.CASH_EXPENSE.name,
            ),
            actual,
        )
        val plan = books.getValue(BookMode.PROJECTION).id
        assertEquals(listOf("Biji kopi" to 1_200_000L), dao.getBudgetItems(plan).map { it.name to it.plannedTotalRupiah })
        // The file's plan period fills the unit, which had none.
        val unit = dao.getProject(projectId)!!
        assertEquals(LocalDate.of(2026, 11, 1) to LocalDate.of(2027, 10, 31), LocalDate.ofEpochDay(unit.startDateEpochDay!!) to LocalDate.ofEpochDay(unit.endDateEpochDay!!))
        val product = dao.getProducts(projectId).single()
        assertEquals("Kopi Susu", product.name)
        assertEquals(listOf("Susu"), dao.getRecipeLines(product.id).map { it.name })
        assertEquals(listOf(18_000L), dao.getRecipeLines(product.id).map { it.amountRupiah })
        assertEquals("6", product.batchYieldText)
        assertEquals(listOf(Triple("cup", "1", 10_000L)), dao.getProductUnits(product.id).map { Triple(it.name, it.contentText, it.priceRupiah) })
        val sale = dao.getEventsForBook(books.getValue(BookMode.REALIZATION).id).single { it.description == "Jual kopi" }
        assertEquals(product.id to "450", sale.productId to sale.quantityText)
        // Import is bulk entry into the selected unit, stock included (schema 10).
        assertEquals(listOf("Gula" to projectId), database.stockDao().getStockItems(businessId, projectId).map { it.name to it.projectId })
        val summary = repository.accountingSummary(books.getValue(BookMode.REALIZATION).id).first()
        // Sale 4.5 jt minus rent 0.5 jt; the 5 jt capital moves cash, not profit.
        assertEquals(4_000_000L, summary.profitRupiah)
        assertEquals(9_000_000L, summary.cashBalanceRupiah)

        repository.undoImportBatch(dao.getImportBatches(projectId).single().id)

        assertTrue(dao.getEventsForBook(books.getValue(BookMode.REALIZATION).id).isEmpty())
        assertEquals(0L, repository.accountingSummary(books.getValue(BookMode.REALIZATION).id).first().cashBalanceRupiah)
        assertTrue(dao.getBudgetItems(plan).all { it.archived })
        assertTrue(database.stockDao().getStockItems(businessId, projectId).isEmpty())
        assertTrue(dao.getProducts(projectId).isEmpty())
        // The period the import filled in is cleared again; the owner had not changed it.
        assertEquals(null to null, dao.getProject(projectId)!!.let { it.startDateEpochDay to it.endDateEpochDay })
    }

    @Test
    fun aSaleNamingAnUnknownProductIsAConflict() = runBlocking {
        val orphan = WorkbookImportPreview(
            "produk.xlsx",
            "sha-orphan",
            transactions = listOf(ImportTransaction("Transaksi!A2", DAY, "Jual teh", "Penjualan & Jasa", incoming = true, amountRupiah = 50_000, productName = "Teh", quantityText = "10")),
        )

        assertEquals(
            listOf("Transaksi!A2: produk \"Teh\" tidak ada di unit usaha ini maupun di lembar Produk."),
            repository.reviewImport(businessId, projectId, orphan).conflicts,
        )
    }

    @Test
    fun aRowWhoseDirectionDoesNotFitItsCategoryIsRefused() = runBlocking {
        val clash = WorkbookImportPreview(
            "salah.xlsx",
            "sha-clash",
            transactions = listOf(ImportTransaction("Transaksi!A2", DAY, "Gaji masuk?", "Gaji & Upah", incoming = true, amountRupiah = 100_000)),
        )

        assertEquals(listOf("Transaksi!A2: kategori \"Gaji & Upah\" bukan untuk Pemasukan."), repository.reviewImport(businessId, projectId, clash).conflicts)
        assertTrue(runCatching { repository.commitImport(businessId, projectId, clash) }.isFailure)
        assertTrue(dao.getImportBatches(projectId).isEmpty())
    }

    @Test
    fun anIdenticalFileCanBeImportedAgainAfterAWarning() = runBlocking {
        repository.commitImport(businessId, projectId, preview())

        val again = repository.reviewImport(businessId, projectId, preview())
        assertTrue(again.previouslyImported)
        // Product and stock of the same name are skipped, so 4 of the 6 rows are added, and every count says so.
        assertEquals(4, again.addedRowCount)
        assertEquals(4, repository.commitImport(businessId, projectId, preview()))
        assertEquals(listOf(6, 4), dao.getImportBatches(projectId).map { it.recordCount })

        val actual = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        assertEquals(18_000_000L, repository.accountingSummary(actual).first().cashBalanceRupiah)
    }

    /** Found by hand on the emulator: a pre-DEC-011 business had no Prive category at all. */
    @Test
    fun anOlderBusinessGetsTheBasicNonOperatingCategoriesAndImportKeepsTheirMeaning() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM categories WHERE businessId = ? AND role IN ('CAPITAL_IN', 'DRAWING', 'ASSET')",
            arrayOf(businessId),
        )
        val drawingImport = WorkbookImportPreview(
            "prive.xlsx",
            "sha-prive",
            transactions = listOf(ImportTransaction("Transaksi!A2", DAY, "Belanja rumah", "Ambil untuk Pribadi (Prive)", incoming = false, amountRupiah = 200_000)),
        )
        // Before the backfill the name is new, but it is a basic name, so it keeps the drawing role.
        assertEquals(listOf("Ambil untuk Pribadi (Prive)" to CategoryRole.DRAWING), repository.reviewImport(businessId, projectId, drawingImport).newCategories)

        repository.currentWorkspace()

        val roles = dao.getCategories(businessId).filter { it.active }.map { CategoryRole.parse(it.role) }.toSet()
        assertTrue(roles.containsAll(setOf(CategoryRole.CAPITAL_IN, CategoryRole.DRAWING, CategoryRole.ASSET)))
        repository.commitImport(businessId, projectId, drawingImport)
        val actual = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        assertEquals(FinancialEventType.OWNER_DRAWING.name, dao.getEventsForBook(actual).single().type)
        assertEquals(0L, repository.accountingSummary(actual).first().profitRupiah)
    }

    /** Schema 10: undoing an import must not clear a period the owner set after it. */
    @Test
    fun aPeriodTheOwnerChangedAfterImportSurvivesUndo() = runBlocking {
        repository.commitImport(businessId, projectId, preview())
        val batch = dao.getImportBatches(projectId).single()
        assertEquals(9_500_000L to 10_000_000L, batch.incomingRupiah to batch.totalAmountRupiah)
        repository.updateProjectDetails(projectId, "Gerai Utama", "", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30))

        val undo = repository.undoImportBatch(batch.id)

        assertFalse(undo.periodCleared)
        assertEquals(LocalDate.of(2027, 1, 1).toEpochDay(), dao.getProject(projectId)!!.startDateEpochDay)
    }

    /** Schema 10: each unit keeps its own stock; shared items are seen by all and names never clash within one view. */
    @Test
    fun stockBelongsToItsUnitAndSharedItemsAreSeenByEveryUnit() = runBlocking {
        val branch = repository.createProject(businessId, "Cabang 2")
        fun add(unit: String?, shared: Boolean, name: String) = runCatching {
            runBlocking { repository.createStockItem(businessId, unit, shared, name, "kg", 15_000, BigDecimal.ONE, BigDecimal.ZERO) }
        }

        assertTrue(add(projectId, false, "Gula").isSuccess)
        assertTrue(add(branch, false, "Gula").isSuccess)
        assertTrue(add(projectId, true, "Plastik").isSuccess)
        assertTrue(add(projectId, true, "gula").isFailure)
        assertTrue(add(branch, false, "PLASTIK").isFailure)

        val stock = database.stockDao()
        assertEquals(listOf("Gula", "Plastik"), stock.getStockItems(businessId, projectId).map { it.name })
        assertEquals(listOf("Gula", "Plastik"), stock.getStockItems(businessId, branch).map { it.name })
        assertFalse(stock.getStockItems(businessId, projectId).first().id == stock.getStockItems(businessId, branch).first().id)
        assertEquals(listOf(false, true), repository.stockItems(businessId, branch).first().map { it.shared })
        // A unit holding its own stock is archived, not deleted, so that stock is never orphaned.
        assertEquals(ScopeRemoval.ARCHIVED, repository.removeProject(branch))
    }

    @Test
    fun theRememberedUnitIsReopenedWhileItStillExists() = runBlocking {
        val branch = repository.createProject(businessId, "Cabang 2")

        assertEquals(WorkspaceSelection(businessId, branch), repository.currentWorkspace(WorkspaceSelection(businessId, branch)))
        assertEquals(WorkspaceSelection(businessId, projectId), repository.currentWorkspace(WorkspaceSelection("hilang", "hilang")))
    }

    /** Template v5 (DEC-028): sale units, a recipe with a material, and a purchase that prices the material (spec #5 #20). */
    @Test
    fun aProductWithSaleUnitsAMaterialRecipeAndPurchaseImportsLinkedAndUndoesCleanly() = runBlocking {
        assertEquals(emptyList<String>(), repository.reviewImport(businessId, projectId, keripik()).conflicts)

        repository.commitImport(businessId, projectId, keripik())

        val product = dao.getProducts(projectId).single()
        assertEquals(listOf("bungkus" to "1", "dus" to "10"), dao.getProductUnits(product.id).sortedBy { it.sortOrder }.map { it.name to it.contentText })
        val oil = database.stockDao().getStockItems(businessId, projectId).single { it.name == "Minyak" }
        val lines = dao.getRecipeLines(product.id).sortedBy { it.sortOrder }
        assertEquals(listOf("Minyak" to oil.id, "Singkong" to null), lines.map { it.name to it.stockItemId })
        assertEquals(listOf("2" to null, null to 30_000L), lines.map { it.quantityText to it.amountRupiah })
        val actual = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        val events = dao.getEventsForBook(actual).associateBy { it.description }
        assertEquals(oil.id, events.getValue("Beli minyak").stockItemId)
        assertEquals("liter", events.getValue("Beli minyak").unit)
        assertEquals("dus", events.getValue("Jual keripik").unit)
        assertEquals(27_500L, repository.materialPrice(oil.id))

        repository.undoImportBatch(dao.getImportBatches(projectId).single().id)

        assertTrue(dao.getProducts(projectId).isEmpty())
        assertTrue(database.stockDao().getStockItems(businessId, projectId).isEmpty())
        assertTrue(dao.getEventsForBook(actual).isEmpty())
    }

    /** A purchase whose amount ÷ quantity rounds to Rp0 must not zero the material's price. */
    @Test
    fun aTinyPurchaseDoesNotZeroTheMaterialPrice() = runBlocking {
        val tiny = keripik().let { p ->
            p.copy(transactions = p.transactions.map { if (it.materialName != null) it.copy(amountRupiah = 1, quantityText = "3") else it })
        }

        repository.commitImport(businessId, projectId, tiny)

        val oil = database.stockDao().getStockItems(businessId, projectId).single { it.name == "Minyak" }
        assertEquals(20_000L, repository.materialPrice(oil.id))
    }

    @Test
    fun anUnknownMaterialOrSaleUnitIsAConflict() = runBlocking {
        val bad = keripik().let { p ->
            p.copy(
                stockItems = emptyList(),
                transactions = p.transactions.map { if (it.productName != null) it.copy(unit = "karung") else it },
            )
        }

        assertEquals(
            listOf(
                "Transaksi!A2: bahan \"Minyak\" tidak ada di lembar Stok Barang maupun di usaha ini.",
                "Transaksi!A3: \"karung\" bukan satuan jual Keripik.",
                "Resep Produk!A2: bahan \"Minyak\" tidak ada di lembar Stok Barang maupun di usaha ini.",
            ),
            repository.reviewImport(businessId, projectId, bad).conflicts,
        )
    }

    /**
     * Final review I-1 (DEC-012): production and stock counts survive export then import. The
     * database-level check the workbook-only round trip missed: same stock and HPP in the new unit,
     * and undo takes the movements away with the product.
     */
    @Test
    fun productionAndStockCountsSurviveExportAndImportIntoANewUnit() = runBlocking {
        val actual = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        val keripik = repository.saveProduct(projectId, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "400",
            units = listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000), SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000)),
            recipe = listOf(RecipeLineDraft("Bumbu", amountRupiah = 2_400_000)),
        ))
        repository.recordProduction(keripik, BigDecimal.ONE, LocalDate.of(2026, 10, 1), "batch pertama")
        fun sale(day: Int, qty: String, unit: String) = EventDraft(businessId, projectId, actual, FinancialEventType.CASH_INCOME, 600_000, LocalDate.of(2026, 10, day), "Jual", "Penjualan & Jasa", quantityText = qty, unit = unit, productId = keripik)
        repository.recordEvent(sale(2, "2", "dus"))
        // Counted at the end of the same day, after that sale.
        Thread.sleep(5) // a later createdAt than the sale, as on a real phone
        repository.countProductStock(keripik, BigDecimal(300), LocalDate.of(2026, 10, 2), "")
        repository.recordEvent(sale(3, "10", "bungkus"))
        suspend fun stockIn(unit: String) = repository.products(unit).first().single().stock
        assertEquals(0, BigDecimal(290).compareTo(stockIn(projectId)))

        val preview = FinanceWorkbookImporter().preview("ekspor.xlsx", repository.exportProjectWorkbook(projectId))
        assertEquals(emptyList<String>(), preview.errors)
        val branch = repository.createProject(businessId, "Cabang 2")
        repository.commitImport(businessId, branch, preview)

        assertEquals(0, BigDecimal(290).compareTo(stockIn(branch)))
        val copy = repository.products(branch).first().single()
        assertEquals(listOf("batch pertama" to "1", "" to null), copy.movements.map { it.notes to it.batchesText })
        assertEquals(
            ProductCosting.cost(repository.products(projectId).first().single().spec, emptyMap()).perSaleUnit.map { it.hppRupiah },
            ProductCosting.cost(copy.spec, emptyMap()).perSaleUnit.map { it.hppRupiah },
        )

        repository.undoImportBatch(dao.getImportBatches(branch).single().id)

        val left = database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM product_movements").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(2, left)
    }

    /** Cleanup P2: an archived material stays archived through export and import, still linked from recipe and purchase. */
    @Test
    fun anArchivedMaterialStaysArchivedAndLinkedThroughExportAndImport() = runBlocking {
        val actual = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        val oil = repository.createStockItem(businessId, projectId, false, "Minyak", "liter", 20_000, BigDecimal.ZERO, BigDecimal.ZERO)
        repository.saveProduct(projectId, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "20",
            units = listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 5_000)),
            recipe = listOf(RecipeLineDraft("Minyak", stockItemId = oil, quantityText = "2")),
        ))
        repository.recordEvent(EventDraft(businessId, projectId, actual, FinancialEventType.CASH_EXPENSE, 55_000, DAY, "Beli minyak", "Bahan Baku & Stok", quantityText = "2", unit = "liter", stockItemId = oil))
        database.stockDao().archiveStockItem(oil, 1)

        val preview = FinanceWorkbookImporter().preview("ekspor.xlsx", repository.exportProjectWorkbook(projectId))
        assertEquals(emptyList<String>(), preview.errors)
        val branch = repository.createProject(businessId, "Cabang 2")
        repository.commitImport(businessId, branch, preview)

        val copy = database.openHelper.writableDatabase.query("SELECT id, archived FROM stock_items WHERE projectId = ?", arrayOf(branch)).use {
            assertTrue(it.moveToFirst()); assertEquals(1, it.count); it.getString(0) to it.getInt(1)
        }
        assertEquals(1, copy.second)
        val product = dao.getProducts(branch).single()
        assertEquals(listOf(copy.first), dao.getRecipeLines(product.id).map { it.stockItemId })
        val branchBook = dao.getBooks(branch).first { it.mode == BookMode.REALIZATION.name }.id
        assertEquals(copy.first, dao.getEventsForBook(branchBook).single().stockItemId)
    }

    /**
     * Cleanup P10: a shared material is priced by the latest purchase anywhere in the business
     * (DEC-028 §2), here bought by another unit. Exporting one unit must carry that price, so the
     * material costs the same after import into another business.
     */
    @Test
    fun aSharedMaterialKeepsItsCurrentPriceThroughExportAndImport() = runBlocking {
        val oil = repository.createStockItem(businessId, projectId, true, "Minyak", "liter", 20_000, BigDecimal.ZERO, BigDecimal.ZERO)
        repository.saveProduct(projectId, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "20",
            units = listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 5_000)),
            recipe = listOf(RecipeLineDraft("Minyak", stockItemId = oil, quantityText = "2")),
        ))
        val branch = repository.createProject(businessId, "Cabang 2")
        val branchBook = dao.getBooks(branch).first { it.mode == BookMode.REALIZATION.name }.id
        repository.recordEvent(EventDraft(businessId, branch, branchBook, FinancialEventType.CASH_EXPENSE, 55_000, DAY, "Beli minyak", "Bahan Baku & Stok", quantityText = "2", unit = "liter", stockItemId = oil))
        assertEquals(27_500L, repository.materialPrice(oil))

        val preview = FinanceWorkbookImporter().preview("ekspor.xlsx", repository.exportProjectWorkbook(projectId))
        assertEquals(emptyList<String>(), preview.errors)
        val (otherBusiness, otherUnit) = repository.createBusinessWithUnit("Usaha Lain", "Pusat")
        repository.commitImport(otherBusiness, otherUnit, preview)

        val copy = database.stockDao().getStockItems(otherBusiness, otherUnit).single()
        assertEquals(27_500L, repository.materialPrice(copy.id))
    }

    /**
     * Cleanup P3: undo removes what the file added, not what the owner recorded since. An imported
     * product or material that later records use stays (released from the batch) and is named;
     * the file's own production rows and unused products and materials still go.
     */
    @Test
    fun undoKeepsImportedProductsAndMaterialsThatLaterRecordsUse() = runBlocking {
        val file = keripik().let { p ->
            p.copy(
                products = p.products.map { it.copy(movements = listOf(ImportProductMovement("Produksi & Stok!A2", DAY, ProductMovementType.PRODUCTION, "20", "1"))) } +
                    ImportProduct("Produk!A3", "Rengginang", "bungkus", "10", null, 1, units = listOf(ImportSaleUnit("bungkus", "1", 4_000)), recipe = emptyList()),
                stockItems = p.stockItems + ImportStockItem("Stok Barang!A3", "Gula", "kg", 15_000, "0", "0"),
            )
        }
        repository.commitImport(businessId, projectId, file)
        val actual = dao.getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        val keripik = dao.getProducts(projectId).single { it.name == "Keripik" }.id
        val oil = database.stockDao().getStockItems(businessId, projectId).single { it.name == "Minyak" }.id
        val later = DAY.plusDays(1)
        repository.recordProduction(keripik, BigDecimal.ONE, later, "sesudah impor")
        repository.recordEvent(EventDraft(businessId, projectId, actual, FinancialEventType.CASH_INCOME, 45_000, later, "Jual keripik lagi", "Penjualan & Jasa", quantityText = "1", unit = "dus", productId = keripik))
        repository.recordEvent(EventDraft(businessId, projectId, actual, FinancialEventType.CASH_EXPENSE, 27_500, later, "Beli minyak lagi", "Bahan Baku & Stok", quantityText = "1", unit = "liter", stockItemId = oil))

        val undo = repository.undoImportBatch(dao.getImportBatches(projectId).single().id)

        assertEquals(listOf("Keripik", "Minyak"), undo.kept)
        assertEquals(listOf("Keripik"), dao.getProducts(projectId).map { it.name })
        assertEquals(null, dao.getProduct(keripik)!!.importBatchId)
        assertEquals(listOf("sesudah impor"), dao.getProductMovements(keripik).map { it.notes })
        assertEquals(setOf("Jual keripik lagi", "Beli minyak lagi"), dao.getEventsForBook(actual).map { it.description }.toSet())
        assertEquals(listOf("Minyak"), database.stockDao().getStockItems(businessId, projectId).map { it.name })
        assertEquals(null, database.stockDao().getStockItem(oil)!!.importBatchId)
        assertEquals(listOf(oil), dao.getRecipeLines(keripik).mapNotNull { it.stockItemId })
    }

    private fun keripik() = WorkbookImportPreview(
        sourceName = "keripik.xlsx",
        sourceSha256 = "sha-keripik",
        transactions = listOf(
            ImportTransaction("Transaksi!A2", DAY, "Beli minyak", "Bahan Baku & Stok", incoming = false, amountRupiah = 55_000, quantityText = "2", unit = "liter", materialName = "Minyak"),
            ImportTransaction("Transaksi!A3", DAY, "Jual keripik", "Penjualan & Jasa", incoming = true, amountRupiah = 135_000, productName = "Keripik", quantityText = "3", unit = "dus"),
        ),
        products = listOf(
            ImportProduct(
                "Produk!A2", "Keripik", "bungkus", "20", null, 1,
                units = listOf(ImportSaleUnit("bungkus", "1", 5_000), ImportSaleUnit("dus", "10", 45_000)),
                recipe = listOf(
                    ImportRecipeLine("Resep Produk!A2", "Minyak", "Minyak", "2", null),
                    ImportRecipeLine("Resep Produk!A3", "Singkong", null, null, 30_000),
                ),
            ),
        ),
        stockItems = listOf(ImportStockItem("Stok Barang!A2", "Minyak", "liter", 20_000, "0", "0")),
    )

    private fun preview() = WorkbookImportPreview(
        sourceName = "kedai.xlsx",
        sourceSha256 = "sha-kedai",
        transactions = listOf(
            ImportTransaction("Transaksi!A2", DAY, "Setoran modal", "Setoran Modal Pemilik", incoming = true, amountRupiah = 5_000_000),
            ImportTransaction("Transaksi!A3", DAY, "Jual kopi", "Penjualan & Jasa", incoming = true, amountRupiah = 4_500_000, productName = "Kopi Susu", quantityText = "450"),
            ImportTransaction("Transaksi!A4", DAY, "Sewa gerobak Agustus", "Sewa Gerobak", incoming = false, amountRupiah = 500_000),
        ),
        budgetLines = listOf(ImportBudgetLine("Rencana Anggaran!A2", YearMonth.of(2026, 8), "Bahan Baku & Stok", "Biji kopi", "10", "kg", 120_000, 1_200_000)),
        products = listOf(
            ImportProduct(
                "Produk!A2", "Kopi Susu", "cup", "6", "700", 1,
                units = listOf(ImportSaleUnit("cup", "1", 10_000)),
                recipe = listOf(ImportRecipeLine("Resep Produk!A2", "Susu", null, null, 18_000)),
            ),
        ),
        planPeriod = YearMonth.of(2026, 11) to YearMonth.of(2027, 10),
        stockItems = listOf(ImportStockItem("Stok Barang!A2", "Gula", "kg", 15_000, "5", "1")),
    )

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 8, 15)
    }
}
