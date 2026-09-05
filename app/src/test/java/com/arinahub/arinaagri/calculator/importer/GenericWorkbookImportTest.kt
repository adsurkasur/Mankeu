package com.arinahub.arinaagri.calculator.importer

import com.arinahub.arinaagri.calculator.domain.FinancialEventType
import com.arinahub.arinaagri.calculator.exporter.WorkbookTable
import com.arinahub.arinaagri.calculator.exporter.XlsxWorkbookWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericWorkbookImportTest {
    @Test
    fun `canonical Arina export preserves active credit and settlement linkage`() {
        val headers = listOf(
            "ID", "Tanggal", "Jenis", "Keterangan", "Kategori", "Nominal", "Arus Kas", "Pihak",
            "Jatuh Tempo", "Referensi", "Jumlah", "Satuan", "Harga Satuan", "ID Anggaran",
            "ID Kewajiban", "Status", "Membalik ID", "Dibalik Oleh ID", "Import Batch",
        )
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable("Metadata", listOf("Kunci", "Nilai"), listOf(listOf("Format", "ARINA_AGRI_FINANCE_XLSX"))),
                WorkbookTable(
                    "Transaksi Realisasi",
                    headers,
                    listOf(
                        row(headers, "sale", "2026-08-01", "CREDIT_SALE", "Jasa panen", 500_000, "Pak Budi", "obligation-1", "POSTED"),
                        row(headers, "pay", "2026-08-05", "RECEIVABLE_COLLECTION", "Terima sebagian", 200_000, "Pak Budi", "obligation-1", "POSTED"),
                        row(headers, "old", "2026-08-06", "CASH_EXPENSE", "Catatan dibalik", 50_000, null, null, "REVERSED", reversedBy = "reversal"),
                        row(headers, "reversal", "2026-08-06", "REVERSAL", "Pembalikan", 50_000, null, null, "POSTED"),
                    ),
                ),
            ),
        )

        val preview = FinanceWorkbookImporter().preview("arina-export.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(2, preview.candidates.size)
        assertEquals(listOf(FinancialEventType.CREDIT_SALE, FinancialEventType.RECEIVABLE_COLLECTION), preview.candidates.map { it.type })
        assertEquals(listOf("obligation-1", "obligation-1"), preview.candidates.map { it.sourceObligationId })
        assertTrue(preview.warnings.any { "pembalikan" in it })
    }

    @Test
    fun `single daily table imports without requiring the five-view baseline`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable(
                    "Catatan Bengkel",
                    listOf("Tanggal", "Uraian", "Pengeluaran", "Pemasukan"),
                    listOf(
                        listOf("2026-08-10", "Servis traktor", null, 800_000),
                        listOf("2026-08-11", "Beli oli", 120_000, null),
                    ),
                ),
            ),
        )

        val preview = FinanceWorkbookImporter().preview("bengkel.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(2, preview.candidates.size)
        assertEquals(800_000, preview.totalIncomeRupiah)
        assertEquals(120_000, preview.totalExpenseRupiah)
        assertTrue(preview.warnings.single().contains("tabel tunggal"))
    }

    @Test
    fun `canonical Mankeu export preserves active credit and settlement linkage`() {
        val headers = listOf(
            "ID", "Tanggal", "Jenis", "Keterangan", "Kategori", "Nominal", "Arus Kas", "Pihak",
            "Jatuh Tempo", "Referensi", "Jumlah", "Satuan", "Harga Satuan", "ID Anggaran",
            "ID Kewajiban", "Status", "Membalik ID", "Dibalik Oleh ID", "Import Batch",
        )
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable("Metadata", listOf("Kunci", "Nilai"), listOf(listOf("Format", "MANKEU_FINANCE_XLSX"))),
                WorkbookTable(
                    "Transaksi Realisasi",
                    headers,
                    listOf(
                        row(headers, "sale", "2026-08-01", "CREDIT_SALE", "Penjualan produk UMKM", 500_000, "Pak Budi", "obligation-1", "POSTED"),
                    ),
                ),
            ),
        )

        val preview = FinanceWorkbookImporter().preview("mankeu-export.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(1, preview.candidates.size)
        assertEquals(listOf(FinancialEventType.CREDIT_SALE), preview.candidates.map { it.type })
        assertEquals(500_000, preview.totalIncomeRupiah)
    }

    @Test
    fun `four-sheet website style workbook imports without requiring post financing`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable(
                    "1. RAB UMKM",
                    listOf("No", "Uraian", "Volume", "Satuan", "Harga Satuan (Rp)", "Total Rencana (Rp)"),
                    listOf(
                        listOf("A", "BIAYA OPERASIONAL", null, null, null, null),
                        listOf("1", "Sewa Kios", 1, "bulan", 1_500_000, 1_500_000),
                        listOf("2", "Bahan Baku Produk", 10, "kg", 50_000, 500_000),
                        listOf("TOTAL", "TOTAL BIAYA", null, null, null, 2_000_000),
                    ),
                ),
                WorkbookTable(
                    "2. Catatan Transaksi Harian",
                    listOf("Tanggal", "Uraian Transaksi", "Volume", "Satuan", "Harga Satuan", "Pengeluaran (Rp)", "Pemasukan (Rp)"),
                    listOf(
                        listOf("2026-08-01", "Bahan Baku Produk", 10, "kg", 50_000, 500_000, null),
                        listOf("2026-08-05", "Penjualan Produk Toko", 5, "pcs", 150_000, null, 750_000),
                    ),
                ),
                WorkbookTable(
                    "3. Laporan Laba Rugi",
                    listOf("Uraian", "Nilai"),
                    listOf(
                        listOf("Pendapatan", 750_000),
                        listOf("Beban", 2_000_000),
                        listOf("Sewa Kios", 1_500_000),
                    ),
                ),
                WorkbookTable(
                    "4. Arus Kas",
                    listOf("Uraian", "Agustus"),
                    listOf(
                        listOf("Kas Masuk", 750_000),
                        listOf("Kas Keluar", 500_000),
                        listOf("Sewa Kios", 1_500_000),
                    ),
                ),
            ),
        )

        val preview = FinanceWorkbookImporter().preview("umkm-website.xlsx", bytes)

        assertTrue(preview.errors.joinToString(), preview.canCommit)
        assertEquals(4, preview.recognizedSheets.size)
        assertTrue("RAB items should be extracted into budgetCandidates", preview.budgetCandidates.isNotEmpty())
        assertEquals(2, preview.budgetCandidates.size)
        assertEquals(listOf("Sewa Kios", "Bahan Baku Produk"), preview.budgetCandidates.map { it.name })
        assertEquals(listOf(1_500_000L, 500_000L), preview.budgetCandidates.map { it.plannedTotalRupiah })
        assertEquals(750_000L, preview.totalIncomeRupiah)
    }

    private fun row(
        headers: List<String>,
        id: String,
        date: String,
        type: String,
        description: String,
        amount: Long,
        counterparty: String?,
        obligationId: String?,
        status: String,
        reversedBy: String? = null,
    ): List<Any?> = headers.map { header ->
        when (header) {
            "ID" -> id
            "Tanggal" -> date
            "Jenis" -> type
            "Keterangan" -> description
            "Kategori" -> "Jasa"
            "Nominal" -> amount
            "Arus Kas" -> "OPERATING"
            "Pihak" -> counterparty
            "Jatuh Tempo" -> "2026-08-31"
            "ID Kewajiban" -> obligationId
            "Status" -> status
            "Dibalik Oleh ID" -> reversedBy
            else -> null
        }
    }
}
