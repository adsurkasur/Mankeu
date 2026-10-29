package com.mankeu.data

import com.mankeu.domain.ProductMovementType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ProductRecordTest {
    /** Final review M-12: a movement type this version does not know (synced, restored) is skipped, not a crash. */
    @Test
    fun `an unknown movement type is skipped`() {
        val record = ProductRecord(
            ProductEntity(id = "k", projectId = "p", name = "Keripik", unit = "bungkus", batchYieldText = "400", createdAtEpochMillis = 0),
            units = emptyList(),
            recipe = emptyList(),
            movements = listOf(
                ProductMovementEntity("m1", "k", "PRODUCTION", "400", "1", dateEpochDay = 1, createdAtEpochMillis = 1),
                ProductMovementEntity("m2", "k", "RETURN_FROM_A_LATER_VERSION", "50", dateEpochDay = 2, createdAtEpochMillis = 2),
            ),
        )

        assertEquals(0, BigDecimal(400).compareTo(record.stock))
    }

    /** Cleanup P1: the Riwayat stok page lists what makes up the stock, oldest first, with the stock after each line. */
    @Test
    fun `the stock history lists production, counts and sales in order with the stock after each`() {
        val record = ProductRecord(
            ProductEntity(id = "k", projectId = "p", name = "Keripik", unit = "bungkus", batchYieldText = "400", createdAtEpochMillis = 0),
            units = listOf(
                ProductUnitEntity("u1", "k", "bungkus", "1", 8_000, 0),
                ProductUnitEntity("u2", "k", "dus", "40", 300_000, 1),
            ),
            recipe = emptyList(),
            movements = listOf(
                ProductMovementEntity("m2", "k", "ADJUSTMENT", "300", dateEpochDay = 2, createdAtEpochMillis = 9),
                ProductMovementEntity("m1", "k", "PRODUCTION", "400", "1", dateEpochDay = 1, notes = "pagi", createdAtEpochMillis = 1),
            ),
            sales = listOf(
                ProductSaleRow("s2", "k", "10", null, dateEpochDay = 3, createdAtEpochMillis = 10),
                ProductSaleRow("s1", "k", "2", "dus", dateEpochDay = 2, createdAtEpochMillis = 5),
            ),
        )

        val history = record.history

        assertEquals(listOf("m1", "s1", "m2", "s2"), history.map { it.id })
        assertEquals(listOf(ProductMovementType.PRODUCTION, ProductMovementType.SALE, ProductMovementType.ADJUSTMENT, ProductMovementType.SALE), history.map { it.type })
        assertEquals(listOf("400", "80", "300", "10"), history.map { it.quantity.toPlainString() })
        assertEquals(listOf("400", "320", "300", "290"), history.map { it.stockAfter.toPlainString() })
        assertEquals(listOf(null, "2 dus", null, "10 bungkus"), history.map { it.soldText })
        assertEquals(listOf("1", null, null, null), history.map { it.batchesText })
        assertEquals("pagi", history.first().notes)
        assertEquals(0, BigDecimal(290).compareTo(record.stock))
    }

    /** DEC-029 §8: consignment sends leave the warehouse, a sell-through note does not take stock again, a return comes back. */
    @Test
    fun `consignment moves stock to the store and back`() {
        val record = ProductRecord(
            ProductEntity(id = "k", projectId = "p", name = "Keripik", unit = "bungkus", batchYieldText = "400", createdAtEpochMillis = 0),
            units = listOf(ProductUnitEntity("u2", "k", "dus", "40", 300_000, 0)),
            recipe = emptyList(),
            movements = listOf(
                ProductMovementEntity("m1", "k", "PRODUCTION", "800", "2", dateEpochDay = 1, createdAtEpochMillis = 1),
                ProductMovementEntity("r1", "k", "CONSIGN_RETURN", "160", dateEpochDay = 5, createdAtEpochMillis = 5, consignmentId = "c1"),
            ),
            sales = listOf(
                ProductSaleRow("l1", "k", "5", "dus", dateEpochDay = 2, createdAtEpochMillis = 2, contentText = "40"),
                ProductSaleRow("l2", "k", "6", "dus", dateEpochDay = 4, createdAtEpochMillis = 4, contentText = "40", consignmentId = "c1"),
            ),
            consignmentSends = listOf(ConsignmentSendRow("cl1", "k", "10", "40", dateEpochDay = 3, createdAtEpochMillis = 3)),
        )

        assertEquals(listOf("m1", "l1", "cl1", "r1"), record.history.map { it.id })
        assertEquals(listOf("800", "600", "200", "360"), record.history.map { it.stockAfter.toPlainString() })
        assertEquals(0, BigDecimal(360).compareTo(record.stock))
        assertEquals(0, BigDecimal.ZERO.compareTo(record.consigned))
    }
}
