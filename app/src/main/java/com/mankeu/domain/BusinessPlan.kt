package com.mankeu.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/** Whether an expense moves with sales volume (DEC-014). Null on income, capital, drawing, asset. */
enum class CostNature { FIXED, VARIABLE }

/** A plan line is either repeated every month of the plan period or paid once. */
enum class Recurrence { MONTHLY, ONCE }

/** One ingredient of a product's per-unit variable cost: [amountRupiah] buys enough for [yield] units. */
data class CostComponent(
    val name: String,
    val amountRupiah: Long,
    val yield: BigDecimal = BigDecimal.ONE,
) {
    val perUnit: BigDecimal get() = BigDecimal.valueOf(amountRupiah).divide(yield, MONEY_SCALE, RoundingMode.HALF_UP)
}

data class PlanProduct(
    val id: String,
    val name: String,
    val unit: String,
    val priceRupiah: Long,
    /** Planned units sold per month; null means "not set" and leaves the sales mix undefined. */
    val targetPerMonth: BigDecimal?,
    val components: List<CostComponent>,
    /** Month of the plan period (1 = first) from which it sells; before that it neither sells nor buys ingredients. */
    val sellFromMonth: Int = 1,
) {
    val variableCostPerUnit: BigDecimal get() = components.fold(BigDecimal.ZERO) { sum, c -> sum + c.perUnit }
    val contributionMargin: BigDecimal get() = BigDecimal.valueOf(priceRupiah) - variableCostPerUnit
}

/** A non-product plan line: a fixed cost, a one-off cost or income, an asset or planned capital. */
data class PlanLine(
    val name: String,
    val role: CategoryRole,
    val amountRupiah: Long,
    val recurrence: Recurrence = Recurrence.ONCE,
    /** Only for EXPENSE lines; null reads as FIXED (overheads are the safe default for BEP). */
    val nature: CostNature? = null,
    val month: YearMonth? = null,
    /** Only for ASSET lines. */
    val usefulLifeMonths: Int? = null,
)

data class PlanInputs(
    val months: List<YearMonth>,
    val products: List<PlanProduct>,
    val lines: List<PlanLine>,
)

data class ProductBep(
    val product: PlanProduct,
    val variableCostPerUnit: Long,
    val contributionMargin: Long,
    /** Share of planned units, null when the mix is undefined. */
    val mixShare: BigDecimal?,
    /** Units of this product needed to break even (within the mix, or alone when no mix). */
    val breakEvenUnits: BigDecimal?,
    val breakEvenRupiah: Long?,
    val sellsAtALoss: Boolean,
)

/** Monthly fixed cost, itemised the way the lecturer's benchmark presents it. */
data class FixedCostBreakdown(
    val recurring: List<Pair<String, Long>>,
    val oneOffSpread: List<Pair<String, Long>>,
    val depreciation: List<Pair<String, Long>>,
) {
    val totalRupiah: Long get() = (recurring + oneOffSpread + depreciation).sumOf { it.second }
}

data class PlanAnalysis(
    val months: List<YearMonth>,
    val fixedCost: FixedCostBreakdown,
    val products: List<ProductBep>,
    val mixDefined: Boolean,
    /** Mix-weighted contribution margin per unit; null without a mix. */
    val weightedContributionMargin: BigDecimal?,
    /** Total units (all products) and rupiah to break even per month; null without a mix or with no margin. */
    val breakEvenUnits: BigDecimal?,
    val breakEvenRupiah: Long?,
    /** Per month at target volume. */
    val revenueRupiah: Long,
    val variableCostRupiah: Long,
    val totalCostRupiah: Long,
    val profitRupiah: Long,
    val revenueCostRatio: BigDecimal?,
    val benefitCostRatio: BigDecimal?,
    /**
     * What the contribution margin must cover each month for profit to be zero: fixed cost, plus
     * variable costs that are not in any recipe, minus other income. Equals [fixedCost] total when
     * the plan has neither.
     */
    val breakEvenBaseRupiah: Long,
    /** The plan's cash schedule (capital included). */
    val schedule: List<MonthlyCashFlow>,
    /** Deepest cumulative deficit of operations and investment, before any capital paid in. */
    val capitalNeedRupiah: Long,
    /** Capital the plan already schedules (Setoran Modal minus prive). */
    val plannedCapitalRupiah: Long,
    /** Deepest cumulative deficit with the planned capital: what is still missing. */
    val capitalShortfallRupiah: Long,
    /** Whole-period cash summary, same shape as a posted book (depreciation is not cash). */
    val summary: FinancialSummary,
) {
    val feasible: Boolean? get() = benefitCostRatio?.let { it.signum() >= 0 }

    /** Some product starts selling after month 1: monthly figures are period averages, unit BEP is per selling month. */
    val seasonal: Boolean get() = products.any { it.product.sellFromMonth > 1 }
}

/**
 * The business plan behind BEP (DEC-014): products with per-unit recipes, monthly fixed costs,
 * one-off lines, assets with straight-line depreciation and planned capital. Money stays in exact
 * rupiah; intermediate values are BigDecimal and are rounded once, at the end.
 */
object PlanCalculator {
    fun analyse(inputs: PlanInputs): PlanAnalysis {
        val months = inputs.months.ifEmpty { listOfNotNull(inputs.lines.mapNotNull { it.month }.minOrNull()) }
        val monthCount = months.size.coerceAtLeast(1)
        val expenses = inputs.lines.filter { it.role == CategoryRole.EXPENSE }
        fun spread(amount: Long) = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(monthCount.toLong()), 0, RoundingMode.HALF_UP).toLong()

        val fixed = FixedCostBreakdown(
            recurring = expenses.filter { it.nature != CostNature.VARIABLE && it.recurrence == Recurrence.MONTHLY }.map { it.name to it.amountRupiah },
            oneOffSpread = expenses.filter { it.nature != CostNature.VARIABLE && it.recurrence == Recurrence.ONCE }.map { it.name to spread(it.amountRupiah) },
            depreciation = inputs.lines.filter { it.role == CategoryRole.ASSET && (it.usefulLifeMonths ?: 0) > 0 }
                .map { it.name to BigDecimal.valueOf(it.amountRupiah).divide(BigDecimal.valueOf(it.usefulLifeMonths!!.toLong()), 0, RoundingMode.HALF_UP).toLong() },
        )
        // Variable costs outside the recipes (seed for a season, a one-off delivery) do not grow
        // with units sold, and other income does not come from units either, so for break-even
        // both act on the amount the margin has to cover. Leaving them out put "BEP" at a loss.
        val otherVariable = expenses.filter { it.nature == CostNature.VARIABLE }.sumOf { monthly(it, ::spread) }
        val otherRevenue = inputs.lines.filter { it.role == CategoryRole.REVENUE }.sumOf { monthly(it, ::spread) }
        val base = (fixed.totalRupiah + otherVariable - otherRevenue).coerceAtLeast(0)
        val fc = BigDecimal.valueOf(base)

        // Seasonal products (DEC-017) sell in s of the n plan months. Weighting every product by
        // target × s keeps the mix, the monthly averages and BEP on the same footing as the cash
        // schedule; with every product selling from month 1 (s = n) this is the plain formula.
        val n = BigDecimal.valueOf(monthCount.toLong())
        fun sellingMonths(p: PlanProduct) = BigDecimal.valueOf((monthCount - p.sellFromMonth + 1).coerceIn(0, monthCount).toLong())
        val targets = inputs.products.mapNotNull { it.targetPerMonth?.takeIf { t -> t.signum() > 0 } }
        val mixDefined = inputs.products.isNotEmpty() && targets.size == inputs.products.size
        val totalUnits = targets.fold(BigDecimal.ZERO, BigDecimal::add)
        val weightedUnits = if (mixDefined) inputs.products.fold(BigDecimal.ZERO) { sum, p -> sum + p.targetPerMonth!! * sellingMonths(p) } else null
        // Σ target × s × CM: every mix figure below divides by this once, so nothing is rounded twice
        // (rounding the share and the weighted CM first drifted BEP rupiah by a few rupiah).
        val mixMargin = if (mixDefined) inputs.products.fold(BigDecimal.ZERO) { sum, p -> sum + p.contributionMargin * p.targetPerMonth!! * sellingMonths(p) } else null
        val weightedCm = weightedUnits?.takeIf { it.signum() > 0 }?.let { mixMargin!!.divide(it, MONEY_SCALE, RoundingMode.HALF_UP) }
        // Units per selling month, all products together.
        val bepUnits = mixMargin?.takeIf { it.signum() > 0 }?.let { (fc * totalUnits * n).divide(it, 3, RoundingMode.HALF_UP) }

        val productBeps = inputs.products.map { p ->
            val s = sellingMonths(p)
            val share = weightedUnits?.takeIf { it.signum() > 0 }?.let { (p.targetPerMonth!! * s).divide(it, MONEY_SCALE, RoundingMode.HALF_UP) }
            // Units to sell in each month the product is on sale; rupiah below is per plan month.
            val units = when {
                s.signum() == 0 -> null
                mixDefined -> mixMargin?.takeIf { it.signum() > 0 }?.let { (fc * p.targetPerMonth!! * n).divide(it, 3, RoundingMode.HALF_UP) }
                p.contributionMargin.signum() > 0 -> (fc * n).divide(p.contributionMargin * s, 3, RoundingMode.HALF_UP)
                else -> null
            }
            ProductBep(
                product = p,
                variableCostPerUnit = p.variableCostPerUnit.setScale(0, RoundingMode.HALF_UP).toLong(),
                contributionMargin = p.contributionMargin.setScale(0, RoundingMode.HALF_UP).toLong(),
                mixShare = share,
                breakEvenUnits = units,
                // Exact: FC × target × s × P / Σ(target × s × CM), not the rounded unit count times price.
                breakEvenRupiah = when {
                    s.signum() == 0 -> null
                    mixDefined && mixMargin != null && mixMargin.signum() > 0 ->
                        (fc * p.targetPerMonth!! * s * BigDecimal.valueOf(p.priceRupiah)).divide(mixMargin, 0, RoundingMode.HALF_UP).toLong()
                    !mixDefined && p.contributionMargin.signum() > 0 ->
                        fc.multiply(BigDecimal.valueOf(p.priceRupiah)).divide(p.contributionMargin, 0, RoundingMode.HALF_UP).toLong()
                    else -> null
                },
                sellsAtALoss = p.contributionMargin.signum() <= 0,
            )
        }
        val bepRupiah = if (bepUnits != null) productBeps.sumOf { it.breakEvenRupiah ?: 0 } else null

        // Per plan month on average, so twelve times the monthly profit is the period's profit.
        fun perMonth(value: (PlanProduct) -> BigDecimal) = money(
            inputs.products.fold(BigDecimal.ZERO) { sum, p -> sum + (p.targetPerMonth ?: BigDecimal.ZERO) * sellingMonths(p) * value(p) }
                .divide(n, MONEY_SCALE, RoundingMode.HALF_UP),
        )
        val productRevenue = perMonth { BigDecimal.valueOf(it.priceRupiah) }
        val productVariable = perMonth { it.variableCostPerUnit }
        val revenue = productRevenue + otherRevenue
        val variable = productVariable + otherVariable
        val totalCost = fixed.totalRupiah + variable
        val profit = revenue - totalCost

        val schedule = schedule(months, inputs)
        var beforeCapital = 0L
        val capitalNeed = schedule.maxOfOrNull { month ->
            beforeCapital += month.operatingRupiah + month.investingRupiah
            -beforeCapital
        }?.coerceAtLeast(0) ?: 0L
        val shortfall = schedule.minOfOrNull { it.cumulativeRupiah }?.let { if (it < 0) -it else 0L } ?: 0L
        return PlanAnalysis(
            months = months,
            fixedCost = fixed,
            products = productBeps,
            mixDefined = mixDefined,
            weightedContributionMargin = weightedCm,
            breakEvenUnits = bepUnits,
            breakEvenRupiah = bepRupiah,
            revenueRupiah = revenue,
            variableCostRupiah = variable,
            totalCostRupiah = totalCost,
            profitRupiah = profit,
            revenueCostRatio = ratio(revenue, totalCost),
            benefitCostRatio = ratio(profit, totalCost),
            breakEvenBaseRupiah = base,
            schedule = schedule,
            capitalNeedRupiah = capitalNeed,
            plannedCapitalRupiah = schedule.sumOf { it.financingRupiah },
            capitalShortfallRupiah = shortfall,
            summary = summary(months, inputs, schedule),
        )
    }

    /**
     * Month by month cash of the plan: products at target and recurring costs every month, dated
     * lines in their month (undated ones in the first month). Depreciation is not cash and is left
     * out here; the asset purchase itself is the investing outflow.
     */
    fun schedule(months: List<YearMonth>, inputs: PlanInputs): List<MonthlyCashFlow> {
        var cumulative = 0L
        return months.map { month ->
            val t = monthTotals(month, months, inputs)
            val operating = t.income - t.expense
            val net = operating + t.investing + t.financing
            cumulative += net
            MonthlyCashFlow(month, operating, t.investing, t.financing, net, cumulative)
        }
    }

    private class MonthTotals(val income: Long, val expense: Long, val investing: Long, val financing: Long)

    private fun monthTotals(month: YearMonth, months: List<YearMonth>, inputs: PlanInputs): MonthTotals {
        val first = months.first()
        fun lines(role: CategoryRole) = inputs.lines.filter { it.role == role }
            .sumOf { line -> if (line.recurrence == Recurrence.MONTHLY || (line.month ?: first) == month) line.amountRupiah else 0L }
        // A seasonal product (harvest in month 3) sells nothing before its start month, so the
        // cash schedule, and with it the capital need, sees the months of waiting.
        val selling = inputs.products.filter { months.indexOf(month) + 1 >= it.sellFromMonth }
        val sales = selling.sumOf { p -> money((p.targetPerMonth ?: BigDecimal.ZERO) * BigDecimal.valueOf(p.priceRupiah)) }
        val variable = selling.sumOf { p -> money((p.targetPerMonth ?: BigDecimal.ZERO) * p.variableCostPerUnit) }
        return MonthTotals(
            income = sales + lines(CategoryRole.REVENUE),
            expense = variable + lines(CategoryRole.EXPENSE),
            investing = -lines(CategoryRole.ASSET),
            financing = lines(CategoryRole.CAPITAL_IN) - lines(CategoryRole.DRAWING),
        )
    }

    /** Whole-period cash summary in the shape a posted book produces (depreciation is not cash). */
    private fun summary(months: List<YearMonth>, inputs: PlanInputs, schedule: List<MonthlyCashFlow>): FinancialSummary {
        val totals = months.map { monthTotals(it, months, inputs) }
        val income = totals.sumOf { it.income }
        val expense = totals.sumOf { it.expense }
        return FinancialSummary(
            operatingIncomeRupiah = income,
            operatingExpenseRupiah = expense,
            profitRupiah = income - expense,
            operatingCashFlowRupiah = income - expense,
            financingCashFlowRupiah = totals.sumOf { it.financing },
            cashBalanceRupiah = schedule.lastOrNull()?.cumulativeRupiah ?: 0,
            investingCashFlowRupiah = totals.sumOf { it.investing },
        )
    }

    private fun monthly(line: PlanLine, spread: (Long) -> Long) =
        if (line.recurrence == Recurrence.MONTHLY) line.amountRupiah else spread(line.amountRupiah)

    internal fun money(value: BigDecimal): Long = value.setScale(0, RoundingMode.HALF_UP).toLong()

    internal fun ratio(numerator: Long, denominator: Long): BigDecimal? =
        if (denominator > 0) BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP) else null
}

/** One month of actual sales and costs, assembled from the Realisasi book (DEC-014 B-6, B-7). */
data class ActualMonthInputs(
    val month: YearMonth,
    /** Units sold per product id, from sales entries that named a product. */
    val unitsSold: Map<String, BigDecimal>,
    val revenueRupiah: Long,
    /** Revenue from sales entries that named no product; split by the planned mix. */
    val unattributedRevenueRupiah: Long,
    val fixedCostRupiah: Long,
    /** Spend in categories counted in HPP (F2-8), cash or on credit; compared with the recipe, not counted as cost. */
    val variableSpendRupiah: Long,
    val depreciationRupiah: Long,
    /** HPP of what was sold this month: what profit counts instead of [variableSpendRupiah] (profit when sold). */
    val hppRupiah: Long = 0,
    /** Variable expenses outside HPP categories (transport): a cost, but not part of the fixed cost for BEP. */
    val otherVariableCostRupiah: Long = 0,
)

data class ActualMonthAnalysis(
    val month: YearMonth,
    val fixedCostRupiah: Long,
    val revenueRupiah: Long,
    val breakEvenRupiah: Long?,
    val reached: Boolean?,
    val unitsSold: Map<String, BigDecimal>,
    val standardVariableCostRupiah: Long,
    /** Actual variable spend minus recipe cost of what was sold; positive means over the recipe. */
    val variableVarianceRupiah: Long,
    val totalCostRupiah: Long,
    val profitRupiah: Long,
    val revenueCostRatio: BigDecimal?,
    val benefitCostRatio: BigDecimal?,
)

object ActualBepCalculator {
    fun analyse(products: List<PlanProduct>, actual: ActualMonthInputs): ActualMonthAnalysis {
        val byId = products.associateBy { it.id }
        val fc = actual.fixedCostRupiah + actual.depreciationRupiah
        // Units for unnamed revenue: spread by the planned mix at planned prices.
        val planned = products.filter { (it.targetPerMonth?.signum() ?: 0) > 0 }
        val plannedRevenue = planned.fold(BigDecimal.ZERO) { s, p -> s + p.targetPerMonth!! * BigDecimal.valueOf(p.priceRupiah) }
        val inferred = if (actual.unattributedRevenueRupiah > 0 && plannedRevenue.signum() > 0) {
            planned.associate { p ->
                p.id to BigDecimal.valueOf(actual.unattributedRevenueRupiah).multiply(p.targetPerMonth!!).divide(plannedRevenue, 3, RoundingMode.HALF_UP)
            }
        } else emptyMap()
        val units = (actual.unitsSold.keys + inferred.keys).associateWith { (actual.unitsSold[it] ?: BigDecimal.ZERO) + (inferred[it] ?: BigDecimal.ZERO) }
            .filterKeys { it in byId }
        val totalUnits = units.values.fold(BigDecimal.ZERO, BigDecimal::add)
        val standardVc = units.entries.sumOf { (id, q) -> PlanCalculator.money(q * byId.getValue(id).variableCostPerUnit) }

        // BEP in rupiah at the actual mix: FC / weighted contribution-margin ratio.
        val unitRevenue = units.entries.fold(BigDecimal.ZERO) { s, (id, q) -> s + q * BigDecimal.valueOf(byId.getValue(id).priceRupiah) }
        val unitCm = units.entries.fold(BigDecimal.ZERO) { s, (id, q) -> s + q * byId.getValue(id).contributionMargin }
        // The base is fixed cost plus variable spend outside any recipe (transport), as in the plan
        // (plan_bep_base_note), so "impas" means the profit row is zero (Task 4 review I-2).
        val bepBase = fc + actual.otherVariableCostRupiah
        val bep = if (totalUnits.signum() > 0 && unitCm.signum() > 0) {
            BigDecimal.valueOf(bepBase).multiply(unitRevenue).divide(unitCm, 0, RoundingMode.HALF_UP).toLong()
        } else null

        val totalCost = fc + actual.hppRupiah + actual.otherVariableCostRupiah
        val profit = actual.revenueRupiah - totalCost
        return ActualMonthAnalysis(
            month = actual.month,
            fixedCostRupiah = fc,
            revenueRupiah = actual.revenueRupiah,
            breakEvenRupiah = bep,
            reached = bep?.let { actual.revenueRupiah >= it },
            unitsSold = units,
            standardVariableCostRupiah = standardVc,
            variableVarianceRupiah = actual.variableSpendRupiah - standardVc,
            totalCostRupiah = totalCost,
            profitRupiah = profit,
            revenueCostRatio = PlanCalculator.ratio(actual.revenueRupiah, totalCost),
            benefitCostRatio = PlanCalculator.ratio(profit, totalCost),
        )
    }
}

/** Komparasi per month, plan against Realisasi; see [PlanComparison.perMonth]. */
data class ComparisonFigures(
    val planIncome: Long,
    val planExpense: Long,
    val planProfit: Long,
    val actualIncome: Long,
    val actualExpense: Long,
    val actualProfit: Long,
)

/**
 * Plan against actual as averages per month, both on the cash basis before depreciation the plan
 * uses (Task 4 review I-1). Realisasi is the operating money in and out of [MonthlySummary] summed over
 * its months, so the screen calls it "Uang masuk/keluar vs rencana", never profit (U2). F4 redesigns it.
 */
object PlanComparison {
    fun perMonth(plan: FinancialSummary, planMonths: Int, actualInRupiah: Long, actualOutRupiah: Long, actualMonths: Int): ComparisonFigures {
        fun avg(total: Long, months: Int) = BigDecimal.valueOf(total).divide(BigDecimal.valueOf(months.coerceAtLeast(1).toLong()), 0, RoundingMode.HALF_UP).toLong()
        return ComparisonFigures(
            planIncome = avg(plan.operatingIncomeRupiah, planMonths),
            planExpense = avg(plan.operatingExpenseRupiah, planMonths),
            planProfit = avg(plan.profitRupiah, planMonths),
            actualIncome = avg(actualInRupiah, actualMonths),
            actualExpense = avg(actualOutRupiah, actualMonths),
            actualProfit = avg(actualInRupiah - actualOutRupiah, actualMonths),
        )
    }
}

private const val MONEY_SCALE = 6
