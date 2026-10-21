package com.mankeu.domain

import java.time.LocalDate

enum class BookMode { PROJECTION, REALIZATION }

/** Strict cash (DEC-006, DEC-011): money that actually moved, nothing owed. */
enum class FinancialEventType {
    CASH_INCOME,
    CASH_EXPENSE,
    OWNER_CAPITAL,
    OWNER_DRAWING,
    ASSET_PURCHASE_CASH,
}

enum class PostingSide { DEBIT, CREDIT }

enum class LedgerAccount {
    CASH,
    REVENUE,
    OPERATING_EXPENSE,
    OWNER_EQUITY,
    OWNER_DRAWING,
    EQUIPMENT_ASSET,
}

enum class CashFlowRole { OPERATING, INVESTING, FINANCING }

/**
 * What a category means, and therefore which cash type a transaction under it is (DEC-011 K-2):
 * money in + REVENUE = income, money in + CAPITAL_IN = owner capital, money out + EXPENSE =
 * expense, money out + DRAWING = owner drawing, money out + ASSET = equipment purchase.
 */
enum class CategoryRole {
    REVENUE,
    EXPENSE,
    CAPITAL_IN,
    DRAWING,
    ASSET,
    ;

    /** The cash type a transaction under this category is, or null when the direction does not fit. */
    fun cashTypeFor(incoming: Boolean): FinancialEventType? = when (this) {
        REVENUE -> FinancialEventType.CASH_INCOME.takeIf { incoming }
        CAPITAL_IN -> FinancialEventType.OWNER_CAPITAL.takeIf { incoming }
        EXPENSE -> FinancialEventType.CASH_EXPENSE.takeIf { !incoming }
        DRAWING -> FinancialEventType.OWNER_DRAWING.takeIf { !incoming }
        ASSET -> FinancialEventType.ASSET_PURCHASE_CASH.takeIf { !incoming }
    }

    companion object {
        /** Stored roles are strings; anything unknown (or pre-schema-8) reads as an expense. */
        fun parse(raw: String?): CategoryRole = entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

data class MoneyEvent(
    val id: String,
    val businessId: String,
    val projectId: String,
    val type: FinancialEventType,
    val amountRupiah: Long,
    val date: LocalDate,
    val description: String,
    val category: String,
    val bookId: String = "",
    val categoryId: String? = null,
    val reference: String? = null,
    val quantityText: String? = null,
    val unit: String? = null,
    val unitPriceRupiah: Long? = null,
    val budgetItemId: String? = null,
    val importBatchId: String? = null,
    val cashFlowRole: CashFlowRole = defaultCashFlowRole(type),
    val costNature: CostNature? = null,
    val productId: String? = null,
    val usefulLifeMonths: Int? = null,
) {
    init {
        require(amountRupiah > 0) { "Amount must be a positive integer rupiah value." }
        require(description.isNotBlank()) { "Description is required." }
        require(businessId.isNotBlank() && projectId.isNotBlank()) { "Business and project scope are required." }
        if (unitPriceRupiah != null) require(unitPriceRupiah >= 0) { "Unit price cannot be negative." }
    }

    companion object {
        fun defaultCashFlowRole(type: FinancialEventType): CashFlowRole = when (type) {
            FinancialEventType.OWNER_CAPITAL,
            FinancialEventType.OWNER_DRAWING,
            -> CashFlowRole.FINANCING

            FinancialEventType.ASSET_PURCHASE_CASH -> CashFlowRole.INVESTING
            FinancialEventType.CASH_INCOME,
            FinancialEventType.CASH_EXPENSE,
            -> CashFlowRole.OPERATING
        }
    }
}

data class Posting(
    val account: LedgerAccount,
    val side: PostingSide,
    val amountRupiah: Long,
)

data class JournalEntry(
    val id: String,
    val sourceEventId: String,
    val date: LocalDate,
    val description: String,
    val postings: List<Posting>,
) {
    fun requireBalanced(): JournalEntry {
        val debit = postings.filter { it.side == PostingSide.DEBIT }.sumOf { it.amountRupiah }
        val credit = postings.filter { it.side == PostingSide.CREDIT }.sumOf { it.amountRupiah }
        require(postings.isNotEmpty() && debit == credit) { "Journal entry is not balanced: debit=$debit credit=$credit" }
        require(postings.all { it.amountRupiah > 0 }) { "Every posting must contain a positive integer rupiah value." }
        return this
    }
}

data class FinancialSummary(
    val operatingIncomeRupiah: Long,
    val operatingExpenseRupiah: Long,
    val profitRupiah: Long,
    val operatingCashFlowRupiah: Long,
    val financingCashFlowRupiah: Long,
    val cashBalanceRupiah: Long,
    val investingCashFlowRupiah: Long = 0,
)
