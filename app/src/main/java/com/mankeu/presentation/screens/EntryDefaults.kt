package com.mankeu.presentation.screens

import com.mankeu.data.CategoryEntity
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.DefaultMasterData
import com.mankeu.domain.MoneyEvent

/**
 * The category the entry form starts on: the sales or cost category last used for this direction
 * (a warung records the same few things all day), else the starting list's order (Penjualan & Jasa,
 * Bahan Baku & Stok).
 *
 * Only revenue and expense categories are ever preselected. Alphabetical order once preselected
 * "Pendapatan Lain" for every sale; "last used" over all categories then preselected Setoran Modal
 * after recording opening money, so the next sale became capital in one tap (review 2026-10-22).
 * Capital, drawings and asset purchases are always chosen on purpose.
 *
 * [recentEvents] is newest first; [compatible] are the categories valid for this direction.
 */
fun defaultCategoryId(compatible: List<CategoryEntity>, recentEvents: List<MoneyEvent>): String? {
    val operating = compatible.filter { CategoryRole.parse(it.role) in OPERATING }
    val starting = DefaultMasterData.categories.map { it.name }
    return recentEvents.firstOrNull { event -> operating.any { it.id == event.categoryId } }?.categoryId
        ?: operating.minByOrNull { category -> starting.indexOf(category.name).let { if (it < 0) Int.MAX_VALUE else it } }?.id
        ?: compatible.firstOrNull()?.id
}

private val OPERATING = setOf(CategoryRole.REVENUE, CategoryRole.EXPENSE)
