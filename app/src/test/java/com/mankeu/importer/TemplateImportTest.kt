package com.mankeu.importer

import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ProductCostComponentEntity
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductWithComponents
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.domain.Recurrence
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
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
                ImportTransaction("Transaksi!A3", DAY, "Beli kopi", "Bahan Baku & Stok", incoming = false, amountRupiah = 1_150_000, costNature = CostNature.FIXED),
                ImportTransaction("Transaksi!A4", DAY, "Jual kopi", "Penjualan & Jasa", incoming = true, amountRupiah = 4_000_000, productName = "Kopi Susu", quantityText = "400"),
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
                    "Produk!A2", "Kopi Susu", "cup", 10_000, "700",
                    listOf(ImportComponent("Susu", 18_000, "6"), ImportComponent("Cup", 500, "1")),
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
            recipes = listOf(listOf("Teh", "Gula", 1_000L, "10")),
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
                WorkbookTable(MankeuTemplate.RECIPES, MankeuTemplate.RECIPE_HEADERS, emptyList()),
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
    fun `a version 3 file without the selling month column still imports, selling from month 1`() {
        val bytes = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            products = listOf(listOf("Cabai", "kg", 20_000L, "300")),
        )

        val preview = importer.preview("lama.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.errors.isEmpty())
        assertEquals(1, preview.products.single().sellFromMonth)
    }

    @Test
    fun `a bad selling month and a renamed selling month column are named`() {
        val headers = MankeuTemplate.PRODUCT_HEADERS + MankeuTemplate.OPTIONAL.getValue(MankeuTemplate.PRODUCTS)
        val bad = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            products = listOf(listOf("Cabai", "kg", 20_000L, "300", "0"), listOf("Tomat", "kg", 8_000L, "100", "Maret")),
            productHeaders = headers,
        )
        val renamed = template(
            transactions = WorkbookTable("Transaksi", MankeuTemplate.TRANSACTION_HEADERS, emptyList()),
            productHeaders = MankeuTemplate.PRODUCT_HEADERS + "Bulan Panen",
        )

        assertEquals(
            listOf(
                "Produk!E2: bulan mulai dijual harus angka bulat 1 atau lebih, tertulis \"0\".",
                "Produk!E3: bulan mulai dijual harus angka bulat 1 atau lebih, tertulis \"Maret\".",
            ),
            importer.preview("x.xlsx", bad).errors,
        )
        assertEquals(
            listOf("Lembar \"Produk\" kolom E1 harus \"Mulai Dijual (bulan ke-)\" atau kosong, tertulis \"Bulan Panen\"."),
            importer.preview("x.xlsx", renamed).errors,
        )
    }

    // --- fixtures ---------------------------------------------------------------------------

    /** A minimal valid template whose Transaksi sheet is [transactions]; products default to the v3 columns. */
    private fun template(
        transactions: WorkbookTable,
        recipes: List<List<Any?>> = emptyList(),
        products: List<List<Any?>> = emptyList(),
        productHeaders: List<String> = MankeuTemplate.PRODUCT_HEADERS,
    ): ByteArray = XlsxWorkbookWriter().write(
        listOf(
            WorkbookTable(MankeuTemplate.GUIDE, MankeuTemplate.GUIDE_HEADERS, listOf(listOf("Format", MankeuTemplate.MARKER))),
            transactions,
            WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList()),
            WorkbookTable(MankeuTemplate.PRODUCTS, productHeaders, products),
            WorkbookTable(MankeuTemplate.RECIPES, MankeuTemplate.RECIPE_HEADERS, recipes),
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
                        .copy(costNature = CostNature.FIXED.name),
                    event("e3", FinancialEventType.CASH_INCOME, 4_000_000, "Jual kopi", "Penjualan & Jasa", createdAt = 3)
                        .copy(productId = "kopi", quantityText = "400"),
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
            ProductWithComponents(
                ProductEntity(id = "kopi", projectId = "p", name = "Kopi Susu", unit = "cup", priceRupiah = 10_000, targetPerMonthText = "700", createdAtEpochMillis = 0),
                listOf(
                    ProductCostComponentEntity(id = "k1", productId = "kopi", name = "Susu", amountRupiah = 18_000, yieldText = "6", sortOrder = 0),
                    ProductCostComponentEntity(id = "k2", productId = "kopi", name = "Cup", amountRupiah = 500, yieldText = "1", sortOrder = 1),
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
