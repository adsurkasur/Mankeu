package com.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class SaleLineInput(val productId: String, val quantity: BigDecimal, val unitPriceRupiah: Long, val hppRupiah: Long)

enum class SaleStatus { PAID, PARTIAL, UNPAID }

/** Pure note math (Fase 2). One rounding rule: half up, via [TransactionCalculation.computeTotalRupiah]. */
object SalesMath {
    fun lineTotal(quantity: BigDecimal, unitPriceRupiah: Long): Long =
        TransactionCalculation.computeTotalRupiah(quantity, unitPriceRupiah)

    fun subtotal(lines: List<SaleLineInput>): Long = lines.sumOf { lineTotal(it.quantity, it.unitPriceRupiah) }

    fun total(lines: List<SaleLineInput>, discountRupiah: Long): Long {
        val sub = subtotal(lines)
        require(discountRupiah in 0..sub) { "discount $discountRupiah outside 0..$sub" }
        return sub - discountRupiah
    }

    /** Discount shared by line value, half up, remainder to the last line; sum == discount. */
    fun discountShares(lines: List<SaleLineInput>, discountRupiah: Long): List<Long> {
        val sub = subtotal(lines)
        require(discountRupiah in 0..sub) { "discount $discountRupiah outside 0..$sub" }
        if (sub == 0L) return lines.map { 0L }
        // Rounding the running total, not each share, keeps every share >= 0 and the sum exact
        // (per-share rounding gave 1, 1, 1, -1 for four equal lines and a discount of 2).
        var cumulative = 0L
        var allocated = 0L
        return lines.map { l ->
            cumulative += lineTotal(l.quantity, l.unitPriceRupiah)
            val upTo = BigDecimal(discountRupiah).multiply(BigDecimal(cumulative))
                .divide(BigDecimal(sub), 0, RoundingMode.HALF_UP).longValueExact()
            (upTo - allocated).also { allocated = upTo }
        }
    }

    fun status(totalRupiah: Long, paidRupiah: Long): SaleStatus = when {
        paidRupiah >= totalRupiah -> SaleStatus.PAID
        paidRupiah > 0 -> SaleStatus.PARTIAL
        else -> SaleStatus.UNPAID
    }

    fun outstanding(totalRupiah: Long, paidRupiah: Long): Long = maxOf(0L, totalRupiah - paidRupiah)

    fun isOverdue(dueDate: LocalDate?, outstanding: Long, today: LocalDate): Boolean =
        dueDate != null && outstanding > 0 && dueDate.isBefore(today)
}
