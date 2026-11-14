package com.mankeu.exporter

import com.mankeu.importer.XlsxWorkbookReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxWorkbookWriterTest {
    @Test
    fun `writer produces readable multi-sheet xlsx with exact integer rupiah`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable("Metadata", listOf("Kunci", "Nilai"), listOf(listOf("Format", "MANKEU"))),
                WorkbookTable(
                    "Transaksi Realisasi",
                    listOf("Tanggal", "Keterangan", "Nominal"),
                    listOf(listOf("2026-08-15", "Jasa traktor & panen", 7_000_000L)),
                    currencyColumns = setOf(2),
                ),
            ),
        )

        assertTrue(bytes.copyOfRange(0, 2).contentEquals(byteArrayOf('P'.code.toByte(), 'K'.code.toByte())))
        val snapshot = XlsxWorkbookReader().read(bytes.inputStream())
        assertEquals(listOf("Metadata", "Transaksi Realisasi"), snapshot.sheets.map { it.name })
        val row = snapshot.sheets[1].rows[1]
        assertEquals("2026-08-15", row.cellAt(0)?.text)
        assertEquals("Jasa traktor & panen", row.cellAt(1)?.text)
        assertEquals(7_000_000.0, row.cellAt(2)?.number)
    }

    @Test
    fun `incomplete product costs withhold derived projection and feasibility from workbook`() {
        val product = com.mankeu.data.ProductRecord(
            com.mankeu.data.ProductEntity("p", "u", "Keripik", "bungkus", batchYieldText = "1", targetPerMonthText = "10", createdAtEpochMillis = 0),
            listOf(com.mankeu.data.ProductUnitEntity("pu", "p", "bungkus", "1", 8_000)), emptyList(),
        )
        val project = com.mankeu.data.ProjectEntity("u", "b", "Utama", true, createdAtEpochMillis = 0)
        val plan = com.mankeu.domain.PlanCalculator.analyse(com.mankeu.data.PlanAssembly.planInputs(project, listOf(product), emptyList(), emptyList(), java.time.LocalDate.of(2026, 11, 14), emptyMap()))
        val projection = FinanceExportBook(com.mankeu.data.BookEntity("book", "u", "PROJECTION", 0), emptyList(), emptyList(), plan.summary)
        val bundle = FinanceExportBundle(com.mankeu.data.BusinessEntity("b", "Usaha", createdAtEpochMillis = 0), project, emptyList(), listOf(projection), emptyList(), emptyList(), emptyList(), products = listOf(product), plan = plan)
        val workbook = XlsxWorkbookReader().read(FinanceWorkbookExporter().export(bundle).inputStream())
        for (name in listOf("Laporan", "Kelayakan")) {
            val sheet = workbook.sheets.single { it.name == name }
            val text = sheet.rows.flatMap { row -> (0..2).mapNotNull { row.cellAt(it)?.text } }
            assertTrue(text.any { it.contains("Modal barang belum lengkap") })
            assertTrue(text.none { it == "Laba / bulan" || it == "Laba atau rugi" || it == "Status kelayakan" })
        }
    }

    @Test
    fun `duplicate and invalid sheet names are normalized uniquely`() {
        val bytes = XlsxWorkbookWriter().write(
            listOf(
                WorkbookTable("A/B", listOf("x"), emptyList()),
                WorkbookTable("A:B", listOf("x"), emptyList()),
            ),
        )

        val names = XlsxWorkbookReader().read(bytes.inputStream()).sheets.map { it.name }
        assertEquals(2, names.distinct().size)
        assertTrue(names.all { it.length <= 31 && '/' !in it && ':' !in it })
    }
}
