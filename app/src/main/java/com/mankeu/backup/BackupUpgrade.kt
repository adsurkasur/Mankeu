package com.mankeu.backup

import com.mankeu.data.LegacyProductShape

/**
 * Brings a snapshot from an older schema to the current one, the way the Room migrations bring a
 * database (spec #1 carries schema 12 forward). Columns a newer schema dropped are ignored by the
 * restore and new columns take their SQL default, so only reshaped tables need work here.
 */
object BackupUpgrade {
    fun toCurrent(snapshot: PortableBackup): PortableBackup =
        if (snapshot.schemaVersion == 12) from12(snapshot) else snapshot

    /** Mirrors `Migration12To13`: price → first sale unit, components → per-unit recipe lines. */
    private fun from12(snapshot: PortableBackup): PortableBackup {
        val byName = snapshot.tables.associateBy { it.name }
        val products = byName.getValue("products")
        val components = byName.getValue("product_cost_components")
        fun BackupTable.text(row: List<BackupValue>, column: String) = (row[columns.indexOf(column)] as? BackupValue.Text)?.value
        fun BackupTable.long(row: List<BackupValue>, column: String) = (row[columns.indexOf(column)] as BackupValue.Integer).value

        val units = BackupTable(
            name = "product_units",
            columns = listOf("id", "productId", "name", "contentText", "priceRupiah", "sortOrder"),
            rows = products.rows.map { row ->
                val unit = LegacyProductShape.saleUnit(products.text(row, "id")!!, products.text(row, "unit")!!, products.long(row, "priceRupiah"))
                listOf(BackupValue.Text(unit.id), BackupValue.Text(unit.productId), BackupValue.Text(unit.name), BackupValue.Text(unit.contentText), BackupValue.Integer(unit.priceRupiah), BackupValue.Integer(0))
            },
        )
        val lines = BackupTable(
            name = "product_recipe_lines",
            columns = listOf("id", "productId", "name", "stockItemId", "quantityText", "amountRupiah", "sortOrder"),
            rows = components.rows.map { row ->
                val line = LegacyProductShape.recipeLine(
                    components.text(row, "id")!!, components.text(row, "productId")!!, components.text(row, "name")!!,
                    components.long(row, "amountRupiah"), components.text(row, "yieldText"), components.long(row, "sortOrder").toInt(),
                )
                listOf(BackupValue.Text(line.id), BackupValue.Text(line.productId), BackupValue.Text(line.name), BackupValue.Null, BackupValue.Null, BackupValue.Integer(line.amountRupiah!!), BackupValue.Integer(line.sortOrder.toLong()))
            },
        )
        val movements = BackupTable(name = "product_movements", columns = listOf("id"), rows = emptyList())
        return snapshot.copy(
            schemaVersion = 13,
            tables = snapshot.tables.filterNot { it.name == "product_cost_components" } + units + lines + movements,
        )
    }
}
