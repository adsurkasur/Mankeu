package com.mankeu.presentation

import com.mankeu.data.ProductRecord
import com.mankeu.data.SaleRecord
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.SaleStatus
import java.time.LocalDate

enum class CatatanFilter {
    ALL,
    UNPAID,
    INCOME,
    EXPENSE,
}

sealed interface UnifiedLedgerItem {
    val id: String
    val date: LocalDate
    val cashDeltaRupiah: Long

    data class Sale(
        val record: SaleRecord,
        val productSummary: String,
        val paidOnDateRupiah: Long,
    ) : UnifiedLedgerItem {
        override val id: String get() = "sale_" + record.sale.id
        override val date: LocalDate get() = LocalDate.ofEpochDay(record.sale.dateEpochDay)
        override val cashDeltaRupiah: Long get() = paidOnDateRupiah
        val totalRupiah: Long get() = record.total
        val outstandingRupiah: Long get() = record.outstanding
        val status: SaleStatus get() = record.status
        val customerName: String? get() = record.contact?.name
    }

    data class SalePayment(
        val event: MoneyEvent,
        val customerName: String,
        val saleRecord: SaleRecord?,
    ) : UnifiedLedgerItem {
        override val id: String get() = "payment_" + event.id
        override val date: LocalDate get() = event.date
        override val cashDeltaRupiah: Long get() = event.amountRupiah
        val amountRupiah: Long get() = event.amountRupiah
    }

    data class Money(
        val event: MoneyEvent,
    ) : UnifiedLedgerItem {
        override val id: String get() = "event_" + event.id
        override val date: LocalDate get() = event.date
        val isCreditPurchase: Boolean get() = event.type == FinancialEventType.CREDIT_PURCHASE
        val isIncoming: Boolean get() = BusinessWords.isIncoming(event.type)
        override val cashDeltaRupiah: Long get() = when {
            isIncoming -> event.amountRupiah
            isCreditPurchase -> 0L
            else -> -event.amountRupiah
        }
        val amountRupiah: Long get() = event.amountRupiah
        val description: String get() = event.description
        val category: String get() = event.category
    }
}

data class LedgerDayGroup(
    val date: LocalDate,
    val items: List<UnifiedLedgerItem>,
    val totalInRupiah: Long,
    val totalOutRupiah: Long,
)

object UnifiedLedger {

    fun hasAnyEntries(sales: List<SaleRecord>, events: List<MoneyEvent>): Boolean {
        return sales.isNotEmpty() || events.isNotEmpty()
    }

    fun build(
        sales: List<SaleRecord>,
        events: List<MoneyEvent>,
        products: List<ProductRecord>,
        filter: CatatanFilter,
        search: String = "",
        today: LocalDate = LocalDate.now(),
    ): List<LedgerDayGroup> {
        val productMap = products.associateBy { it.product.id }
        val saleMap = sales.associateBy { it.sale.id }

        val absorbedEventIds = mutableSetOf<String>()

        val saleItems = sales.map { saleRecord ->
            val saleDate = LocalDate.ofEpochDay(saleRecord.sale.dateEpochDay)
            val paymentsOnSaleDate = saleRecord.payments.filter { it.date == saleDate }
            paymentsOnSaleDate.forEach { absorbedEventIds.add(it.id) }
            val paidOnDate = paymentsOnSaleDate.sumOf { it.amountRupiah }

            // Also absorb any matching events in `events` on the same date for this sale
            events.filter { it.saleId == saleRecord.sale.id && it.date == saleDate }
                .forEach { absorbedEventIds.add(it.id) }

            val productSummary = saleRecord.lines.sortedBy { it.sortOrder }.joinToString(", ") { line ->
                val pName = productMap[line.productId]?.product?.name ?: "—"
                "${line.quantityText} ${line.unitName} $pName"
            }

            UnifiedLedgerItem.Sale(
                record = saleRecord,
                productSummary = productSummary,
                paidOnDateRupiah = paidOnDate,
            )
        }

        val nonAbsorbedEvents = events.filterNot { it.id in absorbedEventIds }

        val moneyItems = nonAbsorbedEvents.map { event ->
            if (event.saleId != null) {
                val linkedSale = saleMap[event.saleId]
                val custName = linkedSale?.contact?.name ?: event.description.removePrefix("Bayar nota ").ifBlank { "Umum" }
                UnifiedLedgerItem.SalePayment(
                    event = event,
                    customerName = custName,
                    saleRecord = linkedSale,
                )
            } else {
                UnifiedLedgerItem.Money(event)
            }
        }

        val allItems: List<UnifiedLedgerItem> = saleItems + moneyItems

        // Filter by CatatanFilter
        val filteredByKind = allItems.filter { item ->
            when (filter) {
                CatatanFilter.ALL -> true
                CatatanFilter.UNPAID -> when (item) {
                    is UnifiedLedgerItem.Sale -> item.outstandingRupiah > 0
                    is UnifiedLedgerItem.Money -> item.isCreditPurchase
                    is UnifiedLedgerItem.SalePayment -> false
                }
                CatatanFilter.INCOME -> when (item) {
                    is UnifiedLedgerItem.Sale -> item.cashDeltaRupiah > 0
                    is UnifiedLedgerItem.SalePayment -> true
                    is UnifiedLedgerItem.Money -> item.isIncoming
                }
                CatatanFilter.EXPENSE -> when (item) {
                    is UnifiedLedgerItem.Sale -> false
                    is UnifiedLedgerItem.SalePayment -> false
                    is UnifiedLedgerItem.Money -> !item.isIncoming && !item.isCreditPurchase
                }
            }
        }

        // Filter by search query
        val trimmedSearch = search.trim()
        val filtered = if (trimmedSearch.isBlank()) {
            filteredByKind
        } else {
            filteredByKind.filter { item ->
                when (item) {
                    is UnifiedLedgerItem.Sale -> {
                        item.customerName?.contains(trimmedSearch, ignoreCase = true) == true ||
                            item.productSummary.contains(trimmedSearch, ignoreCase = true) ||
                            item.record.sale.notes.contains(trimmedSearch, ignoreCase = true)
                    }
                    is UnifiedLedgerItem.SalePayment -> {
                        item.customerName.contains(trimmedSearch, ignoreCase = true) ||
                            item.event.description.contains(trimmedSearch, ignoreCase = true) ||
                            item.event.category.contains(trimmedSearch, ignoreCase = true)
                    }
                    is UnifiedLedgerItem.Money -> {
                        item.description.contains(trimmedSearch, ignoreCase = true) ||
                            item.category.contains(trimmedSearch, ignoreCase = true) ||
                            item.event.reference?.contains(trimmedSearch, ignoreCase = true) == true
                    }
                }
            }
        }

        // Group by date descending
        return filtered.groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .map { (date, items) ->
                val totalIn = items.sumOf { it.cashDeltaRupiah.coerceAtLeast(0) }
                val totalOut = items.sumOf { (-it.cashDeltaRupiah).coerceAtLeast(0) }
                LedgerDayGroup(
                    date = date,
                    items = items,
                    totalInRupiah = totalIn,
                    totalOutRupiah = totalOut,
                )
            }
    }
}
