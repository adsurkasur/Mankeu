package com.mankeu.data

import com.mankeu.domain.ConsignmentMath
import com.mankeu.domain.ConsignmentMove
import com.mankeu.domain.MoneyEvent
import com.mankeu.domain.SaleStatus
import com.mankeu.domain.SalesMath
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal
import java.time.LocalDate

/** A customer and/or supplier as the contact form edits it (F2-2). */
data class ContactDraft(
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val isCustomer: Boolean = true,
    val isSupplier: Boolean = false,
)

/** One product line of a note: [quantityText] of [unitName] (the base unit or a sale unit of the product). */
data class SaleLineDraft(val productId: String, val unitName: String, val quantityText: String, val unitPriceRupiah: Long)

/** A sale note as the note form edits it (spec #17, F2-3, F2-7). [paidNowRupiah] only applies when creating. */
data class SaleDraft(
    val contactId: String?,
    val date: LocalDate,
    val dueDate: LocalDate?,
    val discountRupiah: Long,
    val notes: String,
    val lines: List<SaleLineDraft>,
    val paidNowRupiah: Long,
)

/** One product sent on consignment, priced per [unitName] for the store to pay (F2-6). */
data class ConsignmentLineDraft(val productId: String, val unitName: String, val quantityText: String, val priceRupiah: Long)

/** A note with its lines, payments and customer; every figure is derived (DEC-029 §6). */
data class SaleRecord(
    val sale: SaleEntity,
    val lines: List<SaleLineEntity>,
    val payments: List<MoneyEvent>,
    val contact: ContactEntity?,
) {
    val subtotal: Long get() = lines.sumOf { it.totalRupiah }
    val total: Long get() = subtotal - sale.discountRupiah
    val paid: Long get() = payments.sumOf { it.amountRupiah }
    val outstanding: Long get() = SalesMath.outstanding(total, paid)
    val status: SaleStatus get() = SalesMath.status(total, paid)
    val hpp: Long get() = lines.sumOf { it.hppRupiah }
    val dueDate: LocalDate? get() = sale.dueDateEpochDay?.let(LocalDate::ofEpochDay)
    fun overdue(today: LocalDate): Boolean = SalesMath.isOverdue(dueDate, outstanding, today)
}

/** A credit purchase from a supplier with its payments (F2-5). */
data class PayableRecord(
    val purchase: MoneyEvent,
    val payments: List<MoneyEvent>,
    val contact: ContactEntity?,
) {
    val total: Long get() = purchase.amountRupiah
    val paid: Long get() = payments.sumOf { it.amountRupiah }
    val outstanding: Long get() = SalesMath.outstanding(total, paid)
    val status: SaleStatus get() = SalesMath.status(total, paid)
    fun overdue(today: LocalDate): Boolean = SalesMath.isOverdue(purchase.dueDate, outstanding, today)
}

/** One consignment line with what of it is still at the store, in base units. */
data class ConsignmentLineState(
    val line: ConsignmentLineEntity,
    val sentBase: BigDecimal,
    val soldBase: BigDecimal,
    val returnedBase: BigDecimal,
) {
    val content: BigDecimal get() = TransactionCalculation.parseQuantity(line.contentText) ?: BigDecimal.ONE
    val remainingBase: BigDecimal get() = sentBase - soldBase - returnedBase
}

/** A consignment sent to a store (F2-6). A product appears at most once per consignment. */
data class ConsignmentRecord(
    val consignment: ConsignmentEntity,
    val contact: ContactEntity?,
    val lines: List<ConsignmentLineState>,
) {
    val remainingBase: BigDecimal get() = lines.fold(BigDecimal.ZERO) { sum, l -> sum + l.remainingBase }
}

/** Base units still at each (store, product), through [ConsignmentMath]. */
fun List<ConsignmentRecord>.balances(): Map<Pair<String, String>, BigDecimal> = ConsignmentMath.balances(
    flatMap { record ->
        val store = record.consignment.contactId
        record.lines.flatMap { l ->
            listOf(
                ConsignmentMove(store, l.line.productId, l.sentBase, ConsignmentMove.Kind.SENT),
                ConsignmentMove(store, l.line.productId, l.soldBase, ConsignmentMove.Kind.SOLD),
                ConsignmentMove(store, l.line.productId, l.returnedBase, ConsignmentMove.Kind.RETURNED),
            )
        }
    },
)
