package com.mankeu.data

import com.mankeu.domain.ActualMonthInputs
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostComponent
import com.mankeu.domain.CostNature
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.PlanInputs
import com.mankeu.domain.PlanLine
import com.mankeu.domain.PlanProduct
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.Recurrence
import com.mankeu.domain.TransactionCalculation
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Turns stored rows into the pure inputs of [com.mankeu.domain.PlanCalculator] and
 * [com.mankeu.domain.ActualBepCalculator] (DEC-014). No database access, so it is unit-tested.
 */
object PlanAssembly {
    /** Assets bought before schema 9 carry no useful life; this is the documented default. */
    const val DEFAULT_ASSET_LIFE_MONTHS = 36

    /**
     * Until Fase 3 rewrites the plan it prices each product by its main sale unit (DEC-028 §8), with
     * the HPP of that unit as its one variable cost, so the plan and the Produk tab show the same number.
     */
    fun products(rows: List<ProductRecord>, materialPrices: Map<String, Long>): List<PlanProduct> = rows.mapNotNull { row ->
        val spec = row.spec
        val main = spec.saleUnits.firstOrNull() ?: return@mapNotNull null
        val hpp = ProductCosting.cost(spec, materialPrices).perSaleUnit.first().hppRupiah
        PlanProduct(
            id = spec.id,
            name = spec.name,
            unit = main.name,
            priceRupiah = main.priceRupiah,
            targetPerMonth = TransactionCalculation.parseQuantity(row.product.targetPerMonthText),
            components = if (hpp > 0) listOf(CostComponent("HPP", hpp, BigDecimal.ONE)) else emptyList(),
            sellFromMonth = row.product.sellFromMonth,
        )
    }

    /**
     * The plan period: the business unit's start..end months when both are set, else the span of
     * the dated plan lines, else just [today]'s month.
     */
    fun months(project: ProjectEntity?, planItems: List<BudgetItemEntity>, today: LocalDate): List<YearMonth> {
        val start = project?.startDateEpochDay?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
        val end = project?.endDateEpochDay?.let { YearMonth.from(LocalDate.ofEpochDay(it)) }
        val dated = planItems.mapNotNull { it.plannedDateEpochDay?.let { d -> YearMonth.from(LocalDate.ofEpochDay(d)) } }
        val (from, to) = when {
            start != null && end != null && !end.isBefore(start) -> start to end
            dated.isNotEmpty() -> dated.min() to dated.max()
            else -> YearMonth.from(today).let { it to it }
        }
        return generateSequence(from) { it.plusMonths(1).takeIf { next -> !next.isAfter(to) } }.toList()
    }

    fun planInputs(
        project: ProjectEntity?,
        products: List<ProductRecord>,
        planItems: List<BudgetItemEntity>,
        categories: List<CategoryEntity>,
        today: LocalDate,
        materialPrices: Map<String, Long>,
    ): PlanInputs {
        val byId = categories.associateBy { it.id }
        return PlanInputs(
            months = months(project, planItems, today),
            products = products(products, materialPrices),
            lines = planItems.filterNot { it.archived }.map { item ->
                val category = byId[item.categoryId]
                val role = CategoryRole.parse(category?.role)
                PlanLine(
                    name = item.name,
                    role = role,
                    amountRupiah = item.plannedTotalRupiah,
                    recurrence = if (item.recurrence == Recurrence.MONTHLY.name) Recurrence.MONTHLY else Recurrence.ONCE,
                    nature = nature(item.costNature ?: category?.costNature).takeIf { role == CategoryRole.EXPENSE },
                    month = item.plannedDateEpochDay?.let { YearMonth.from(LocalDate.ofEpochDay(it)) },
                    usefulLifeMonths = if (role == CategoryRole.ASSET) item.usefulLifeMonths ?: DEFAULT_ASSET_LIFE_MONTHS else null,
                )
            },
        )
    }

    /**
     * One month of the Realisasi book: sales (with units when a product was named), fixed and
     * variable spend by effective cost nature, and depreciation of every asset bought on or before
     * the month that is still within its life. Capital and drawings are neither cost nor revenue.
     */
    fun actualMonth(month: YearMonth, rows: List<CostRow>): ActualMonthInputs {
        val inMonth = rows.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == month }
        val sales = inMonth.filter { it.type == FinancialEventType.CASH_INCOME.name }
        val named = sales.filter { it.productId != null && TransactionCalculation.parseQuantity(it.quantityText) != null }
        val expenses = inMonth.filter { it.type == FinancialEventType.CASH_EXPENSE.name }
        val depreciation = rows.filter { it.type == FinancialEventType.ASSET_PURCHASE_CASH.name }.sumOf { asset ->
            val bought = YearMonth.from(LocalDate.ofEpochDay(asset.dateEpochDay))
            val life = asset.usefulLifeMonths ?: DEFAULT_ASSET_LIFE_MONTHS
            if (!month.isBefore(bought) && month.isBefore(bought.plusMonths(life.toLong()))) asset.amountRupiah / life else 0L
        }
        return ActualMonthInputs(
            month = month,
            unitsSold = named.groupBy { it.productId!! }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { s, r -> s + TransactionCalculation.parseQuantity(r.quantityText)!! } },
            revenueRupiah = sales.sumOf { it.amountRupiah },
            unattributedRevenueRupiah = (sales - named.toSet()).sumOf { it.amountRupiah },
            fixedCostRupiah = expenses.filter { nature(it.costNature) != CostNature.VARIABLE }.sumOf { it.amountRupiah },
            variableSpendRupiah = expenses.filter { nature(it.costNature) == CostNature.VARIABLE }.sumOf { it.amountRupiah },
            depreciationRupiah = depreciation,
        )
    }

    fun nature(raw: String?): CostNature? = CostNature.entries.firstOrNull { it.name == raw }
}
