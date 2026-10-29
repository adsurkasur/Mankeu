package com.mankeu.importer

import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductMovementEntity
import com.mankeu.data.ProductRecipeLineEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProductUnitEntity
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.domain.Recurrence
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.ProductMovementType
import com.mankeu.domain.StockItemEntity
import com.mankeu.exporter.FinanceExportBook
import com.mankeu.exporter.FinanceExportBundle
import com.mankeu.exporter.FinanceWorkbookExporter
import com.mankeu.exporter.TemplateCategory
import com.mankeu.exporter.WorkbookTable
import com.mankeu.exporter.XlsxWorkbookWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The single Mankeu template (DEC-012): the exporter writes it, the importer reads exactly it, and
 * anything else is rejected with the precise problem. These drive the real exporter, so a drift in
 * sheet names or headers on either side fails here.
 */
class TemplateImportTest {
    private val importer = FinanceWorkbookImporter()

    @Test
    fun `an export imports back without loss`() {
        val preview = importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(bundle()))

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(
            listOf(
                ImportTransaction("Transaksi!A2", DAY, "Setoran modal awal", "Setoran Modal Pemilik", incoming = true, amountRupiah = 5_000_000),
                ImportTransaction("Transaksi!A3", DAY, "Beli kopi", "Bahan Baku & Stok", incoming = false, amountRupiah = 1_150_000, costNature = CostNature.FIXED, quantityText = "10", unit = "kg", materialName = "Gula", unitPriceRupiah = 115_000),
                ImportTransaction("Transaksi!A4", DAY, "Jual kopi", "Penjualan & Jasa", incoming = true, amountRupiah = 4_000_000, productName = "Kopi Susu", quantityText = "40", unit = "pak"),
                ImportTransaction("Transaksi!A5", DAY, "Mesin espresso", "Pembelian Aset", incoming = false, amountRupiah = 3_600_000, usefulLifeMonths = 24),
            ),
            preview.transactions,
        )
        assertEquals(
            listOf(
                ImportBudgetLine("Rencana Anggaran!A2", YearMonth.of(2026, 8), "Bahan Baku & Stok", "Biji kopi", "10", "kg", 120_000, 1_200_000),
                ImportBudgetLine("Rencana Anggaran!A3", null, "Bahan Baku & Stok", "Sewa", null, null, null, 1_500_000, Recurrence.MONTHLY, CostNature.FIXED),
            ),
            preview.budgetLines,
        )
        assertEquals(
            listOf(
                ImportProduct(
                    source = "Produk!A2", name = "Kopi Susu", baseUnit = "cup", batchYieldText = "6", targetPerMonthText = "700", sellFromMonth = 1,
                    units = listOf(ImportSaleUnit("cup", "1", 10_000), ImportSaleUnit("pak", "10", 95_000)),
                    recipe = listOf(
                        ImportRecipeLine("Resep Produk!A2", "Susu", materialName = null, quantityText = null, amountRupiah = 18_000),
                        ImportRecipeLine("Resep Produk!A3", "Gula", materialName = "Gula", quantityText = "0.5", amountRupiah = null),
                    ),
                    movements = listOf(
                        ImportProductMovement("Produksi & Stok!A2", DAY.minusDays(1), ProductMovementType.PRODUCTION, "60", "10", "pagi"),
                        ImportProductMovement("Produksi & Stok!A3", DAY, ProductMovementType.ADJUSTMENT, "0"),
                    ),
                ),
            ),
            preview.products,
        )
        assertEquals(listOf(ImportStockItem("Stok Barang!A2", "Gula", "kg", 15_000, "2.5", "1")), preview.stockItems)
        assertEquals(YearMonth.of(2026, 11) to YearMonth.of(2027, 10), preview.planPeriod)
    }

    @Test
    fun `archived plan lines are not exported`() {
        val preview = importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(bundle()))

        assertTrue(preview.budgetLines.none { it.name == "Rencana lama" })
    }

    /** DEC-022: the description is optional on import too; a blank one takes the category name. */
    @Test
    fun `a row without Keterangan imports with its category as description`() {
        val withBlank = bundle().let { b ->
            b.copy(books = b.books.map { book ->
                if (book.book.mode != BookMode.REALIZATION.name) book
                else book.copy(events = book.events + event("e9", FinancialEventType.CASH_EXPENSE, 12_345, "", "Bahan Baku & Stok", createdAt = 9))
            })
        }

        val preview = importer.preview("tanpa-keterangan.xlsx", FinanceWorkbookExporter().export(withBlank))

        assertEquals(emptyList<String>(), preview.errors)
        assertEquals("Bahan Baku & Stok", preview.transactions.single { it.amountRupiah == 12_345L }.description)
    }

    @Test
    fun `the blank template imports as nothing, without errors`() {
        val preview = importer.preview("template.xlsx", FinanceWorkbookExporter().template(listOf(TemplateCategory("Penjualan & Jasa", CategoryRole.REVENUE, null))))

        assertEquals(emptyList<String>(), preview.errors)
        assertFalse(preview.canCommit)
    }

    @Test
    fun `a workbook that is not the template is rejected with the reason`() {
        val bytes = XlsxWorkbookWriter().write(listOf(WorkbookTable("Buku Kas", listOf("Tanggal", "Jumlah"), listOf(listOf("2026-08-01", 5_000L)))))

        val preview = importer.preview("lain.xlsx", bytes)

        assertFalse(preview.canCommit)
        assertTrue(preview.errors.first().startsWith("File ini bukan template Mankeu"))
        assertTrue(preview.errors.any { it == "Lembar \"Transaksi\" tidak ada." })
    }

    @Test
    fun `a renamed column is named exactly`() {
        val bytes = template(transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS.map { if (it == "Keterangan") "Uraian" else it }, emptyList()))

        assertEquals(listOf("Lembar \"Transaksi\" kolom B1 harus \"Keterangan\", tertulis \"Uraian\"."), importer.preview("x.xlsx", bytes).errors)
    }

    @Test
    fun `invalid cells are reported by sheet and cell`() {
        val bytes = template(
            transactions = WorkbookTable(
                "Transaksi",
                MankeuTemplate.TRANSACTION_HEADERS,
                listOf(
                    listOf("2026-08-01", "Dua kolom", "Penjualan", 10_000L, 2_000L),
                    listOf("1/8/2026", "Tanggal salah", "Penjualan", 10_000L, null),
                    listOf("2026-08-02", "Rp teks", "Penjualan", "Rp 25.000", null),
                ),
            ),
        )

        val preview = importer.preview("x.xlsx", bytes)

        assertEquals(
            listOf(
                "Transaksi!D2: isi tepat satu kolom, Pemasukan atau Pengeluaran.",
                "Transaksi!A3: tanggal harus berformat YYYY-MM-DD, tertulis \"1/8/2026\".",
            ),
            preview.errors,
        )
        assertEquals(25_000L, preview.transactions.single().amountRupiah)
    }

    @Test
    fun `a recipe line for a product not in the file is rejected`() {
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            recipes = listOf(listOf("Teh", "Gula", null, null, 1_000L)),
        )

        assertEquals(listOf("Resep Produk!A2: produk \"Teh\" tidak ada di lembar \"Produk\"."), importer.preview("x.xlsx", bytes).errors)
    }

    @Test
    fun `a malformed plan period is named`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable(MankeuTemplate.GUIDE, MankeuTemplate.GUIDE_HEADERS, listOf(listOf("Format", MankeuTemplate.MARKER), listOf(MankeuTemplate.PLAN_PERIOD, "Nov 2026 - Okt 2027"))),
                WorkbookTable(MankeuTemplate.TRANSACTIONS, MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.PRODUCTS, MankeuTemplate.PRODUCT_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.SALE_UNITS, MankeuTemplate.SALE_UNIT_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.RECIPES, MankeuTemplate.RECIPE_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.MOVEMENTS, MankeuTemplate.MOVEMENT_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList()),
            ),
        )

        assertEquals(
            listOf("Petunjuk!B3: periode rencana harus berformat YYYY-MM s.d. YYYY-MM, tertulis \"Nov 2026 - Okt 2027\"."),
            importer.preview("x.xlsx", bytes).errors,
        )
    }

    @Test
    fun `an unknown cost nature is named`() {
        val bytes = template(
            transactions = WorkbookTable(
                "Transaksi",
                MankeuTemplate.TRANSACTION_HEADERS,
                listOf(listOf("2026-08-01", "Beli", "Bahan", null, 5_000L, null, null, "Kadang")),
            ),
        )

        assertEquals(
            listOf("Transaksi!H2: Sifat Biaya harus \"Tetap\" atau \"Variabel\", tertulis \"Kadang\"."),
            importer.preview("x.xlsx", bytes).errors,
        )
    }

    @Test
    fun `an excel date cell is read as a date`() {
        val serial = LocalDate.of(2026, 8, 15).toEpochDay() - LocalDate.of(1899, 12, 30).toEpochDay()
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, listOf(listOf(serial, "Jual", "Penjualan", 5_000L, null))),
        )

        assertEquals(LocalDate.of(2026, 8, 15), importer.preview("x.xlsx", bytes).transactions.single().date)
    }

    @Test
    fun `a seasonal product keeps its first selling month through export and import`() {
        val seasonal = bundle().let { b -> b.copy(products = b.products.map { it.copy(product = it.product.copy(sellFromMonth = 3)) }) }

        assertEquals(3, importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(seasonal)).products.single().sellFromMonth)
    }

    @Test
    fun `a bad selling month is named`() {
        val bad = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            products = listOf(listOf("Cabai", "kg", "1", "300", "0"), listOf("Tomat", "kg", "1", "100", "Maret")),
            saleUnits = listOf(listOf("Cabai", "kg", "1", 20_000L), listOf("Tomat", "kg", "1", 8_000L)),
        )

        assertEquals(
            listOf(
                "Produk!E2: bulan mulai dijual harus angka bulat 1 atau lebih, tertulis \"0\".",
                "Produk!E3: bulan mulai dijual harus angka bulat 1 atau lebih, tertulis \"Maret\".",
            ),
            importer.preview("x.xlsx", bad).errors,
        )
    }

    @Test
    fun `recipe rows need either a material with a quantity or a free cost`() {
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            products = listOf(listOf("Teh", "gelas", "5", null, null)),
            saleUnits = listOf(listOf("Teh", "gelas", "1", 5_000L)),
            recipes = listOf(
                listOf("Teh", "Gula", "Gula", null, null),
                listOf("Teh", "Susu", "Susu", "2", 1_000L),
                listOf("Teh", "Es", null, null, null),
            ),
        )

        assertEquals(
            listOf(
                "Resep Produk!D2: Jumlah per Batch wajib diisi untuk bahan \"Gula\".",
                "Resep Produk!E3: baris berbahan diisi Jumlah, Biaya dibiarkan kosong.",
                "Resep Produk!E4: Biaya per Batch wajib diisi untuk komponen tanpa bahan.",
            ),
            importer.preview("x.xlsx", bytes).errors,
        )
    }

    @Test
    fun `a product without a sale unit row is named`() {
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            products = listOf(listOf("Teh", "gelas", "5", null, null)),
        )

        assertEquals(listOf("Produk!A2: produk \"Teh\" belum punya baris di lembar \"Satuan Jual\"."), importer.preview("x.xlsx", bytes).errors)
    }

    @Test
    fun `a version 4 file is rejected with the exact differences`() {
        val v4 = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable(MankeuTemplate.GUIDE, MankeuTemplate.GUIDE_HEADERS, listOf(listOf("Format", MankeuTemplate.MARKER))),
                WorkbookTable(MankeuTemplate.TRANSACTIONS, MankeuTemplate.TRANSACTION_HEADERS.dropLast(2), emptyList()),
                WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.PRODUCTS, listOf("Nama", "Satuan", "Harga Jual (Rp)", "Target per Bulan"), emptyList()),
                WorkbookTable(MankeuTemplate.RECIPES, listOf("Produk", "Komponen", "Biaya (Rp)", "Untuk Berapa Unit"), emptyList()),
                WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList()),
            ),
        )

        val errors = importer.preview("lama.xlsx", v4).errors

        assertTrue(errors.joinToString(), "Lembar \"Satuan Jual\" tidak ada." in errors)
        assertTrue(errors.joinToString(), "Lembar \"Produksi & Stok\" tidak ada." in errors)
        assertTrue(errors.joinToString(), "Lembar \"Produk\" kolom B1 harus \"Satuan Dasar\", tertulis \"Satuan\"." in errors)
    }

    @Test
    fun `an archived material used by a recipe and a purchase keeps its Bahan through export and import`() {
        val archived = StockItemEntity(id = "t", businessId = "b", name = "Tepung", unit = "kg", basePriceRupiah = 12_000, createdAtEpochMillis = 0, archived = true)
        val b = bundle()
        val withArchived = b.copy(
            stockItems = b.stockItems + archived,
            products = b.products.map { r -> r.copy(recipe = r.recipe + ProductRecipeLineEntity(id = "k3", productId = "kopi", name = "Tepung", stockItemId = "t", quantityText = "1", sortOrder = 2)) },
            books = b.books.map { book ->
                if (book.book.mode != BookMode.REALIZATION.name) book
                else book.copy(events = book.events + event("e7", FinancialEventType.CASH_EXPENSE, 24_000, "Beli tepung", "Bahan Baku & Stok", createdAt = 7).copy(stockItemId = "t", quantityText = "2", unit = "kg"))
            },
        )

        val preview = importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(withArchived))

        assertEquals(emptyList<String>(), preview.errors)
        assertEquals("Tepung", preview.products.single().recipe.single { it.name == "Tepung" }.materialName)
        assertEquals("Tepung", preview.transactions.single { it.description == "Beli tepung" }.materialName)
        // Re-import resolves a Bahan only through Stok Barang, so the archived material must be listed there,
        // and it stays archived (cleanup P2).
        assertEquals(listOf("Gula" to false, "Tepung" to true), preview.stockItems.map { it.name to it.archived })
    }

    /** Cleanup P2: Diarsipkan is "Ya" or blank, nothing else. */
    @Test
    fun `an unknown Diarsipkan value is named`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable(MankeuTemplate.GUIDE, MankeuTemplate.GUIDE_HEADERS, listOf(listOf("Format", MankeuTemplate.MARKER))),
                WorkbookTable(MankeuTemplate.TRANSACTIONS, MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.PRODUCTS, MankeuTemplate.PRODUCT_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.SALE_UNITS, MankeuTemplate.SALE_UNIT_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.RECIPES, MankeuTemplate.RECIPE_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.MOVEMENTS, MankeuTemplate.MOVEMENT_HEADERS, emptyList()),
                WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, listOf(listOf("Gula", "kg", 15_000L, "1", "0", "Ya"), listOf("Teh", "kg", 15_000L, "1", "0", "Tidak"))),
            ),
        )

        val preview = importer.preview("x.xlsx", bytes)

        assertEquals(listOf("Stok Barang!F3: Diarsipkan diisi \"Ya\" atau dibiarkan kosong, tertulis \"Tidak\"."), preview.errors)
        assertEquals(listOf("Gula" to true), preview.stockItems.map { it.name to it.archived })
    }

    @Test
    fun `a manual unit price that amount divided by quantity cannot reproduce survives the round trip`() {
        val b = bundle()
        val manual = b.copy(
            books = b.books.map { book ->
                if (book.book.mode != BookMode.REALIZATION.name) book
                else book.copy(events = book.events + event("e8", FinancialEventType.CASH_EXPENSE, 4_667, "Beli gula", "Bahan Baku & Stok", createdAt = 8).copy(stockItemId = "s", quantityText = "0.3", unit = "kg", unitPriceRupiah = 15_555))
            },
        )

        val row = importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(manual)).transactions.single { it.description == "Beli gula" }

        assertEquals(15_555L, row.unitPriceRupiah)
    }

    /** Final review M-3: a plain expense keeps Jumlah, Satuan and Harga Satuan through export and import. */
    @Test
    fun `a plain expense keeps quantity, unit and unit price through the round trip`() {
        val b = bundle()
        val plain = b.copy(
            books = b.books.map { book ->
                if (book.book.mode != BookMode.REALIZATION.name) book
                else book.copy(events = book.events + event("e10", FinancialEventType.CASH_EXPENSE, 150_000, "Beli pupuk", "Bahan Baku & Stok", createdAt = 10).copy(quantityText = "3", unit = "sak", unitPriceRupiah = 50_000))
            },
        )

        val row = importer.preview("ekspor.xlsx", FinanceWorkbookExporter().export(plain)).transactions.single { it.description == "Beli pupuk" }

        assertEquals(Triple("3", "sak", 50_000L), Triple(row.quantityText, row.unit, row.unitPriceRupiah))
    }

    /** I-1: the Produksi & Stok sheet is checked cell by cell, and only for products of the same file. */
    @Test
    fun `production and stock count rows are validated`() {
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            products = listOf(listOf("Teh", "gelas", "5", null, null)),
            saleUnits = listOf(listOf("Teh", "gelas", "1", 5_000L)),
            movements = listOf(
                listOf("2026-08-01", "Teh", "Produksi", "10", "2", "pagi"),
                listOf("2026-08-02", "Teh", "Hitung stok", "0", null, null),
                listOf("2026-08-03", "Teh", "Produksi", "0", null, null),
                listOf("2026-08-03", "Teh", "Hitung stok", "-1", null, null),
                listOf("2026-08-03", "Teh", "Hitung stok", "4", "1", null),
                listOf("2026-08-03", "Teh", "Jual", "4", null, null),
                listOf("2026-08-03", "Kopi", "Produksi", "4", null, null),
                listOf(null, "Teh", "Produksi", "4", null, null),
            ),
        )

        val preview = importer.preview("x.xlsx", bytes)

        assertEquals(
            listOf(
                "Produksi & Stok!D4: Jumlah produksi harus angka lebih dari nol, tertulis \"0\".",
                "Produksi & Stok!D5: Jumlah hitung stok harus angka nol atau lebih, tertulis \"-1\".",
                "Produksi & Stok!E6: Batch hanya diisi untuk Produksi.",
                "Produksi & Stok!C7: Jenis harus \"Produksi\" atau \"Hitung stok\", tertulis \"Jual\".",
                "Produksi & Stok!A9: Tanggal wajib diisi.",
                "Produksi & Stok!A8: produk \"Kopi\" tidak ada di lembar \"Produk\" berkas ini; produksi dan hitung stok hanya diimpor bersama produknya.",
            ),
            preview.errors,
        )
        assertEquals(
            listOf(
                ImportProductMovement("Produksi & Stok!A2", LocalDate.of(2026, 8, 1), ProductMovementType.PRODUCTION, "10", "2", "pagi"),
                ImportProductMovement("Produksi & Stok!A3", LocalDate.of(2026, 8, 2), ProductMovementType.ADJUSTMENT, "0"),
            ),
            preview.products.single().movements,
        )
    }

    /** Cleanup P5: a product named twice in one file is refused per cell instead of silently keeping the first. */
    @Test
    fun `a product written twice in the Produk sheet is named`() {
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            products = listOf(listOf("Teh", "gelas", "5", null, null), listOf("Kopi", "gelas", "5", null, null), listOf(" teh ", "gelas", "10", null, null)),
            saleUnits = listOf(listOf("Teh", "gelas", "1", 5_000L), listOf("Kopi", "gelas", "1", 6_000L)),
        )

        val preview = importer.preview("x.xlsx", bytes)

        assertEquals(listOf("Produk!A4: produk \"teh\" ditulis dua kali di lembar \"Produk\"."), preview.errors)
        assertFalse(preview.canCommit)
    }

    // --- fixtures ---------------------------------------------------------------------------

    /** A minimal valid template whose Transaksi sheet is [transactions]; products and units default to none. */
    private fun template(
        transactions: WorkbookTable,
        recipes: List<List<Any?>> = emptyList(),
        products: List<List<Any?>> = emptyList(),
        saleUnits: List<List<Any?>> = emptyList(),
        movements: List<List<Any?>> = emptyList(),
    ): ByteArray = XlsxWorkbookWriter().write(
        listOf(
            WorkbookTable(MankeuTemplate.GUIDE, MankeuTemplate.GUIDE_HEADERS, listOf(listOf("Format", MankeuTemplate.MARKER))),
            transactions,
            WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList()),
            WorkbookTable(MankeuTemplate.PRODUCTS, MankeuTemplate.PRODUCT_HEADERS, products),
            WorkbookTable(MankeuTemplate.SALE_UNITS, MankeuTemplate.SALE_UNIT_HEADERS, saleUnits),
            WorkbookTable(MankeuTemplate.RECIPES, MankeuTemplate.RECIPE_HEADERS, recipes),
            WorkbookTable(MankeuTemplate.MOVEMENTS, MankeuTemplate.MOVEMENT_HEADERS, movements),
            WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList()),
        ),
    )

    private fun bundle() = FinanceExportBundle(
        business = BusinessEntity(id = "b", name = "Kedai Kopi Berkah", createdAtEpochMillis = 0),
        project = ProjectEntity(
            id = "p", businessId = "b", name = "Gerai Utama", isGeneral = false, createdAtEpochMillis = 0,
            startDateEpochDay = LocalDate.of(2026, 11, 1).toEpochDay(), endDateEpochDay = LocalDate.of(2027, 10, 31).toEpochDay(),
        ),
        categories = listOf(
            category("c-bahan", "Bahan Baku & Stok", CategoryRole.EXPENSE),
            category("c-jual", "Penjualan & Jasa", CategoryRole.REVENUE),
            category("c-modal", "Setoran Modal Pemilik", CategoryRole.CAPITAL_IN),
            category("c-aset", "Pembelian Aset", CategoryRole.ASSET),
        ),
        books = listOf(
            FinanceExportBook(
                book = BookEntity(id = "plan", projectId = "p", mode = BookMode.PROJECTION.name, createdAtEpochMillis = 0),
                budgets = listOf(
                    budget("Biji kopi", 1_200_000),
                    budget("Rencana lama", 999_000, archived = true),
                    budget("Sewa", 1_500_000).copy(
                        quantityText = null, unit = null, unitPriceRupiah = null, plannedDateEpochDay = null,
                        recurrence = Recurrence.MONTHLY.name, costNature = CostNature.FIXED.name, sortOrder = 2,
                    ),
                ),
                events = emptyList(),
                summary = EMPTY,
            ),
            FinanceExportBook(
                book = BookEntity(id = "actual", projectId = "p", mode = BookMode.REALIZATION.name, createdAtEpochMillis = 0),
                budgets = emptyList(),
                events = listOf(
                    event("e1", FinancialEventType.OWNER_CAPITAL, 5_000_000, "Setoran modal awal", "Setoran Modal Pemilik", createdAt = 1),
                    event("e2", FinancialEventType.CASH_EXPENSE, 1_150_000, "Beli kopi", "Bahan Baku & Stok", createdAt = 2)
                        .copy(costNature = CostNature.FIXED.name, stockItemId = "s", quantityText = "10", unit = "kg", unitPriceRupiah = 115_000),
                    event("e3", FinancialEventType.CASH_INCOME, 4_000_000, "Jual kopi", "Penjualan & Jasa", createdAt = 3)
                        .copy(productId = "kopi", quantityText = "40", unit = "pak"),
                    event("e6", FinancialEventType.ASSET_PURCHASE_CASH, 3_600_000, "Mesin espresso", "Pembelian Aset", createdAt = 3)
                        .copy(usefulLifeMonths = 24),
                ),
                summary = EMPTY,
            ),
        ),
        importBatches = emptyList(),
        notices = emptyList(),
        provenance = emptyList(),
        products = listOf(
            ProductRecord(
                ProductEntity(id = "kopi", projectId = "p", name = "Kopi Susu", unit = "cup", batchYieldText = "6", targetPerMonthText = "700", createdAtEpochMillis = 0),
                listOf(
                    ProductUnitEntity(id = "u1", productId = "kopi", name = "cup", contentText = "1", priceRupiah = 10_000, sortOrder = 0),
                    ProductUnitEntity(id = "u2", productId = "kopi", name = "pak", contentText = "10", priceRupiah = 95_000, sortOrder = 1),
                ),
                listOf(
                    ProductRecipeLineEntity(id = "k1", productId = "kopi", name = "Susu", amountRupiah = 18_000, sortOrder = 0),
                    ProductRecipeLineEntity(id = "k2", productId = "kopi", name = "Gula", stockItemId = "s", quantityText = "0.5", sortOrder = 1),
                ),
                // Out of order on purpose: the export writes them oldest first.
                listOf(
                    ProductMovementEntity("m2", "kopi", ProductMovementType.ADJUSTMENT.name, "0", dateEpochDay = DAY.toEpochDay(), createdAtEpochMillis = 5),
                    ProductMovementEntity("m1", "kopi", ProductMovementType.PRODUCTION.name, "60", "10", DAY.minusDays(1).toEpochDay(), "pagi", createdAtEpochMillis = 9),
                ),
            ),
        ),
        stockItems = listOf(
            StockItemEntity(id = "s", businessId = "b", name = "Gula", unit = "kg", basePriceRupiah = 15_000, currentStockText = "2.5", minAlertStockText = "1", createdAtEpochMillis = 0),
        ),
    )

    private fun category(id: String, name: String, role: CategoryRole) =
        CategoryEntity(id = id, businessId = "b", role = role.name, name = name, normalizedName = name.lowercase(), createdAtEpochMillis = 0)

    private fun budget(name: String, total: Long, archived: Boolean = false) = BudgetItemEntity(
        id = name,
        bookId = "plan",
        categoryId = "c-bahan",
        name = name,
        normalizedName = name.lowercase(),
        quantityText = "10",
        unit = "kg",
        unitPriceRupiah = total / 10,
        plannedTotalRupiah = total,
        plannedDateEpochDay = LocalDate.of(2026, 8, 1).toEpochDay(),
        sortOrder = 1,
        archived = archived,
        createdAtEpochMillis = 0,
    )

    private fun event(
        id: String,
        type: FinancialEventType,
        amount: Long,
        description: String,
        category: String,
        createdAt: Long,
    ) = FinancialEventEntity(
        id = id,
        businessId = "b",
        projectId = "p",
        bookId = "actual",
        type = type.name,
        amountRupiah = amount,
        dateEpochDay = DAY.toEpochDay(),
        description = description,
        category = category,
        cashFlowRole = "OPERATING",
        createdAtEpochMillis = createdAt,
    )

    private companion object {
        val DAY: LocalDate = LocalDate.of(2026, 8, 15)
        val EMPTY = FinancialSummary(0, 0, 0, 0, 0, 0)
    }
}
