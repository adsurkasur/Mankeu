package com.mankeu.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyProductShapeTest {
    @Test fun `old product becomes batch of one with its unit as first sale unit`() {
        val draft = LegacyProductShape.draft(
            "Kopi susu", "cup", 10_000, "700",
            listOf(ComponentDraft("Susu", 18_000, "6"), ComponentDraft("Plastik", 25_000, "50"), ComponentDraft("Gula", 10_000, "3")),
            sellFromMonth = 2,
        )
        assertEquals("1", draft.batchYieldText)
        assertEquals(listOf(SaleUnitDraft(name = "cup", contentText = "1", priceRupiah = 10_000)), draft.units)
        assertEquals(listOf(3_000L, 500L, 3_333L), draft.recipe.map { it.amountRupiah })
        assertEquals(2, draft.sellFromMonth)
    }

    @Test fun `sale unit id is derived so independent migrations agree`() {
        assertEquals("p1-u0", LegacyProductShape.saleUnit("p1", "cup", 1).id)
    }
}
