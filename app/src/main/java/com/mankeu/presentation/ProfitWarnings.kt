package com.mankeu.presentation

import com.mankeu.R
import com.mankeu.domain.MonthProfit

internal fun MonthProfit.incompleteWarningResource(): Int = when {
    uncertainExpenseCategoryCount > 0 && salesWithoutCostCount > 0 -> R.string.profit_incomplete_both
    uncertainExpenseCategoryCount > 0 -> R.string.profit_incomplete_category
    else -> R.string.profit_incomplete
}
