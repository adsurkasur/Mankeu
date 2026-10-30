package com.mankeu.importer

import com.mankeu.domain.CostNature
import com.mankeu.domain.ProductMovementType
import com.mankeu.domain.Recurrence
import com.mankeu.domain.StockMovementType
import com.mankeu.domain.TransactionCalculation
import com.mankeu.domain.sha256Hex
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * Reads Mankeu's own template ([MankeuTemplate]) and nothing else (DEC-012).
 *
 * A file without the marker, or with a missing/renamed sheet or column, is rejected with the exact
 * list of problems. Cells are read literally: no guessing of columns, years, months or categories.
 * Every error names its sheet and cell so the owner can fix the file.
 */
class FinanceWorkbookImporter(
    private val reader: XlsxWorkbookReader = XlsxWorkbookReader(),
) {
    fun preview(sourceName: String, bytes: ByteArray): WorkbookImportPreview {
        val hash = bytes.sha256Hex()
        val workbook = runCatching { reader.read(bytes.inputStream()) }.getOrElse {
            return WorkbookImportPreview(sourceName, hash, errors = listOf("Berkas ini bukan file Excel (.xlsx) yang bisa dibaca."))
        }
        val sheets = workbook.sheets.associateBy { it.name.trim() }

        val structure = buildList {
            val guide = sheets[MankeuTemplate.GUIDE]
            val marked = guide?.rows?.any { row -> row.cells.any { it.display.trim() == MankeuTemplate.MARKER } } == true
            if (!marked) add("File ini bukan template Mankeu (penanda ${MankeuTemplate.MARKER} di lembar \"${MankeuTemplate.GUIDE}\" tidak ditemukan).")
            MankeuTemplate.REQUIRED.forEach { (name, headers) ->
                val sheet = sheets[name]
                if (sheet == null) {
                    add("Lembar \"$name\" tidak ada.")
                } else {
                    val actual = sheet.rows.firstOrNull { it.index == 1 }
                    headers.forEachIndexed { column, expected ->
                        val found = actual?.cellAt(column)?.display?.trim().orEmpty()
                        if (found != expected) add("Lembar \"$name\" kolom ${columnLetter(column)}1 harus \"$expected\", tertulis \"$found\".")
                    }
                }
            }
        }
        if (structure.isNotEmpty()) return WorkbookImportPreview(sourceName, hash, errors = structure)

        val errors = mutableListOf<String>()
        val period = sheets.getValue(MankeuTemplate.GUIDE).rows
            .firstOrNull { it.cellAt(0)?.display?.trim() == MankeuTemplate.PLAN_PERIOD }
            ?.let { row -> readPeriod(Row(MankeuTemplate.GUIDE, row), errors) }
        val (productMoves, materialMoves) = readMovements(sheets.getValue(MankeuTemplate.MOVEMENTS), errors)
        val stock = dataRows(sheets.getValue(MankeuTemplate.STOCK)).mapNotNull { readStock(it, errors) }
        // Material moves are imported only with their item, so undoing the import removes them too.
        val stockNames = dataRows(sheets.getValue(MankeuTemplate.STOCK)).mapNotNull { it.text(0)?.lowercase() }.toSet()
        materialMoves.filter { it.second.lowercase() !in stockNames }.forEach { (source, material, _) ->
            errors += "$source: bahan \"$material\" tidak ada di lembar \"${MankeuTemplate.STOCK}\" berkas ini; " +
                "gerakan bahan hanya diimpor bersama bahannya."
        }
        return WorkbookImportPreview(
            sourceName = sourceName,
            sourceSha256 = hash,
            transactions = dataRows(sheets.getValue(MankeuTemplate.TRANSACTIONS)).mapNotNull { readTransaction(it, errors) },
            budgetLines = dataRows(sheets.getValue(MankeuTemplate.BUDGET)).mapNotNull { readBudget(it, errors) },
            products = readProducts(
                sheets.getValue(MankeuTemplate.PRODUCTS), sheets.getValue(MankeuTemplate.SALE_UNITS),
                sheets.getValue(MankeuTemplate.RECIPES), productMoves, errors,
            ),
            stockItems = stock.map { item -> item.copy(movements = materialMoves.filter { it.second.equals(item.name.trim(), ignoreCase = true) }.map { it.third }) },
            planPeriod = period,
            errors = errors,
        )
    }

    private fun readTransaction(row: Row, errors: MutableList<String>): ImportTransaction? {
        val date = row.date(0, errors) ?: return null
        val category = row.requiredText(2, "Kategori", errors) ?: return null
        // Optional like in the entry form (DEC-022): a blank one takes the category's name.
        val description = row.text(1) ?: category
        val income = row.money(3, errors)
        val expense = row.money(4, errors)
        if ((income == null) == (expense == null)) {
            errors += "${row.ref(3)}: isi tepat satu kolom, Pemasukan atau Pengeluaran."
            return null
        }
        val quantity = row.text(6)
        if (quantity != null && TransactionCalculation.parseQuantity(quantity) == null) {
            errors += "${row.ref(6)}: jumlah harus angka lebih dari nol, tertulis \"$quantity\"."
            return null
        }
        return ImportTransaction(
            source = row.ref(0),
            date = date,
            description = description,
            categoryName = category,
            incoming = income != null,
            amountRupiah = income ?: expense!!,
            productName = row.text(5),
            quantityText = quantity,
            costNature = row.nature(7, errors),
            usefulLifeMonths = row.months(8, errors),
            unit = row.text(9),
            materialName = row.text(10),
            unitPriceRupiah = row.money(11, errors),
        )
    }

    private fun readBudget(row: Row, errors: MutableList<String>): ImportBudgetLine? {
        val monthText = row.text(0)
        val month = monthText?.let {
            runCatching { YearMonth.parse(it) }.getOrElse { errors += "${row.ref(0)}: bulan harus berformat YYYY-MM, tertulis \"$monthText\"."; return null }
        }
        val category = row.requiredText(1, "Kategori", errors) ?: return null
        val name = row.requiredText(2, "Item", errors) ?: return null
        val quantity = row.text(3)
        if (quantity != null && TransactionCalculation.parseQuantity(quantity) == null) {
            errors += "${row.ref(3)}: jumlah harus angka lebih dari nol, tertulis \"$quantity\"."
            return null
        }
        val price = row.money(5, errors)
        val total = row.money(6, errors)
            ?: quantity?.let(TransactionCalculation::parseQuantity)?.let { q -> price?.let { TransactionCalculation.computeTotalRupiah(q, it) } }
        if (total == null || total <= 0) {
            errors += "${row.ref(6)}: isi Total, atau Jumlah dan Harga Satuan."
            return null
        }
        val recurrence = when (val raw = row.text(7)) {
            null, MankeuTemplate.ONCE -> Recurrence.ONCE
            MankeuTemplate.MONTHLY -> Recurrence.MONTHLY
            else -> { errors += "${row.ref(7)}: isi \"${MankeuTemplate.MONTHLY}\" atau \"${MankeuTemplate.ONCE}\", tertulis \"$raw\"."; return null }
        }
        return ImportBudgetLine(row.ref(0), month, category, name, quantity, row.text(4), price, total, recurrence, row.nature(8, errors), row.months(9, errors))
    }

    private fun readProducts(
        products: WorkbookSheet,
        saleUnits: WorkbookSheet,
        recipes: WorkbookSheet,
        movements: List<Triple<String, String, ImportProductMovement>>,
        errors: MutableList<String>,
    ): List<ImportProduct> {
        fun Row.positiveQuantity(column: Int, label: String): String? {
            val raw = text(column) ?: return null.also { errors += "${ref(column)}: $label wajib diisi." }
            return raw.takeIf { TransactionCalculation.parseQuantity(it) != null }
                ?: null.also { errors += "${ref(column)}: $label harus angka lebih dari nol, tertulis \"$raw\"." }
        }
        val units = dataRows(saleUnits).mapNotNull { row ->
            val product = row.requiredText(0, "Produk", errors) ?: return@mapNotNull null
            val name = row.requiredText(1, "Satuan", errors) ?: return@mapNotNull null
            val content = row.positiveQuantity(2, "Isi") ?: return@mapNotNull null
            val price = row.money(3, errors) ?: return@mapNotNull null.also { errors += "${row.ref(3)}: Harga Jual wajib diisi." }
            Triple(row.ref(0), product.trim(), ImportSaleUnit(name, content, price))
        }
        val lines = dataRows(recipes).mapNotNull { row ->
            val product = row.requiredText(0, "Produk", errors) ?: return@mapNotNull null
            val name = row.requiredText(1, "Komponen", errors) ?: return@mapNotNull null
            val material = row.text(2)
            val quantity = row.text(3)
            val amount = row.money(4, errors)
            if (material != null) {
                if (quantity == null) { errors += "${row.ref(3)}: Jumlah per Batch wajib diisi untuk bahan \"$material\"."; return@mapNotNull null }
                if (TransactionCalculation.parseQuantity(quantity) == null) {
                    errors += "${row.ref(3)}: Jumlah per Batch harus angka lebih dari nol, tertulis \"$quantity\"."
                    return@mapNotNull null
                }
                if (amount != null) { errors += "${row.ref(4)}: baris berbahan diisi Jumlah, Biaya dibiarkan kosong."; return@mapNotNull null }
            } else if (amount == null) {
                errors += "${row.ref(4)}: Biaya per Batch wajib diisi untuk komponen tanpa bahan."
                return@mapNotNull null
            }
            Triple(row.ref(0), product.trim(), ImportRecipeLine(row.ref(0), name, material, quantity.takeIf { material != null }, amount))
        }
        val names = mutableSetOf<String>()
        val items = dataRows(products).mapNotNull { row ->
            val name = row.requiredText(0, "Nama", errors) ?: return@mapNotNull null
            if (!names.add(name.lowercase())) {
                errors += "${row.ref(0)}: produk \"$name\" ditulis dua kali di lembar \"${MankeuTemplate.PRODUCTS}\"."
                return@mapNotNull null
            }
            val baseUnit = row.requiredText(1, "Satuan Dasar", errors) ?: return@mapNotNull null
            val yield = row.positiveQuantity(2, "Hasil per Batch") ?: return@mapNotNull null
            val target = row.text(3)
            if (target != null && TransactionCalculation.parseQuantity(target) == null) {
                errors += "${row.ref(3)}: target harus angka lebih dari nol, tertulis \"$target\"."
                return@mapNotNull null
            }
            val sellFrom = row.text(4)?.let { raw ->
                raw.toIntOrNull()?.takeIf { it >= 1 } ?: return@mapNotNull null.also {
                    errors += "${row.ref(4)}: bulan mulai dijual harus angka bulat 1 atau lebih, tertulis \"$raw\"."
                }
            } ?: 1
            val own = units.filter { it.second.equals(name.trim(), ignoreCase = true) }.map { it.third }
            if (own.isEmpty()) {
                errors += "${row.ref(0)}: produk \"${name.trim()}\" belum punya baris di lembar \"${MankeuTemplate.SALE_UNITS}\"."
                return@mapNotNull null
            }
            ImportProduct(
                row.ref(0), name, baseUnit, yield, target, sellFrom, own,
                lines.filter { it.second.equals(name.trim(), ignoreCase = true) }.map { it.third },
                movements.filter { it.second.equals(name.trim(), ignoreCase = true) }.map { it.third },
            )
        }
        // Sale units and recipe lines belong to a product row of the same file; the export always writes all three sheets.
        // Named rows count even when invalid: their own error is reported once, not again on their units.
        val known = dataRows(products).mapNotNull { it.text(0)?.lowercase() }.toSet()
        units.filter { it.second.lowercase() !in known }.forEach { (source, product, _) ->
            errors += "$source: produk \"$product\" tidak ada di lembar \"${MankeuTemplate.PRODUCTS}\"."
        }
        lines.filter { it.second.lowercase() !in known }.forEach { (source, product, _) ->
            errors += "$source: produk \"$product\" tidak ada di lembar \"${MankeuTemplate.PRODUCTS}\"."
        }
        // Production and counts are imported only with their product, so undoing the import removes them too.
        movements.filter { it.second.lowercase() !in known }.forEach { (source, product, _) ->
            errors += "$source: produk \"$product\" tidak ada di lembar \"${MankeuTemplate.PRODUCTS}\" berkas ini; " +
                "produksi dan hitung stok hanya diimpor bersama produknya."
        }
        return items
    }

    /**
     * `Produksi & Stok` rows in sheet order: product rows paired with their product name, material rows with their
     * material name. A `Pemakaian bahan` row joins the nearest Produksi row of its product above it.
     */
    private fun readMovements(sheet: WorkbookSheet, errors: MutableList<String>): Pair<List<Triple<String, String, ImportProductMovement>>, List<Triple<String, String, ImportMaterialMovement>>> {
        val products = mutableListOf<Triple<String, String, ImportProductMovement>>()
        val materials = mutableListOf<Triple<String, String, ImportMaterialMovement>>()
        val lastProduction = mutableMapOf<String, Int>()
        dataRows(sheet).forEach { row ->
            val date = row.date(0, errors) ?: return@forEach
            val raw = row.text(2)
            val material = row.text(6)
            val product = row.text(1)
            val order = row.source.index
            when (raw) {
                MankeuTemplate.PRODUCTION, MankeuTemplate.STOCK_COUNT -> {
                    if (material != null) { errors += "${row.ref(6)}: Bahan dibiarkan kosong untuk $raw."; return@forEach }
                    val movement = readProductRow(row, date, raw, errors) ?: return@forEach
                    val name = product!!.trim()
                    if (movement.type == ProductMovementType.PRODUCTION) lastProduction[name.lowercase()] = products.size
                    products += Triple(row.ref(0), name, movement.copy(order = order))
                }
                MankeuTemplate.MATERIAL_USE -> {
                    val productName = row.requiredText(1, "Produk", errors) ?: return@forEach
                    val materialName = row.requiredText(6, "Bahan", errors) ?: return@forEach
                    val quantity = row.materialQuantity(allowZero = false, errors) ?: return@forEach
                    val index = lastProduction[productName.trim().lowercase()]
                        ?: return@forEach Unit.also { errors += "${row.ref(2)}: ${MankeuTemplate.MATERIAL_USE} harus di bawah baris ${MankeuTemplate.PRODUCTION} \"${productName.trim()}\"." }
                    val (source, name, production) = products[index]
                    products[index] = Triple(source, name, production.copy(uses = production.uses + ImportMaterialUse(row.ref(0), materialName.trim(), quantity, order)))
                }
                MankeuTemplate.MATERIAL_IN, MankeuTemplate.MATERIAL_OUT, MankeuTemplate.MATERIAL_COUNT -> {
                    if (product != null) { errors += "${row.ref(1)}: Produk dibiarkan kosong untuk $raw."; return@forEach }
                    val materialName = row.requiredText(6, "Bahan", errors) ?: return@forEach
                    val type = when (raw) {
                        MankeuTemplate.MATERIAL_IN -> StockMovementType.IN
                        MankeuTemplate.MATERIAL_OUT -> StockMovementType.OUT
                        else -> StockMovementType.ADJUSTMENT
                    }
                    val quantity = row.materialQuantity(allowZero = type == StockMovementType.ADJUSTMENT, errors) ?: return@forEach
                    materials += Triple(row.ref(0), materialName.trim(), ImportMaterialMovement(row.ref(0), date, type, quantity, row.text(5).orEmpty(), order))
                }
                else -> errors += "${row.ref(2)}: Jenis harus salah satu dari ${KINDS.joinToString { "\"$it\"" }}, tertulis \"${raw.orEmpty()}\"."
            }
        }
        return products to materials
    }

    /** A material row's Jumlah, in the material's unit; Batch stays blank. */
    private fun Row.materialQuantity(allowZero: Boolean, errors: MutableList<String>): String? {
        if (text(4) != null) { errors += "${ref(4)}: Batch hanya diisi untuk Produksi."; return null }
        val raw = text(3) ?: return null.also { errors += "${ref(3)}: Jumlah wajib diisi." }
        val value = raw.replace(',', '.').toBigDecimalOrNull()
        if (value == null || value.signum() < 0 || (!allowZero && value.signum() == 0)) {
            errors += "${ref(3)}: Jumlah ${if (allowZero) "hitung ulang harus angka nol atau lebih" else "harus angka lebih dari nol"}, tertulis \"$raw\"."
            return null
        }
        return value.stripTrailingZeros().toPlainString()
    }

    /** A Produksi or Hitung stok row of a product. */
    private fun readProductRow(row: Row, date: LocalDate, kind: String, errors: MutableList<String>): ImportProductMovement? {
        row.requiredText(1, "Produk", errors) ?: return null
        val type = if (kind == MankeuTemplate.PRODUCTION) ProductMovementType.PRODUCTION else ProductMovementType.ADJUSTMENT
        val raw = row.text(3) ?: return null.also { errors += "${row.ref(3)}: Jumlah wajib diisi." }
        val value = raw.replace(',', '.').toBigDecimalOrNull()
        val production = type == ProductMovementType.PRODUCTION
        if (value == null || value.signum() < 0 || (production && value.signum() == 0)) {
            errors += "${row.ref(3)}: Jumlah ${if (production) "produksi harus angka lebih dari nol" else "hitung stok harus angka nol atau lebih"}, tertulis \"$raw\"."
            return null
        }
        val batches = row.text(4)
        if (batches != null && !production) { errors += "${row.ref(4)}: Batch hanya diisi untuk Produksi."; return null }
        if (batches != null && TransactionCalculation.parseQuantity(batches) == null) {
            errors += "${row.ref(4)}: Batch harus angka lebih dari nol, tertulis \"$batches\"."
            return null
        }
        return ImportProductMovement(
            source = row.ref(0),
            date = date,
            type = type,
            quantityText = value.stripTrailingZeros().toPlainString(),
            batchesText = batches?.let { TransactionCalculation.parseQuantity(it)!!.stripTrailingZeros().toPlainString() },
            notes = row.text(5).orEmpty(),
        )
    }

    private fun readPeriod(row: Row, errors: MutableList<String>): Pair<YearMonth, YearMonth>? {
        val raw = row.text(1) ?: return null
        val parts = raw.split(MankeuTemplate.PERIOD_SEPARATOR.trim()).map { it.trim() }
        val months = parts.takeIf { it.size == 2 }?.mapNotNull { runCatching { YearMonth.parse(it) }.getOrNull() }
        if (months == null || months.size != 2 || months[1].isBefore(months[0])) {
            errors += "${row.ref(1)}: periode rencana harus berformat YYYY-MM s.d. YYYY-MM, tertulis \"$raw\"."
            return null
        }
        return months[0] to months[1]
    }

    private fun readStock(row: Row, errors: MutableList<String>): ImportStockItem? {
        val name = row.requiredText(0, "Nama", errors) ?: return null
        val unit = row.requiredText(1, "Satuan", errors) ?: return null
        val price = row.money(2, errors) ?: 0
        val quantity = row.quantity(3, errors) ?: return null
        val minimum = row.quantity(4, errors) ?: return null
        val archived = when (val raw = row.text(5)) {
            null -> false
            MankeuTemplate.YES -> true
            else -> return null.also { errors += "${row.ref(5)}: Diarsipkan diisi \"${MankeuTemplate.YES}\" atau dibiarkan kosong, tertulis \"$raw\"." }
        }
        return ImportStockItem(row.ref(0), name, unit, price, quantity, minimum, archived)
    }

    // ---- cell reading -----------------------------------------------------------------------

    private class Row(val sheet: String, val source: WorkbookRow) {
        fun ref(column: Int) = "$sheet!${columnLetter(column)}${source.index}"
        fun cell(column: Int) = source.cellAt(column)
        fun text(column: Int): String? = cell(column)?.let { it.text ?: it.number?.let(::plainNumber) }?.trim()?.ifBlank { null }
    }

    private fun dataRows(sheet: WorkbookSheet) = sheet.rows
        .filter { it.index > 1 && it.cells.any { cell -> cell.display.isNotBlank() } }
        .map { Row(sheet.name.trim(), it) }

    private fun Row.requiredText(column: Int, label: String, errors: MutableList<String>): String? =
        text(column) ?: null.also { errors += "${ref(column)}: $label wajib diisi." }

    /** Whole rupiah. A number cell, or text like "150000", "150.000" or "Rp 150.000". Blank is null. */
    private fun Row.money(column: Int, errors: MutableList<String>): Long? {
        val cell = cell(column) ?: return null
        cell.number?.let { number ->
            if (number < 0 || number != Math.floor(number)) {
                errors += "${ref(column)}: nominal harus bilangan bulat rupiah tanpa minus."
                return null
            }
            return number.toLong().takeIf { it > 0 }
        }
        val raw = cell.text?.trim()?.ifBlank { null } ?: return null
        val digits = raw.removePrefix("Rp").replace(".", "").replace(" ", "")
        return digits.toLongOrNull()?.takeIf { it >= 0 && digits.all(Char::isDigit) }?.takeIf { it > 0 }
            ?: null.also { if (digits != "0") errors += "${ref(column)}: \"$raw\" bukan nominal rupiah yang valid." }
    }

    private fun Row.nature(column: Int, errors: MutableList<String>): CostNature? = when (val raw = text(column)) {
        null -> null
        MankeuTemplate.NATURE_FIXED -> CostNature.FIXED
        MankeuTemplate.NATURE_VARIABLE -> CostNature.VARIABLE
        else -> null.also { errors += "${ref(column)}: Sifat Biaya harus \"${MankeuTemplate.NATURE_FIXED}\" atau \"${MankeuTemplate.NATURE_VARIABLE}\", tertulis \"$raw\"." }
    }

    private fun Row.months(column: Int, errors: MutableList<String>): Int? {
        val raw = text(column) ?: return null
        return raw.toIntOrNull()?.takeIf { it > 0 } ?: null.also { errors += "${ref(column)}: umur pakai harus jumlah bulan bulat lebih dari nol, tertulis \"$raw\"." }
    }

    /** A stock count; zero allowed, decimals with comma or dot. */
    private fun Row.quantity(column: Int, errors: MutableList<String>): String? {
        val raw = text(column) ?: return "0"
        val value = raw.replace(',', '.').toBigDecimalOrNull()
        if (value == null || value.signum() < 0) {
            errors += "${ref(column)}: \"$raw\" bukan jumlah yang valid."
            return null
        }
        return value.stripTrailingZeros().toPlainString()
    }

    /** ISO `YYYY-MM-DD` text, or an Excel date cell (serial day number). */
    private fun Row.date(column: Int, errors: MutableList<String>): LocalDate? {
        val cell = cell(column)
        cell?.number?.let { return EXCEL_EPOCH.plusDays(it.toLong()) }
        val raw = cell?.text?.trim()?.ifBlank { null } ?: return null.also { errors += "${ref(column)}: Tanggal wajib diisi." }
        return try {
            LocalDate.parse(raw)
        } catch (_: DateTimeParseException) {
            errors += "${ref(column)}: tanggal harus berformat YYYY-MM-DD, tertulis \"$raw\"."
            null
        }
    }

    private companion object {
        val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)
        val KINDS = listOf(
            MankeuTemplate.PRODUCTION, MankeuTemplate.STOCK_COUNT, MankeuTemplate.MATERIAL_USE,
            MankeuTemplate.MATERIAL_IN, MankeuTemplate.MATERIAL_OUT, MankeuTemplate.MATERIAL_COUNT,
        )

        fun columnLetter(index: Int): String = ('A' + index).toString()

        fun plainNumber(value: Double): String =
            if (value == Math.floor(value)) value.toLong().toString() else value.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}
