package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class StockManagementTest {

    @Test
    fun `stock level calculation respects initial plus in minus out flow`() {
        val initial = BigDecimal("50.0")
        val movements = listOf(
            StockMovement("1", "item-1", StockMovementType.IN, BigDecimal("30.0"), LocalDate.now()),
            StockMovement("2", "item-1", StockMovementType.OUT, BigDecimal("25.0"), LocalDate.now()),
            StockMovement("3", "item-1", StockMovementType.IN, BigDecimal("10.0"), LocalDate.now()),
            StockMovement("4", "item-1", StockMovementType.OUT, BigDecimal("15.0"), LocalDate.now()),
        )

        // 50 + 30 - 25 + 10 - 15 = 50
        val finalStock = StockCalculation.calculateStockLevel(initial, movements)
        assertEquals(BigDecimal("50.000"), finalStock)
    }

    @Test
    fun `stock adjustment resets current physical stock count`() {
        val initial = BigDecimal("100.0")
        val movements = listOf(
            StockMovement("1", "item-1", StockMovementType.OUT, BigDecimal("40.0"), LocalDate.now()), // 60
            StockMovement("2", "item-1", StockMovementType.ADJUSTMENT, BigDecimal("55.0"), LocalDate.now()), // Opname: 55
            StockMovement("3", "item-1", StockMovementType.IN, BigDecimal("20.0"), LocalDate.now()), // 75
        )

        val finalStock = StockCalculation.calculateStockLevel(initial, movements)
        assertEquals(BigDecimal("75.000"), finalStock)
    }

    @Test
    fun `inventory total value computation uses exact integer rupiah`() {
        val cupItem = StockItem(
            id = "1",
            businessId = "biz-1",
            name = "Cup 16oz",
            unit = "pcs",
            basePriceRupiah = 1_000,
            currentStock = BigDecimal("200"),
            minAlertStock = BigDecimal("50"),
        )
        val coffeeItem = StockItem(
            id = "2",
            businessId = "biz-1",
            name = "Kopi Susu Bubuk",
            unit = "cup",
            basePriceRupiah = 3_500,
            currentStock = BigDecimal("100"),
            minAlertStock = BigDecimal("20"),
        )

        // Cup: 200 * 1.000 = 200.000
        // Kopi: 100 * 3.500 = 350.000
        // Total = 550.000
        val totalValue = StockCalculation.computeInventoryValue(listOf(cupItem, coffeeItem))
        assertEquals(550_000L, totalValue)

        assertFalse(cupItem.isLowStock)
        assertFalse(coffeeItem.isLowStock)

        // Test low stock alert
        val lowCup = cupItem.copy(currentStock = BigDecimal("45"))
        assertTrue(lowCup.isLowStock)
    }

    @Test
    fun `summary counts items, low-stock items and total value`() {
        val items = listOf(
            StockItem("1", "biz-1", "Cup", "pcs", 1_000, BigDecimal("200"), BigDecimal("50")),
            StockItem("2", "biz-1", "Gula", "kg", 15_000, BigDecimal("2"), BigDecimal("2")),
        )

        assertEquals(StockSummary(totalItems = 2, lowStockCount = 1, totalInventoryValueRupiah = 230_000), StockCalculation.summarize(items))
    }

    /** U3 / DEC-030 §3: material stock is derived; it may go below zero (recorded after the fact) instead of being clamped. */
    @Test
    fun `history orders by date then time, purchases add, production use subtracts, a count sets, negative allowed`() {
        val d1 = LocalDate.of(2026, 10, 1)
        val d2 = LocalDate.of(2026, 10, 2)
        val lines = StockCalculation.history(
            BigDecimal("5"),
            listOf(
                StockMovement("count", "m", StockMovementType.ADJUSTMENT, BigDecimal("12"), d2, createdAtEpochMillis = 30),
                StockMovement("buy", "m", StockMovementType.PURCHASE, BigDecimal("10"), d1, createdAtEpochMillis = 50),
                StockMovement("use", "m", StockMovementType.CONSUME, BigDecimal("20"), d1, createdAtEpochMillis = 60),
                StockMovement("out", "m", StockMovementType.OUT, BigDecimal("4"), d2, createdAtEpochMillis = 40),
            ),
        )
        assertEquals(listOf("buy", "use", "count", "out"), lines.map { it.movement.id })
        assertEquals(listOf("15", "-5", "12", "8"), lines.map { it.stockAfter.stripTrailingZeros().toPlainString() })
        assertEquals(0, BigDecimal("-3").compareTo(StockCalculation.calculateStockLevel(BigDecimal.ONE, listOf(StockMovement("o", "m", StockMovementType.OUT, BigDecimal("4"), d1)))))
    }

    /** Audit L05: no minimum set means no "menipis"; with a minimum, the real stock decides. */
    @Test
    fun `low stock needs a minimum and reads the real stock`() {
        val item = StockItem("1", "biz-1", "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        assertFalse(item.isLowStock)
        assertTrue(item.copy(minAlertStock = BigDecimal("5"), currentStock = BigDecimal("5")).isLowStock)
        assertFalse(item.copy(minAlertStock = BigDecimal("5"), currentStock = BigDecimal("10")).isLowStock)
    }

    @Test
    fun `movement use case allows taking more than recorded, refuses empty or derived kinds, and stores only the movement`() = runBlocking {
        val dao = FakeStockDao(StockItemEntity("i1", "biz-1", "Gula", "kg", 15_000, initialStockText = "5", createdAtEpochMillis = 0))
        val useCase = RecordStockMovementUseCase(dao)
        val day = LocalDate.of(2026, 10, 20)

        assertTrue(useCase.execute("i1", StockMovementType.OUT, BigDecimal("6"), day).isSuccess)
        assertTrue(useCase.execute("i1", StockMovementType.IN, BigDecimal.ZERO, day).isFailure)
        assertTrue(useCase.execute("i1", StockMovementType.CONSUME, BigDecimal.ONE, day).isFailure)
        assertTrue(useCase.execute("i1", StockMovementType.PURCHASE, BigDecimal.ONE, day).isFailure)
        assertTrue(useCase.execute("i1", StockMovementType.ADJUSTMENT, BigDecimal.ZERO, day).isSuccess)
        assertEquals(listOf("OUT", "ADJUSTMENT"), dao.movements.map { it.movementType })
        assertEquals(0, dao.updates)
    }

    private class FakeStockDao(vararg initial: StockItemEntity) : StockDao {
        val items = initial.associateBy { it.id }.toMutableMap()
        val movements = mutableListOf<StockMovementEntity>()
        override suspend fun insertStockItem(item: StockItemEntity) { items[item.id] = item }
        var updates = 0
        override suspend fun updateStockItem(item: StockItemEntity) { updates++; items[item.id] = item }
        override suspend fun getStockItem(id: String) = items[id]
        override fun observeStockItems(businessId: String, projectId: String?) = flowOf(items.values.toList())
        override suspend fun getStockItems(businessId: String, projectId: String?) = items.values.toList()
        override suspend fun getAllStockItems(businessId: String) = items.values.toList()
        override suspend fun countStockItemsOfProject(projectId: String) = items.values.count { it.projectId == projectId }
        override suspend fun insertStockMovement(movement: StockMovementEntity) { movements += movement }
        override suspend fun archiveStockItem(id: String, updatedAt: Long) = Unit
        override suspend fun deleteImportedItems(batchId: String) = 0
        override suspend fun getUsedImportedItems(batchId: String) = emptyList<StockItemEntity>()
        override suspend fun releaseFromImport(ids: List<String>) = Unit
        override fun observeBusinessMovements(businessId: String) = flowOf(movements.toList())
        override suspend fun insertStockMovements(movements: List<StockMovementEntity>) { this.movements += movements }
        override suspend fun deleteOrphanConsumption() = 0
        override suspend fun deleteImportedMovements(batchId: String) = 0
        override suspend fun getManualMovements(businessId: String) = movements.toList()
        override suspend fun getConsumptionOfProject(projectId: String) = emptyList<StockMovementEntity>()
    }
}
