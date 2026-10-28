package com.mankeu.backup

import com.mankeu.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BackupUpgradeTest {
    private fun table(name: String, columns: List<String> = listOf("id"), rows: List<List<BackupValue>> = emptyList()) = BackupTable(name, columns, rows)

    private val schema12Tables = (AppDatabase.DATA_TABLES - listOf("product_units", "product_recipe_lines", "product_movements")).map { name ->
        when (name) {
            "products" -> table(name, listOf("id", "projectId", "name", "unit", "priceRupiah"), listOf(listOf(BackupValue.Text("kopi"), BackupValue.Text("p"), BackupValue.Text("Kopi"), BackupValue.Text("cup"), BackupValue.Integer(10_000))))
            else -> table(name)
        }
    } + table(
        "product_cost_components",
        listOf("id", "productId", "name", "amountRupiah", "yieldText", "sortOrder"),
        listOf(listOf(BackupValue.Text("c1"), BackupValue.Text("kopi"), BackupValue.Text("Susu"), BackupValue.Integer(18_000), BackupValue.Text("6"), BackupValue.Integer(0))),
    )

    @Test fun `schema 12 snapshot gets the current table set and the reshaped product rows`() {
        val upgraded = BackupUpgrade.toCurrent(PortableBackup(12, 0, schema12Tables))
        assertEquals(13, upgraded.schemaVersion)
        assertEquals(AppDatabase.DATA_TABLES.toSet(), upgraded.tables.map { it.name }.toSet())
        val units = upgraded.tables.single { it.name == "product_units" }
        assertEquals(listOf(BackupValue.Text("kopi-u0"), BackupValue.Text("kopi"), BackupValue.Text("cup"), BackupValue.Text("1"), BackupValue.Integer(10_000), BackupValue.Integer(0)), units.rows.single())
        val lines = upgraded.tables.single { it.name == "product_recipe_lines" }
        assertEquals(BackupValue.Integer(3_000), lines.rows.single()[5])
    }

    @Test fun `current snapshot passes through untouched`() {
        val current = PortableBackup(13, 0, AppDatabase.DATA_TABLES.map { table(it) })
        assertSame(current, BackupUpgrade.toCurrent(current))
    }
}
