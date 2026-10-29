package com.mankeu.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Everyday language on screen (spec #19): accounting terms only in the PDF and "Rincian analisis". */
class ScreenLanguageTest {
    @Test
    fun `screen strings do not speak of debit, credit or journals`() {
        val accountant = Regex("""\b(debit|kredit|credit|jurnal|journal)\b""", RegexOption.IGNORE_CASE)
        val offenders = listOf("values", "values-en").flatMap { dir ->
            File("src/main/res/$dir/strings.xml").readLines().filter { accountant.containsMatchIn(it) }.map { "$dir: ${it.trim()}" }
        }
        assertEquals(emptyList<String>(), offenders)
    }
}
