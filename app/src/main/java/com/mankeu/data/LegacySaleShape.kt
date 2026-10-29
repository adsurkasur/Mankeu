package com.mankeu.data

import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.LedgerPolicy
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal
import java.time.LocalDate

/**
 * F2-4 / DEC-029 §10: a schema-13 Realisasi cash sale that names a product becomes a paid one-line
 * note, and the sale event becomes that note's payment. `Migration13To14` and a schema-13 backup
 * (`BackupUpgrade`) both go through here. Ids and timestamps come from the event, so two phones that
 * migrate on their own produce the same rows.
 */
object LegacySaleShape {
    /** The columns of the old sale event this conversion reads. */
    data class OldSale(
        val eventId: String,
        val businessId: String,
        val projectId: String,
        val productId: String,
        val dateEpochDay: Long,
        val createdAtEpochMillis: Long,
        val amountRupiah: Long,
        val quantityText: String?,
        val unit: String?,
        val unitPriceRupiah: Long?,
    )

    fun saleId(eventId: String) = "$eventId-s"
    fun lineId(eventId: String) = "$eventId-l"

    /**
     * [product] is null when the product no longer exists: the line keeps the event's unit, content 1
     * and HPP 0 (still marked estimated). The HPP is the recipe cost now, over the base units sold.
     */
    fun convert(old: OldSale, product: ProductRecord?, materialPrices: Map<String, Long>): Pair<SaleEntity, SaleLineEntity> {
        val quantity = TransactionCalculation.parseQuantity(old.quantityText) ?: BigDecimal.ONE
        val unitName = old.unit?.trim()?.ifBlank { null } ?: product?.let { it.mainUnit?.name ?: it.product.unit } ?: ""
        val content = product?.contentOf(old.unit) ?: BigDecimal.ONE
        val hpp = product?.let { p ->
            ProductCosting.hpp(ProductCosting.cost(p.spec, materialPrices).batchCostRupiah, p.spec.batchYield, content * quantity)
        } ?: 0L
        val sale = SaleEntity(
            id = saleId(old.eventId),
            businessId = old.businessId,
            projectId = old.projectId,
            dateEpochDay = old.dateEpochDay,
            hppEstimated = true,
            createdAtEpochMillis = old.createdAtEpochMillis,
        )
        val line = SaleLineEntity(
            id = lineId(old.eventId),
            saleId = sale.id,
            productId = old.productId,
            unitName = unitName,
            contentText = content.stripTrailingZeros().toPlainString(),
            quantityText = old.quantityText?.trim()?.takeIf { TransactionCalculation.parseQuantity(it) != null } ?: "1",
            unitPriceRupiah = old.unitPriceRupiah ?: TransactionCalculation.computeUnitPriceRupiah(old.amountRupiah, quantity),
            totalRupiah = old.amountRupiah,
            hppRupiah = hpp,
        )
        return sale to line
    }

    /**
     * Material prices per business, exactly as `FinanceRepository` derives them (DEC-028 §2): the
     * newest purchase with a unit price above zero, else the price entered with the item.
     * [purchasesOldestFirst] is (stockItemId, unitPrice) ordered by date, time recorded, rowid.
     */
    fun materialPrices(items: List<Triple<String, String, Long>>, purchasesOldestFirst: List<Pair<String, Long>>): Map<String, Map<String, Long>> {
        val latest = purchasesOldestFirst.filter { it.second > 0 }.toMap()
        return items.groupBy { it.second }.mapValues { (_, own) -> own.associate { (id, _, base) -> id to (latest[id] ?: base) } }
    }

    /** The payment's postings, from the one ledger rule: Kas / Piutang (DEC-029 §2). */
    fun paymentPostings(journalId: String, old: OldSale): List<PostingEntity> = LedgerPolicy.journalFor(
        MoneyEvent(
            id = old.eventId,
            businessId = old.businessId,
            projectId = old.projectId,
            type = FinancialEventType.CASH_INCOME,
            amountRupiah = old.amountRupiah,
            date = LocalDate.ofEpochDay(old.dateEpochDay),
            description = "-",
            category = "",
            saleId = saleId(old.eventId),
        ),
    ).postings.mapIndexed { index, p -> PostingEntity("$journalId-$index", journalId, p.account.name, p.side.name, p.amountRupiah) }
}
