package com.mankeu.data

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
}
