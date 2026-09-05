package com.arinahub.arinaagri.calculator.exporter

import com.arinahub.arinaagri.calculator.data.BookEntity
import com.arinahub.arinaagri.calculator.data.BudgetItemEntity
import com.arinahub.arinaagri.calculator.data.BusinessEntity
import com.arinahub.arinaagri.calculator.data.CategoryEntity
import com.arinahub.arinaagri.calculator.data.FinancialEventEntity
import com.arinahub.arinaagri.calculator.data.ImportBatchEntity
import com.arinahub.arinaagri.calculator.data.ImportNoticeEntity
import com.arinahub.arinaagri.calculator.data.ObligationBalanceRow
import com.arinahub.arinaagri.calculator.data.ProjectEntity
import com.arinahub.arinaagri.calculator.data.ProvenanceEntity
import com.arinahub.arinaagri.calculator.data.ScenarioAssumptionEntity
import com.arinahub.arinaagri.calculator.domain.BookMode
import com.arinahub.arinaagri.calculator.domain.FinancialSummary
import java.time.Instant
import java.time.LocalDate

data class FinanceExportBook(
    val book: BookEntity,
    val budgets: List<BudgetItemEntity>,
    val events: List<FinancialEventEntity>,
    val obligations: List<ObligationBalanceRow>,
    val summary: FinancialSummary,
    val assumptions: ScenarioAssumptionEntity?,
)

data class FinanceExportBundle(
    val business: BusinessEntity,
    val project: ProjectEntity,
    val categories: List<CategoryEntity>,
    val books: List<FinanceExportBook>,
    val importBatches: List<ImportBatchEntity>,
    val notices: List<ImportNoticeEntity>,
    val provenance: List<ProvenanceEntity>,
    val generatedAtEpochMillis: Long = System.currentTimeMillis(),
)

class FinanceWorkbookExporter(
    private val writer: XlsxWorkbookWriter = XlsxWorkbookWriter(),
) {
    fun export(bundle: FinanceExportBundle): ByteArray {
        val categories = bundle.categories.associateBy { it.id }
        val booksByMode = bundle.books.associateBy { BookMode.valueOf(it.book.mode) }
        val tables = buildList {
            add(metadata(bundle))
            BookMode.entries.forEach { mode ->
                booksByMode[mode]?.let { book ->
                    add(budgetTable(mode, book, categories))
                    add(transactionTable(mode, book))
                }
            }
            add(obligationTable(bundle.books))
            add(reportTable(bundle.books))
            add(comparisonTable(booksByMode))
            add(assumptionTable(bundle.books))
            add(importAuditTable(bundle))
        }
        return writer.write(tables)
    }

    private fun metadata(bundle: FinanceExportBundle) = WorkbookTable(
        name = "Metadata",
        headers = listOf("Kunci", "Nilai"),
        rows = listOf(
            listOf("Format", "MANKEU_FINANCE_XLSX"),
            listOf("Versi format", 1),
            listOf("Dibuat oleh", "Mankeu"),
            listOf("Dibuat pada", Instant.ofEpochMilli(bundle.generatedAtEpochMillis).toString()),
            listOf("ID usaha", bundle.business.id),
            listOf("Nama usaha", bundle.business.name),
            listOf("ID proyek", bundle.project.id),
            listOf("Nama proyek", bundle.project.name),
            listOf("Mata uang", bundle.business.currency),
            listOf("Catatan", "Transaksi, laporan, asumsi, dan jejak impor diekspor sebagai nilai yang dapat diaudit. Backup penuh menggunakan berkas .aabk terenkripsi."),
        ),
    )

    private fun budgetTable(
        mode: BookMode,
        book: FinanceExportBook,
        categories: Map<String, CategoryEntity>,
    ) = WorkbookTable(
        name = "Anggaran ${mode.label()}",
        headers = listOf(
            "ID", "Kategori", "Item", "Jumlah", "Satuan", "Harga Satuan", "Total Rencana",
            "Tanggal", "Sumber Nilai", "Selisih Formula", "Status", "Import Batch",
        ),
        rows = book.budgets.map { item ->
            listOf(
                item.id,
                categories[item.categoryId]?.name ?: item.categoryId,
                item.name,
                item.quantityText,
                item.unit,
                item.unitPriceRupiah,
                item.plannedTotalRupiah,
                item.plannedDateEpochDay?.let(LocalDate::ofEpochDay)?.toString(),
                item.amountSource,
                item.formulaDifferenceRupiah,
                if (item.archived) "DIARSIPKAN" else "AKTIF",
                item.importBatchId,
            )
        },
        currencyColumns = setOf(5, 6, 9),
    )

    private fun transactionTable(mode: BookMode, book: FinanceExportBook) = WorkbookTable(
        name = "Transaksi ${mode.label()}",
        headers = listOf(
            "ID", "Tanggal", "Jenis", "Keterangan", "Kategori", "Nominal", "Arus Kas", "Pihak",
            "Jatuh Tempo", "Referensi", "Jumlah", "Satuan", "Harga Satuan", "ID Anggaran",
            "ID Kewajiban", "Status", "Membalik ID", "Dibalik Oleh ID", "Import Batch",
        ),
        rows = book.events.map { event ->
            listOf(
                event.id,
                LocalDate.ofEpochDay(event.dateEpochDay).toString(),
                event.type,
                event.description,
                event.category,
                event.amountRupiah,
                event.cashFlowRole,
                event.counterparty,
                event.dueDateEpochDay?.let(LocalDate::ofEpochDay)?.toString(),
                event.reference,
                event.quantityText,
                event.unit,
                event.unitPriceRupiah,
                event.budgetItemId,
                event.obligationId,
                event.status,
                event.reversesEventId,
                event.reversedByEventId,
                event.importBatchId,
            )
        },
        currencyColumns = setOf(5, 12),
    )

    private fun obligationTable(books: List<FinanceExportBook>) = WorkbookTable(
        name = "Piutang dan Utang",
        headers = listOf(
            "Buku", "Jenis", "Pihak", "Keterangan", "Tanggal", "Jatuh Tempo", "Nilai Awal",
            "Sudah Dibayar", "Sisa", "Status", "ID Sumber",
        ),
        rows = books.flatMap { book ->
            book.obligations.map { row ->
                val outstanding = (row.originalAmountRupiah - row.settledAmountRupiah).coerceAtLeast(0)
                listOf(
                    BookMode.valueOf(book.book.mode).label(),
                    row.kind,
                    row.counterparty,
                    row.description,
                    LocalDate.ofEpochDay(row.issueDateEpochDay).toString(),
                    row.dueDateEpochDay?.let(LocalDate::ofEpochDay)?.toString(),
                    row.originalAmountRupiah,
                    row.settledAmountRupiah,
                    outstanding,
                    when {
                        row.reversed -> "DIBALIK"
                        outstanding == 0L -> "LUNAS"
                        row.settledAmountRupiah > 0 -> "SEBAGIAN"
                        else -> "BELUM DIBAYAR"
                    },
                    row.sourceEventId,
                )
            }
        },
        currencyColumns = setOf(6, 7, 8),
    )

    private fun reportTable(books: List<FinanceExportBook>) = WorkbookTable(
        name = "Laporan",
        headers = listOf("Buku", "Metrik", "Nilai"),
        rows = books.flatMap { book ->
            val mode = BookMode.valueOf(book.book.mode).label()
            with(book.summary) {
                listOf(
                    listOf(mode, "Pendapatan", operatingIncomeRupiah),
                    listOf(mode, "Beban", operatingExpenseRupiah),
                    listOf(mode, "Laba atau rugi", profitRupiah),
                    listOf(mode, "Arus kas operasi", operatingCashFlowRupiah),
                    listOf(mode, "Arus kas investasi", investingCashFlowRupiah),
                    listOf(mode, "Arus kas pendanaan", financingCashFlowRupiah),
                    listOf(mode, "Saldo kas", cashBalanceRupiah),
                    listOf(mode, "Piutang tersisa", receivableOutstandingRupiah),
                    listOf(mode, "Utang tersisa", payableOutstandingRupiah),
                )
            }
        },
        currencyColumns = setOf(2),
    )

    private fun comparisonTable(books: Map<BookMode, FinanceExportBook>): WorkbookTable {
        val projection = books[BookMode.PROJECTION]
        val realization = books[BookMode.REALIZATION]
        fun metric(selector: (FinancialSummary) -> Long): List<Any?> {
            val planned = projection?.summary?.let(selector) ?: 0
            val actual = realization?.summary?.let(selector) ?: 0
            return listOf(planned, actual, actual - planned)
        }
        val plannedBudget = projection?.budgets?.filterNot { it.archived }?.sumOf { it.plannedTotalRupiah } ?: 0
        val actualBudget = realization?.budgets?.filterNot { it.archived }?.sumOf { it.plannedTotalRupiah } ?: 0
        return WorkbookTable(
            name = "Perbandingan",
            headers = listOf("Metrik", "Proyeksi", "Realisasi", "Selisih"),
            rows = listOf(
                listOf("Total anggaran", plannedBudget, actualBudget, actualBudget - plannedBudget),
                listOf("Pendapatan") + metric { it.operatingIncomeRupiah },
                listOf("Beban") + metric { it.operatingExpenseRupiah },
                listOf("Laba atau rugi") + metric { it.profitRupiah },
                listOf("Arus kas operasi") + metric { it.operatingCashFlowRupiah },
                listOf("Saldo kas") + metric { it.cashBalanceRupiah },
                listOf("Piutang") + metric { it.receivableOutstandingRupiah },
                listOf("Utang") + metric { it.payableOutstandingRupiah },
            ),
            currencyColumns = setOf(1, 2, 3),
        )
    }

    private fun assumptionTable(books: List<FinanceExportBook>) = WorkbookTable(
        name = "Asumsi",
        headers = listOf("Buku", "Saldo Awal", "Modal Sendiri", "Pinjaman", "Bunga Basis Poin", "Biaya Pembiayaan", "Jumlah Output", "Satuan Output", "Harga Output"),
        rows = books.map { book ->
            val value = book.assumptions
            listOf(
                BookMode.valueOf(book.book.mode).label(),
                value?.openingCashRupiah ?: 0,
                value?.ownerCapitalRupiah ?: 0,
                value?.loanAmountRupiah ?: 0,
                value?.interestRateBasisPoints ?: 0,
                value?.otherFinancingCostRupiah ?: 0,
                value?.outputQuantityText,
                value?.outputUnit,
                value?.outputPriceRupiah,
            )
        },
        currencyColumns = setOf(1, 2, 3, 5, 8),
    )

    private fun importAuditTable(bundle: FinanceExportBundle): WorkbookTable {
        val batchRows = bundle.importBatches.map { batch ->
            listOf(
                "BATCH", batch.id, batch.sourceName, batch.state, batch.recordCount, batch.totalAmountRupiah,
                batch.mappingVersion, batch.sourceSha256, null, null,
            )
        }
        val noticeRows = bundle.notices.map { notice ->
            listOf(
                "NOTIFIKASI", notice.batchId, notice.title, notice.severity, null, notice.amountRupiah,
                notice.action, notice.sourceEvidence, notice.explanation, if (notice.reviewed) "DITINJAU" else "BELUM DITINJAU",
            )
        }
        val provenanceRows = bundle.provenance.map { record ->
            listOf(
                "PROVENANCE", record.batchId, record.targetEventId ?: record.targetBudgetItemId, record.classification,
                record.confidenceBasisPoints, null, record.ruleVersion, record.sourceEvidence, record.rationale, record.inferredFields,
            )
        }
        return WorkbookTable(
            name = "Jejak Impor",
            headers = listOf("Jenis", "Batch", "Target atau Sumber", "Status atau Klasifikasi", "Jumlah atau Keyakinan", "Nominal", "Aturan atau Aksi", "Bukti", "Penjelasan", "Atribut"),
            rows = batchRows + noticeRows + provenanceRows,
            currencyColumns = setOf(5),
        )
    }

    private fun BookMode.label(): String = when (this) {
        BookMode.PROJECTION -> "Proyeksi"
        BookMode.REALIZATION -> "Realisasi"
    }
}
