package com.mankeu.presentation

import com.mankeu.data.CategoryEntity
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent
import com.mankeu.presentation.screens.defaultCategoryId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * The entry form's starting category (review 2026-10-22): sales and costs only, last used first,
 * else the starting list's order — never capital, drawings or an asset purchase by accident.
 */
class EntryDefaultsTest {
    private val sales = category("c-jual", "Penjualan & Jasa", CategoryRole.REVENUE)
    private val other = category("c-lain", "Pendapatan Lain", CategoryRole.REVENUE)
    private val capital = category("c-modal", "Setoran Modal Pemilik", CategoryRole.CAPITAL_IN)
    private val stock = category("c-bahan", "Bahan Baku & Stok", CategoryRole.EXPENSE)
    private val asset = category("c-aset", "Beli Peralatan & Aset", CategoryRole.ASSET)
    private val drawing = category("c-prive", "Ambil untuk Pribadi (Prive)", CategoryRole.DRAWING)
    private val moneyIn = listOf(other, capital, sales) // as the database lists them: alphabetical
    private val moneyOut = listOf(drawing, stock, asset)

    @Test
    fun `a first sale starts on Penjualan & Jasa, not the alphabetically first Pendapatan Lain`() {
        assertEquals(sales.id, defaultCategoryId(moneyIn, emptyList()))
    }

    @Test
    fun `after recording opening money the next sale still starts on a sales category`() {
        val recent = listOf(event("modal", capital, FinancialEventType.OWNER_CAPITAL))

        assertEquals(sales.id, defaultCategoryId(moneyIn, recent))
    }

    @Test
    fun `after buying equipment the next expense starts on a cost category`() {
        val recent = listOf(event("mesin", asset, FinancialEventType.ASSET_PURCHASE_CASH))

        assertEquals(stock.id, defaultCategoryId(moneyOut, recent))
    }

    @Test
    fun `the last used sales or cost category wins`() {
        val recent = listOf(
            event("modal", capital, FinancialEventType.OWNER_CAPITAL),
            event("lain", other, FinancialEventType.CASH_INCOME),
            event("jual", sales, FinancialEventType.CASH_INCOME),
        )

        assertEquals(other.id, defaultCategoryId(moneyIn, recent))
    }

    @Test
    fun `with only non-operating categories the first one is used rather than none`() {
        assertEquals(capital.id, defaultCategoryId(listOf(capital), emptyList()))
    }

    private fun category(id: String, name: String, role: CategoryRole) =
        CategoryEntity(id = id, businessId = "b", role = role.name, name = name, normalizedName = name.lowercase(), createdAtEpochMillis = 0)

    private fun event(id: String, category: CategoryEntity, type: FinancialEventType) = MoneyEvent(
        id = id, businessId = "b", projectId = "p", type = type, amountRupiah = 10_000,
        date = LocalDate.of(2026, 10, 22), description = id, category = category.name, categoryId = category.id,
    )
}
