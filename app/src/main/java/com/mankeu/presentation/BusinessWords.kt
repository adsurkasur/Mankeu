package com.mankeu.presentation

import androidx.annotation.StringRes
import com.mankeu.R
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.MoneyEvent

/**
 * Universal translation layer between domain accounting rules and intuitive, non-accounting language
 * suitable for MSMEs (UMKM) and small businesses.
 */
object BusinessWords {

    @StringRes
    fun roleLabel(role: CategoryRole): Int = when (role) {
        CategoryRole.REVENUE -> R.string.role_income
        CategoryRole.EXPENSE -> R.string.role_expense
        CategoryRole.ASSET -> R.string.role_tools
        CategoryRole.CAPITAL_IN -> R.string.role_own_capital
        CategoryRole.DRAWING -> R.string.role_drawing
    }

    /** Same as [roleLabel] but tolerant of the raw stored string on CategoryEntity.role. */
    @StringRes
    fun roleLabel(rawRole: String): Int = roleLabel(CategoryRole.parse(rawRole))

    @StringRes
    fun eventLabel(type: FinancialEventType): Int = when (type) {
        FinancialEventType.CASH_INCOME -> R.string.kind_cash_in
        FinancialEventType.CASH_EXPENSE -> R.string.kind_cash_out
        FinancialEventType.OWNER_CAPITAL -> R.string.kind_capital_in
        FinancialEventType.OWNER_DRAWING -> R.string.kind_capital_out
        FinancialEventType.ASSET_PURCHASE_CASH -> R.string.kind_tool_cash
        FinancialEventType.CREDIT_PURCHASE -> R.string.kind_credit_purchase
    }

    /** Event types that increase cash on hand. Used for the +/- sign and the in/out colour. */
    val INCOMING_TYPES = setOf(
        FinancialEventType.CASH_INCOME,
        FinancialEventType.OWNER_CAPITAL,
    )

    fun isIncoming(type: FinancialEventType): Boolean = type in INCOMING_TYPES
}
