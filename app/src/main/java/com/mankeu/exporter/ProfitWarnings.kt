package com.mankeu.exporter

import com.mankeu.domain.MonthProfit

internal fun MonthProfit.incompleteWarning(): String = when {
    uncertainExpenseCategoryCount > 0 && salesWithoutCostCount > 0 -> "Untung belum dapat dihitung karena ada modal penjualan dan kategori belanja yang belum jelas."
    uncertainExpenseCategoryCount > 0 -> "Untung belum dapat dihitung karena ada belanja yang kategorinya belum jelas."
    else -> "Untung belum dapat dihitung karena ada penjualan yang modal barangnya belum diketahui."
}
