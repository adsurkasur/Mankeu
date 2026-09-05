package com.arinahub.arinaagri.calculator.exporter

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class WorkbookTable(
    val name: String,
    val headers: List<String>,
    val rows: List<List<Any?>>,
    val currencyColumns: Set<Int> = emptySet(),
)

/** Small dependency-free OOXML writer for auditable, ordinary `.xlsx` workbooks. */
class XlsxWorkbookWriter {
    fun write(tables: List<WorkbookTable>): ByteArray {
        require(tables.isNotEmpty()) { "Workbook harus memiliki setidaknya satu lembar." }
        val normalized = uniqueSheetNames(tables)
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.put("[Content_Types].xml", contentTypes(normalized.size))
                zip.put("_rels/.rels", rootRelationships)
                zip.put("docProps/app.xml", appProperties(normalized.map { it.name }))
                zip.put("docProps/core.xml", coreProperties)
                zip.put("xl/workbook.xml", workbook(normalized.map { it.name }))
                zip.put("xl/_rels/workbook.xml.rels", workbookRelationships(normalized.size))
                zip.put("xl/styles.xml", styles)
                normalized.forEachIndexed { index, table ->
                    zip.put("xl/worksheets/sheet${index + 1}.xml", worksheet(table))
                }
            }
            output.toByteArray()
        }
    }

    private fun uniqueSheetNames(tables: List<WorkbookTable>): List<WorkbookTable> {
        val used = mutableSetOf<String>()
        return tables.mapIndexed { index, table ->
            val base = table.name.map { if (it in "\\/?*[]:") ' ' else it }.joinToString("")
                .trim().ifBlank { "Sheet ${index + 1}" }.take(31)
            var candidate = base
            var suffix = 2
            while (!used.add(candidate.lowercase())) {
                val marker = " ($suffix)"
                candidate = base.take(31 - marker.length) + marker
                suffix += 1
            }
            table.copy(name = candidate)
        }
    }

    private fun worksheet(table: WorkbookTable): String {
        val allRows = listOf(table.headers) + table.rows
        val lastColumn = columnName(table.headers.size.coerceAtLeast(1))
        val lastRow = allRows.size.coerceAtLeast(1)
        val rowsXml = allRows.mapIndexed { rowIndex, row ->
            val cells = table.headers.indices.joinToString("") { columnIndex ->
                val value = row.getOrNull(columnIndex)
                cell(
                    reference = "${columnName(columnIndex + 1)}${rowIndex + 1}",
                    value = value,
                    style = when {
                        rowIndex == 0 -> 1
                        columnIndex in table.currencyColumns && value is Number -> 2
                        else -> 0
                    },
                )
            }
            "<row r=\"${rowIndex + 1}\">$cells</row>"
        }.joinToString("")
        val widths = table.headers.indices.joinToString("") { index ->
            val sampleWidth = allRows.asSequence().mapNotNull { it.getOrNull(index)?.toString()?.length }.maxOrNull() ?: 10
            val width = sampleWidth.coerceIn(10, 42) + 2
            "<col min=\"${index + 1}\" max=\"${index + 1}\" width=\"$width\" customWidth=\"1\"/>"
        }
        val filter = if (table.headers.isEmpty()) "" else "<autoFilter ref=\"A1:${lastColumn}${lastRow}\"/>"
        return xmlHeader + """
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <dimension ref="A1:${lastColumn}${lastRow}"/>
              <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
              <cols>$widths</cols>
              <sheetData>$rowsXml</sheetData>
              $filter
            </worksheet>
        """.trimIndent()
    }

    private fun cell(reference: String, value: Any?, style: Int): String {
        val styleAttribute = if (style == 0) "" else " s=\"$style\""
        return when (value) {
            null -> "<c r=\"$reference\"$styleAttribute/>"
            is Byte, is Short, is Int, is Long, is Float, is Double ->
                "<c r=\"$reference\"$styleAttribute t=\"n\"><v>$value</v></c>"
            is Boolean -> "<c r=\"$reference\"$styleAttribute t=\"b\"><v>${if (value) 1 else 0}</v></c>"
            else -> "<c r=\"$reference\"$styleAttribute t=\"inlineStr\"><is><t xml:space=\"preserve\">${escape(value.toString())}</t></is></c>"
        }
    }

    private fun workbook(names: List<String>): String = xmlHeader + """
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <bookViews><workbookView/></bookViews>
          <sheets>${names.mapIndexed { index, name -> "<sheet name=\"${escape(name)}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>" }.joinToString("")}</sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelationships(count: Int): String = xmlHeader + """
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          ${(1..count).joinToString("") { "<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>" }}
          <Relationship Id="rId${count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun contentTypes(count: Int): String = xmlHeader + """
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
          ${(1..count).joinToString("") { "<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" }}
          <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
          <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
        </Types>
    """.trimIndent()

    private fun appProperties(names: List<String>): String = xmlHeader + """
        <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
          <Application>Mankeu</Application>
          <TitlesOfParts><vt:vector size="${names.size}" baseType="lpstr">${names.joinToString("") { "<vt:lpstr>${escape(it)}</vt:lpstr>" }}</vt:vector></TitlesOfParts>
        </Properties>
    """.trimIndent()

    private fun columnName(oneBased: Int): String {
        var value = oneBased
        val result = StringBuilder()
        while (value > 0) {
            value -= 1
            result.append(('A'.code + value % 26).toChar())
            value /= 26
        }
        return result.reverse().toString()
    }

    private fun escape(value: String): String = buildString {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (character == '\t' || character == '\n' || character == '\r' || character.code >= 0x20) append(character)
            }
        }
    }

    private fun ZipOutputStream.put(path: String, contents: String) {
        putNextEntry(ZipEntry(path))
        write(contents.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private companion object {
        const val xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        val rootRelationships = xmlHeader + """
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
              <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
            </Relationships>
        """.trimIndent()
        val coreProperties = xmlHeader + """
            <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <dc:creator>Mankeu</dc:creator><cp:lastModifiedBy>Mankeu</cp:lastModifiedBy>
            </cp:coreProperties>
        """.trimIndent()
        val styles = xmlHeader + """
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <numFmts count="1"><numFmt numFmtId="164" formatCode="[${'$'}Rp-421] #,##0"/></numFmts>
              <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><color rgb="FFFFFFFF"/><sz val="11"/><name val="Calibri"/></font></fonts>
              <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF0061A4"/><bgColor indexed="64"/></patternFill></fill></fills>
              <borders count="1"><border/></borders>
              <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
              <cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="1" borderId="0" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs>
              <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
            </styleSheet>
        """.trimIndent()
    }
}
