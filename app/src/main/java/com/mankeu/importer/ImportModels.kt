package com.mankeu.importer

import com.mankeu.domain.CostNature
import com.mankeu.domain.Recurrence
import java.time.LocalDate
import java.time.YearMonth

/**
 * The one Excel format Mankeu reads and writes (DEC-012). The blank template is the export with no
 * rows; [FinanceWorkbookImporter] reads exactly this and rejects anything else. Sheet names and
 * headers live here only, so the exporter and the importer cannot drift apart.
 *
 * Version 3 (DEC-014): products and recipes replace the BEP parameter sheet, and plan lines and
 * transactions carry cost nature, recurrence, asset life, product and quantity.
 * Version 5 (DEC-028): products have a base unit and batch yield, sale units and recipe lines
 * (material or free cost per batch) have their own sheets, and Transaksi carries unit and material.
 * Older files are rejected by their headers.
 */
object MankeuTemplate {
    const val MARKER = "MANKEU_TEMPLATE"
    const val VERSION = 5

    const val GUIDE = "Petunjuk"
    const val TRANSACTIONS = "Transaksi"
    const val BUDGET = "Rencana Anggaran"
    const val PRODUCTS = "Produk"
    const val SALE_UNITS = "Satuan Jual"
    const val RECIPES = "Resep Produk"
    const val STOCK = "Stok Barang"
    const val CATEGORIES = "Kategori"

    val TRANSACTION_HEADERS = listOf(
        "Tanggal", "Keterangan", "Kategori", "Pemasukan (Rp)", "Pengeluaran (Rp)",
        "Produk", "Jumlah", "Sifat Biaya", "Umur Pakai (bulan)", "Satuan", "Bahan", "Harga Satuan (Rp)",
    )
    val BUDGET_HEADERS = listOf(
        "Bulan", "Kategori", "Item", "Jumlah", "Satuan", "Harga Satuan (Rp)", "Total (Rp)",
        "Berulang", "Sifat Biaya", "Umur Pakai (bulan)",
    )
    val PRODUCT_HEADERS = listOf("Nama", "Satuan Dasar", "Hasil per Batch", "Target per Bulan", "Mulai Dijual (bulan ke-)")
    val SALE_UNIT_HEADERS = listOf("Produk", "Satuan", "Isi (satuan dasar)", "Harga Jual (Rp)")
    val RECIPE_HEADERS = listOf("Produk", "Komponen", "Bahan", "Jumlah per Batch", "Biaya per Batch (Rp)")
    val STOCK_HEADERS = listOf("Nama", "Satuan", "Harga Beli (Rp)", "Jumlah Stok", "Batas Minimum")
    val CATEGORY_HEADERS = listOf("Nama", "Peran", "Sifat Biaya")
    val GUIDE_HEADERS = listOf("Kunci", "Nilai")

    const val NATURE_FIXED = "Tetap"
    const val NATURE_VARIABLE = "Variabel"
    const val MONTHLY = "Bulanan"
    /** Optional `Petunjuk` row: the unit's plan period as `YYYY-MM s.d. YYYY-MM`. */
    const val PLAN_PERIOD = "Periode rencana"
    const val PERIOD_SEPARATOR = " s.d. "
    const val ONCE = "Sekali"

    /** Sheets that must exist with exactly these headers in row 1. [CATEGORIES] is reference only. */
    val REQUIRED = linkedMapOf(
        GUIDE to GUIDE_HEADERS,
        TRANSACTIONS to TRANSACTION_HEADERS,
        BUDGET to BUDGET_HEADERS,
        PRODUCTS to PRODUCT_HEADERS,
        SALE_UNITS to SALE_UNIT_HEADERS,
        RECIPES to RECIPE_HEADERS,
        STOCK to STOCK_HEADERS,
    )

    fun natureLabel(nature: CostNature?): String? = when (nature) {
        CostNature.FIXED -> NATURE_FIXED
        CostNature.VARIABLE -> NATURE_VARIABLE
        null -> null
    }
}

/** One `Transaksi` row. The cash type is decided at commit from the category's role (DEC-011 K-2). */
data class ImportTransaction(
    val source: String,
    val date: LocalDate,
    val description: String,
    val categoryName: String,
    val incoming: Boolean,
    val amountRupiah: Long,
    val productName: String? = null,
    val quantityText: String? = null,
    val costNature: CostNature? = null,
    val usefulLifeMonths: Int? = null,
    /** Sale unit (sales) or purchase unit (material purchases). */
    val unit: String? = null,
    val materialName: String? = null,
    /** As typed in the entry form; blank means amount / quantity (material purchases). */
    val unitPriceRupiah: Long? = null,
)

/** One `Rencana Anggaran` row, for the Proyeksi book. */
data class ImportBudgetLine(
    val source: String,
    val month: YearMonth?,
    val categoryName: String,
    val name: String,
    val quantityText: String?,
    val unit: String?,
    val unitPriceRupiah: Long?,
    val totalRupiah: Long,
    val recurrence: Recurrence = Recurrence.ONCE,
    val costNature: CostNature? = null,
    val usefulLifeMonths: Int? = null,
)

/** One `Satuan Jual` row: [contentText] base units per [name]. */
data class ImportSaleUnit(val name: String, val contentText: String, val priceRupiah: Long)

/** One `Resep Produk` row: a material with a quantity per batch, or a free cost per batch. */
data class ImportRecipeLine(val source: String, val name: String, val materialName: String?, val quantityText: String?, val amountRupiah: Long?)

/** One `Produk` row with its `Satuan Jual` and `Resep Produk` rows. */
data class ImportProduct(
    val source: String,
    val name: String,
    val baseUnit: String,
    val batchYieldText: String,
    val targetPerMonthText: String?,
    val sellFromMonth: Int = 1,
    val units: List<ImportSaleUnit>,
    val recipe: List<ImportRecipeLine>,
)

/** One `Stok Barang` row. */
data class ImportStockItem(
    val source: String,
    val name: String,
    val unit: String,
    val priceRupiah: Long,
    val quantityText: String,
    val minAlertText: String,
)

data class WorkbookImportPreview(
    val sourceName: String,
    val sourceSha256: String,
    val transactions: List<ImportTransaction> = emptyList(),
    val budgetLines: List<ImportBudgetLine> = emptyList(),
    val products: List<ImportProduct> = emptyList(),
    val stockItems: List<ImportStockItem> = emptyList(),
    /** The plan period the file was exported with; applied to a unit that has none yet. */
    val planPeriod: Pair<YearMonth, YearMonth>? = null,
    /** Problems that make the file unusable: not a template, renamed columns, invalid cells. */
    val errors: List<String> = emptyList(),
) {
    val canCommit: Boolean get() = errors.isEmpty() &&
        (transactions.isNotEmpty() || budgetLines.isNotEmpty() || products.isNotEmpty() || stockItems.isNotEmpty())
    val rowCount: Int get() = transactions.size + budgetLines.size + products.size + stockItems.size
    val totalIncomeRupiah: Long get() = transactions.filter { it.incoming }.sumOf { it.amountRupiah }
    val totalExpenseRupiah: Long get() = transactions.filterNot { it.incoming }.sumOf { it.amountRupiah }
}
