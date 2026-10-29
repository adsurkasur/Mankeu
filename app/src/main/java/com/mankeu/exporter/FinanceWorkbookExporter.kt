package com.mankeu.exporter

import com.mankeu.domain.TransactionCalculation
import com.mankeu.data.BookEntity
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.BusinessEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.FinancialEventEntity
import com.mankeu.data.ImportBatchEntity
import com.mankeu.data.ImportNoticeEntity
import com.mankeu.data.PlanAssembly
import com.mankeu.data.ProductRecord
import com.mankeu.data.ProjectEntity
import com.mankeu.data.ProvenanceEntity
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.FinancialSummary
import com.mankeu.domain.PlanAnalysis
import com.mankeu.domain.Recurrence
import com.mankeu.domain.StockItemEntity
import com.mankeu.importer.MankeuTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

data class FinanceExportBook(
    val book: BookEntity,
    val budgets: List<BudgetItemEntity>,
    val events: List<FinancialEventEntity>,
    val summary: FinancialSummary,
)

data class FinanceExportBundle(
    val business: BusinessEntity,
    val project: ProjectEntity,
    val categories: List<CategoryEntity>,
    val books: List<FinanceExportBook>,
    val importBatches: List<ImportBatchEntity>,
    val notices: List<ImportNoticeEntity>,
    val provenance: List<ProvenanceEntity>,
    val stockItems: List<StockItemEntity> = emptyList(),
    val products: List<ProductRecord> = emptyList(),
    /** The business plan's BEP (DEC-014), computed by the repository from the same rows. */
    val plan: PlanAnalysis? = null,
    val generatedAtEpochMillis: Long = System.currentTimeMillis(),
)

/** A category as the template's reference sheet lists it. */
data class TemplateCategory(val name: String, val role: CategoryRole, val nature: CostNature?)

/**
 * Writes [MankeuTemplate] (DEC-012, v3). [template] is the blank form; [export] is the same sheets
 * filled from a business unit, so an export imports back without loss. The export also appends
 * two calculated sheets (Laporan, Kelayakan) that the importer ignores.
 */
class FinanceWorkbookExporter(
    private val writer: XlsxWorkbookWriter = XlsxWorkbookWriter(),
) {
    fun template(categories: List<TemplateCategory>): ByteArray = writer.write(
        listOf(
            guide(emptyList()),
            WorkbookTable(MankeuTemplate.TRANSACTIONS, MankeuTemplate.TRANSACTION_HEADERS, emptyList(), setOf(3, 4, 11)),
            WorkbookTable(MankeuTemplate.BUDGET, MankeuTemplate.BUDGET_HEADERS, emptyList(), setOf(5, 6)),
            WorkbookTable(MankeuTemplate.PRODUCTS, MankeuTemplate.PRODUCT_HEADERS, emptyList()),
            WorkbookTable(MankeuTemplate.SALE_UNITS, MankeuTemplate.SALE_UNIT_HEADERS, emptyList(), setOf(3)),
            WorkbookTable(MankeuTemplate.RECIPES, MankeuTemplate.RECIPE_HEADERS, emptyList(), setOf(4)),
            WorkbookTable(MankeuTemplate.STOCK, MankeuTemplate.STOCK_HEADERS, emptyList(), setOf(2)),
            categoryTable(categories),
        ),
    )

    fun export(bundle: FinanceExportBundle): ByteArray {
        val books = bundle.books.associateBy { BookMode.valueOf(it.book.mode) }
        val categories = bundle.categories.associateBy { it.id }
        val productNames = bundle.products.associate { it.product.id to it.product.name }
        val stockNames = bundle.stockItems.associate { it.id to it.name }
        val referencedStock = bundle.products.flatMap { p -> p.recipe.mapNotNull { it.stockItemId } }.toSet() +
            bundle.books.flatMap { b -> b.events.mapNotNull { it.stockItemId } }
        return writer.write(
            listOfNotNull(
                guide(
                    listOf(
                        listOf("Usaha", bundle.business.name),
                        listOf("Unit usaha", bundle.project.name),
                        listOf(
                            "Dibuat pada",
                            Instant.ofEpochMilli(bundle.generatedAtEpochMillis).atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime().withNano(0).toString().replace('T', ' '),
                        ),
                    ) + listOfNotNull(planPeriodRow(bundle.project)),
                ),
                transactions(books[BookMode.REALIZATION]?.events.orEmpty(), productNames, stockNames),
                budget(books[BookMode.PROJECTION]?.budgets.orEmpty(), categories),
                products(bundle.products),
                saleUnits(bundle.products),
                recipes(bundle.products, stockNames),
                stock(bundle.stockItems, referencedStock),
                categoryTable(bundle.categories.filter { it.active }.map { TemplateCategory(it.name, CategoryRole.parse(it.role), PlanAssembly.nature(it.costNature)) }),
                reportTable(bundle.books),
                bundle.plan?.let(::feasibilityTable),
            ),
        )
    }

    /** The unit's plan period, so an export re-imports with the same months (one-off costs spread the same). */
    private fun planPeriodRow(project: ProjectEntity): List<Any?>? {
        val start = project.startDateEpochDay ?: return null
        val end = project.endDateEpochDay ?: return null
        return listOf(
            MankeuTemplate.PLAN_PERIOD,
            YearMonth.from(LocalDate.ofEpochDay(start)).toString() + MankeuTemplate.PERIOD_SEPARATOR + YearMonth.from(LocalDate.ofEpochDay(end)),
        )
    }

    /** A count typed as text ("1500", "2,5") goes out as a number cell, so Excel can add it up. */
    private fun quantity(text: String?): Any? = text?.let { TransactionCalculation.parseQuantity(it) ?: it }

    private fun guide(context: List<List<Any?>>) = WorkbookTable(
        name = MankeuTemplate.GUIDE,
        headers = MankeuTemplate.GUIDE_HEADERS,
        rows = listOf(
            listOf("Format", MankeuTemplate.MARKER),
            listOf("Versi format", MankeuTemplate.VERSION),
        ) + context + listOf(
            listOf("Transaksi", "Satu baris satu transaksi buku Realisasi. Isi Pemasukan ATAU Pengeluaran saja. Tanggal YYYY-MM-DD. Penjualan: Produk, Jumlah, dan Satuan (satuan jual produk itu). Belanja bahan: Bahan (nama di lembar Stok Barang), Jumlah, dan Satuan beli; harga bahan mengikuti belanja terbaru. Harga Satuan (Rp) opsional; kosong berarti Pengeluaran dibagi Jumlah. Sifat Biaya (Tetap/Variabel) opsional, mengganti sifat kategori. Umur Pakai hanya untuk beli aset."),
            listOf("Kategori", "Tulis nama kategori. Kategori yang belum ada dibuat otomatis saat impor. Lihat daftar di lembar Kategori."),
            listOf("Modal & prive", "Setoran modal: Pemasukan berkategori 'Setoran Modal Pemilik'. Ambil uang pribadi: Pengeluaran berkategori 'Ambil untuk Pribadi (Prive)'. Beli alat: Pengeluaran berkategori 'Beli Peralatan & Aset'."),
            listOf("Rencana Anggaran", "Biaya tetap, biaya sekali bayar, aset, dan rencana setoran modal buku Proyeksi. Berulang: Bulanan (tiap bulan periode) atau Sekali. Bulan YYYY-MM."),
            listOf("Produk & Resep", "Produk: satuan dasar, hasil per batch, target terjual per bulan, dan bulan mulai dijual (1 = bulan pertama periode). Satuan Jual: tiap satuan jual produk, isinya dalam satuan dasar, dan harga jualnya; baris pertama jadi satuan utama. Resep: isi Bahan dan Jumlah per Batch untuk bahan di Stok Barang, atau Biaya per Batch saja untuk biaya bebas."),
            listOf("Stok Barang", "Barang yang namanya sudah ada di aplikasi dilewati."),
            listOf("Aturan", "Jangan ubah nama lembar atau judul kolom. Lembar Laporan dan Kelayakan hanya hasil hitungan dan diabaikan saat impor."),
        ),
    )

    /** Cash rows: what an owner would write in a cash book. */
    private fun transactions(events: List<FinancialEventEntity>, productNames: Map<String, String>, stockNames: Map<String, String>) = WorkbookTable(
        name = MankeuTemplate.TRANSACTIONS,
        headers = MankeuTemplate.TRANSACTION_HEADERS,
        rows = events
            .sortedWith(compareBy({ it.dateEpochDay }, { it.createdAtEpochMillis }))
            .map { event ->
                val incoming = event.type == FinancialEventType.CASH_INCOME.name || event.type == FinancialEventType.OWNER_CAPITAL.name
                listOf(
                    LocalDate.ofEpochDay(event.dateEpochDay).toString(),
                    event.description,
                    event.category,
                    event.amountRupiah.takeIf { incoming },
                    event.amountRupiah.takeUnless { incoming },
                    event.productId?.let(productNames::get),
                    quantity(event.quantityText),
                    MankeuTemplate.natureLabel(PlanAssembly.nature(event.costNature)),
                    event.usefulLifeMonths,
                    event.unit,
                    event.stockItemId?.let(stockNames::get),
                    event.unitPriceRupiah,
                )
            },
        currencyColumns = setOf(3, 4, 11),
    )

    private fun budget(items: List<BudgetItemEntity>, categories: Map<String, CategoryEntity>) = WorkbookTable(
        name = MankeuTemplate.BUDGET,
        headers = MankeuTemplate.BUDGET_HEADERS,
        rows = items.filterNot { it.archived }.sortedBy { it.sortOrder }.map { item ->
            listOf(
                item.plannedDateEpochDay?.let { YearMonth.from(LocalDate.ofEpochDay(it)).toString() },
                categories[item.categoryId]?.name ?: item.categoryId,
                item.name,
                quantity(item.quantityText),
                item.unit,
                item.unitPriceRupiah,
                item.plannedTotalRupiah,
                if (item.recurrence == Recurrence.MONTHLY.name) MankeuTemplate.MONTHLY else MankeuTemplate.ONCE,
                MankeuTemplate.natureLabel(PlanAssembly.nature(item.costNature)),
                item.usefulLifeMonths,
            )
        },
        currencyColumns = setOf(5, 6),
    )

    private fun products(products: List<ProductRecord>) = WorkbookTable(
        name = MankeuTemplate.PRODUCTS,
        headers = MankeuTemplate.PRODUCT_HEADERS,
        rows = products.map { p ->
            listOf(p.product.name, p.product.unit, quantity(p.product.batchYieldText), quantity(p.product.targetPerMonthText), p.product.sellFromMonth)
        },
    )

    private fun saleUnits(products: List<ProductRecord>) = WorkbookTable(
        name = MankeuTemplate.SALE_UNITS,
        headers = MankeuTemplate.SALE_UNIT_HEADERS,
        rows = products.flatMap { p ->
            p.units.sortedBy { it.sortOrder }.map { listOf(p.product.name, it.name, quantity(it.contentText), it.priceRupiah) }
        },
        currencyColumns = setOf(3),
    )

    private fun recipes(products: List<ProductRecord>, stockNames: Map<String, String>) = WorkbookTable(
        name = MankeuTemplate.RECIPES,
        headers = MankeuTemplate.RECIPE_HEADERS,
        rows = products.flatMap { p ->
            p.recipe.sortedBy { it.sortOrder }.map {
                listOf(p.product.name, it.name, it.stockItemId?.let(stockNames::get), quantity(it.quantityText), it.amountRupiah)
            }
        },
        currencyColumns = setOf(4),
    )

    /** Archived items are written only when something still names them, so the re-import resolves every Bahan. */
    private fun stock(items: List<StockItemEntity>, referenced: Set<String>) = WorkbookTable(
        name = MankeuTemplate.STOCK,
        headers = MankeuTemplate.STOCK_HEADERS,
        rows = items.filter { !it.archived || it.id in referenced }.map { listOf(it.name, it.unit, it.basePriceRupiah, quantity(it.currentStockText), quantity(it.minAlertStockText)) },
        currencyColumns = setOf(2),
    )

    private fun categoryTable(categories: List<TemplateCategory>) = WorkbookTable(
        name = MankeuTemplate.CATEGORIES,
        headers = MankeuTemplate.CATEGORY_HEADERS,
        rows = categories.map { listOf(it.name, roleLabel(it.role), MankeuTemplate.natureLabel(it.nature)) },
    )

    private fun reportTable(books: List<FinanceExportBook>) = WorkbookTable(
        name = "Laporan",
        headers = listOf("Buku", "Metrik", "Nilai"),
        rows = books.flatMap { book ->
            val mode = BookMode.valueOf(book.book.mode).label()
            with(book.summary) {
                listOf(
                    listOf(mode, "Pendapatan", operatingIncomeRupiah),
                    listOf(mode, "Biaya operasional", operatingExpenseRupiah),
                    listOf(mode, "Laba atau rugi", profitRupiah),
                    listOf(mode, "Arus kas operasi", operatingCashFlowRupiah),
                    listOf(mode, "Arus kas investasi", investingCashFlowRupiah),
                    listOf(mode, "Arus kas pembiayaan (modal/prive)", financingCashFlowRupiah),
                    listOf(mode, "Saldo kas", cashBalanceRupiah),
                )
            }
        },
        currencyColumns = setOf(2),
    )

    /** The plan's BEP laid out like the lecturer's benchmark: cost items, margins, BEP, ratios. */
    private fun feasibilityTable(plan: PlanAnalysis) = WorkbookTable(
        name = "Kelayakan",
        headers = listOf("Bagian", "Rincian", "Nilai (Rp / unit)"),
        rows = buildList {
            plan.fixedCost.recurring.forEach { add(listOf("Biaya tetap / bulan", it.first, it.second)) }
            plan.fixedCost.oneOffSpread.forEach { add(listOf("Biaya tetap / bulan", "${it.first} (sekali, dibagi rata)", it.second)) }
            plan.fixedCost.depreciation.forEach { add(listOf("Biaya tetap / bulan", "Penyusutan ${it.first}", it.second)) }
            add(listOf("Biaya tetap / bulan", "Total biaya tetap (FC)", plan.fixedCost.totalRupiah))
            if (plan.breakEvenBaseRupiah != plan.fixedCost.totalRupiah) {
                add(listOf("Biaya tetap / bulan", "Dasar BEP (+ biaya variabel di luar resep, - pendapatan lain)", plan.breakEvenBaseRupiah))
            }
            plan.products.forEach { p ->
                add(listOf(p.product.name, "Harga jual (P)", p.product.priceRupiah))
                p.product.components.forEach { add(listOf(p.product.name, "Biaya variabel: ${it.name}", PlanAnalysisFormat.money(it.perUnit))) }
                add(listOf(p.product.name, "Biaya variabel per unit (VC)", p.variableCostPerUnit))
                add(listOf(p.product.name, "Margin kontribusi (CM)", p.contributionMargin))
                add(listOf(p.product.name, "Porsi penjualan (%)", p.mixShare?.let { PlanAnalysisFormat.percentValue(it) }))
                if (p.product.sellFromMonth > 1) add(listOf(p.product.name, "Mulai dijual (bulan ke-)", p.product.sellFromMonth))
                add(listOf(p.product.name, if (plan.seasonal) "BEP (${p.product.unit} per bulan jual)" else "BEP (${p.product.unit})", p.breakEvenUnits))
                add(listOf(p.product.name, "BEP (Rp)", p.breakEvenRupiah))
            }
            add(listOf("Total", "BEP semua produk (unit)", plan.breakEvenUnits))
            add(listOf("Total", "BEP omzet (Rp)", plan.breakEvenRupiah))
            add(listOf("Total", if (plan.seasonal) "Omzet target / bulan (rata-rata periode)" else "Omzet target / bulan", plan.revenueRupiah))
            add(listOf("Total", "Total biaya / bulan", plan.totalCostRupiah))
            add(listOf("Total", "Laba / bulan", plan.profitRupiah))
            add(listOf("Total", "R/C Ratio", plan.revenueCostRatio))
            add(listOf("Total", "B/C Ratio", plan.benefitCostRatio))
            add(listOf("Total", "Status kelayakan", plan.feasible?.let { if (it) "LAYAK" else "BELUM LAYAK" }))
            add(listOf("Total", "Kebutuhan modal (defisit terdalam sebelum setoran)", plan.capitalNeedRupiah))
            add(listOf("Total", "Setoran modal direncanakan", plan.plannedCapitalRupiah))
            add(listOf("Total", "Modal yang masih kurang", plan.capitalShortfallRupiah))
        },
    )

    private fun roleLabel(role: CategoryRole) = when (role) {
        CategoryRole.REVENUE -> "Pemasukan"
        CategoryRole.EXPENSE -> "Pengeluaran"
        CategoryRole.CAPITAL_IN -> "Setoran modal (Pemasukan)"
        CategoryRole.DRAWING -> "Ambil pribadi (Pengeluaran)"
        CategoryRole.ASSET -> "Beli aset (Pengeluaran)"
    }

    private fun BookMode.label(): String = when (this) {
        BookMode.PROJECTION -> "Proyeksi"
        BookMode.REALIZATION -> "Realisasi"
    }
}

/** Formatting shared by the workbook and PDF breakdowns. */
internal object PlanAnalysisFormat {
    fun money(value: java.math.BigDecimal): Long = value.setScale(0, java.math.RoundingMode.HALF_UP).toLong()
    fun percentValue(share: java.math.BigDecimal): java.math.BigDecimal =
        share.multiply(java.math.BigDecimal(100)).setScale(1, java.math.RoundingMode.HALF_UP)

    /** Indonesian decimals for reading ("851,955", "1,42"); a dot there reads as thousands. */
    fun decimal(value: java.math.BigDecimal): String =
        java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("id-ID")).apply { maximumFractionDigits = 3 }.format(value)
}
