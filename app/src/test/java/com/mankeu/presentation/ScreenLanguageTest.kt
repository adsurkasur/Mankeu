package com.mankeu.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/** Lampiran C §1.1 / PK-0.6: existing jargon may shrink; new screen jargon must fail. */
class ScreenLanguageTest {
    @Test
    fun `layar tidak memuat kata terlarang di luar daftar dasar`() {
        val baselineLines = File("src/test/resources/kata_terlarang_dasar.txt").readLines()
            .filter { it.isNotBlank() }
        assertEquals("Daftar dasar tidak boleh berisi baris ganda", baselineLines.distinct(), baselineLines)
        assertTrue("Format daftar dasar harus nama_string|kata", baselineLines.all { line ->
            val parts = line.split('|')
            parts.size == 2 && parts[0].matches(Regex("[a-z][a-z0-9_]*")) && parts[1] in bannedWords
        })
        val strings = listOf("values", "values-en").flatMap { directory ->
            readStrings(File("src/main/res/$directory/strings.xml").readText())
        }
        val problems = baselineProblems(violations(strings), baselineLines.toSet())
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun `kata utuh tidak membedakan kapital dan frasa lintas baris`() {
        assertEquals(
            setOf("example|hpp", "example|unit usaha", "example|r/c", "example|break-even"),
            violations(listOf(ScreenString("example", "HPP, UNIT\nUSAHA; R/C; BREAK-EVEN."))),
        )
        assertEquals(emptySet<String>(), violations(listOf(ScreenString(
            "example", "bukunya nominalisasi asetnya receivables debiting épiutang piutangé",
        ))))
    }

    @Test
    fun `semua kata Indonesia dan Inggris diperiksa`() {
        bannedWords.forEach { word ->
            assertEquals(setOf("example|$word"), violations(listOf(ScreenString("example", word.uppercase()))))
        }
    }

    @Test
    fun `hpp hanya boleh dalam kurung setelah modal`() {
        assertEquals(emptySet<String>(), violations(listOf(ScreenString("example", "Modal barang (HPP)"))))
        assertEquals(emptySet<String>(), violations(listOf(ScreenString("example", "MODAL per bungkus (hpp)"))))
        listOf("(HPP) modal", "Harga barang (HPP)", "Modal barang (HPP), HPP terjual", "Modal barang. (HPP)", "Modal HPP (HPP)").forEach { text ->
            assertEquals(setOf("example|hpp"), violations(listOf(ScreenString("example", text))))
        }
        assertEquals(setOf("example|buku"), violations(listOf(ScreenString("example", "buku resep"))))
    }

    @Test
    fun `pengecualian hanya awalan string laporan formal yang disebutkan`() {
        formalStringPrefixes.forEach { prefix ->
            assertEquals(emptySet<String>(), violations(listOf(ScreenString("${prefix}example", "piutang HPP"))))
        }
        listOf("reports_empty_body", "reports_subtitle", "plan_subtitle", "feasibility_title", "home_example").forEach { name ->
            assertEquals(setOf("$name|piutang"), violations(listOf(ScreenString(name, "piutang"))))
        }
    }

    @Test
    fun `xml diperiksa nilainya termasuk entitas dan markup tanpa komentar atau nama`() {
        assertEquals(setOf("example|piutang", "example|arus kas"), violations(readStrings("""
            <resources>
                <!-- piutang -->
                <string name="piutang">Tidak ada tagihan</string>
                <string name="example">Piu<b>tang</b> &amp; arus&#32;kas</string>
            </resources>
        """.trimIndent())))
    }

    @Test
    fun `daftar dasar menolak pelanggaran baru dan memaksa penghapusan baris usang`() {
        assertEquals(listOf("Pelanggaran baru (ganti kata di layar):", "new_label|piutang"), baselineProblems(
            setOf("new_label|piutang"), emptySet(),
        ))
        assertEquals(listOf("Hapus baris daftar dasar yang sudah tidak melanggar:", "old_label|hpp"), baselineProblems(
            emptySet(), setOf("old_label|hpp"),
        ))
        assertEquals(emptyList<String>(), baselineProblems(setOf("old_label|hpp"), setOf("old_label|hpp")))
    }

    @Test
    fun `variabel hanya diperbolehkan pada dua label kategori yang dipilih pemilik`() {
        val found = violations(listOf(
            ScreenString("category_cost_nature_hint", "Tetap dan variabel"),
            ScreenString("category_cost_nature_variable_short", "Variabel"),
            ScreenString("other_label", "Variabel"),
        ))
        assertEquals(setOf("other_label|variabel"), found)
    }

    private data class ScreenString(val name: String, val text: String)

    private fun readStrings(xml: String): List<ScreenString> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val nodes = factory.newDocumentBuilder().parse(InputSource(StringReader(xml))).getElementsByTagName("string")
        return (0 until nodes.length).map { index ->
            val node = nodes.item(index)
            ScreenString(node.attributes.getNamedItem("name").nodeValue, node.textContent)
        }
    }

    private fun violations(strings: List<ScreenString>): Set<String> = strings.flatMap { string ->
        if (formalStringPrefixes.any(string.name::startsWith)) emptyList() else {
            val text = string.text.replace("\\n", " ").replace("\\t", " ").replace(Regex("\\s+"), " ")
            bannedWords.filter { word ->
                val checkedText = if (word == "hpp") modalHpp.replace(text) { it.value.substringBeforeLast('(') } else text
                wholeWord(word).containsMatchIn(checkedText)
            }.map { word -> "${string.name}|$word" }.filterNot { it in ownerApprovedWords }
        }
    }.toSortedSet()

    private fun baselineProblems(actual: Set<String>, baseline: Set<String>): List<String> = buildList {
        val added = (actual - baseline).sorted()
        val stale = (baseline - actual).sorted()
        if (added.isNotEmpty()) {
            add("Pelanggaran baru (ganti kata di layar):")
            addAll(added)
        }
        if (stale.isNotEmpty()) {
            add("Hapus baris daftar dasar yang sudah tidak melanggar:")
            addAll(stale)
        }
    }

    private fun wholeWord(word: String) = Regex("(?<![\\p{L}\\p{N}_])${Regex.escape(word)}(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE)

    companion object {
        // Owner explicitly chose “Variabel” for the two cost-category labels (DEC-042).
        private val ownerApprovedWords = setOf("category_cost_nature_hint|variabel", "category_cost_nature_variable_short|variabel")
        // Exact Indonesian and English lists: Lampiran C §1.1 (2026-11-12), canonical lowercase.
        private val bannedWords = setOf(
            "hpp", "batch", "unit usaha", "buku", "rasio", "persediaan", "piutang", "kategori pembukuan",
            "realisasi", "proyeksi", "nominal", "opname", "prive", "operasional", "konsinyasi", "arus kas",
            "aset", "variabel", "workbook", "impor", "ekspor", "debit", "kredit", "jurnal", "r/c", "b/c", "bep", "rab", "anggaran",
            "cogs", "business unit", "ledger", "ratio", "inventory", "receivable", "payable", "projection", "realization",
            "drawing", "consignment", "cash flow", "asset", "variable cost", "credit", "journal", "break-even",
        )

        // Existing strings.xml families traced to ReportsScreen's FinancialsSection/FeasibilitySection
        // and PlanCards.PlanBreakdownCard (formal analysis), 2026-11-12. No blanket reports_, plan_,
        // feasibility_, help_ or general UI exemption. PDF currently has its own literal labels.
        private val formalStringPrefixes = setOf(
            "reports_operating_", "reports_income_note", "reports_cash_flow", "reports_cf_",
            "reports_monthly_", "reports_financing_", "reports_working_capital", "reports_rc_ratio", "reports_bc_ratio",
            "plan_bep_base", "plan_variable_total", "plan_vc_unit", "feasibility_bep_",
        )

        // Only HPP in parentheses immediately after a modal noun phrase, within the same clause.
        private val modalHpp = Regex("(?<![\\p{L}\\p{N}_])modal(?: [\\p{L}\\p{N}]+)* \\(hpp\\)", RegexOption.IGNORE_CASE)
    }
}
