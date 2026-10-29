package com.mankeu.presentation.screens

import com.mankeu.presentation.guide.HelpArticle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.BudgetItemEntity
import com.mankeu.data.CategoryEntity
import com.mankeu.data.PlanAssembly
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.CostNature
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.Recurrence
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.ChoiceMenu
import com.mankeu.presentation.appLocale
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.components.PlanFeasibilityCard
import com.mankeu.presentation.components.ReportCard
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.toRupiah
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * "Rencana Usaha": the Proyeksi book as one page (DEC-014 §6). Products with recipes, monthly
 * fixed costs, other one-off costs and income, assets and capital are entered here; break-even,
 * profit and capital need are derived and shown on top. Nothing on this page is a transaction.
 */

/** Which kind of plan line a section holds; decides the categories offered and the fields shown. */
private enum class LineSection(val titleRes: Int, val hintRes: Int, val roles: Set<CategoryRole>) {
    FIXED(R.string.plan_section_fixed, R.string.plan_section_fixed_hint, setOf(CategoryRole.EXPENSE)),
    OTHER(R.string.plan_section_other, R.string.plan_section_other_hint, setOf(CategoryRole.EXPENSE, CategoryRole.REVENUE, CategoryRole.DRAWING)),
    ASSET(R.string.plan_section_asset, R.string.plan_section_asset_hint, setOf(CategoryRole.ASSET)),
    CAPITAL(R.string.plan_section_capital, R.string.plan_section_capital_hint, setOf(CategoryRole.CAPITAL_IN)),
    ;

    companion object {
        fun of(item: BudgetItemEntity, categories: Map<String, CategoryEntity>): LineSection {
            val role = CategoryRole.parse(categories[item.categoryId]?.role)
            return when {
                role == CategoryRole.ASSET -> ASSET
                role == CategoryRole.CAPITAL_IN -> CAPITAL
                role == CategoryRole.EXPENSE && item.recurrence == Recurrence.MONTHLY.name -> FIXED
                else -> OTHER
            }
        }
    }
}

private sealed interface PlanEditor {
    data class Line(val section: LineSection, val existing: BudgetItemEntity?) : PlanEditor
    data object Period : PlanEditor
}

@Composable
fun PlanScreen(
    state: AppUiState,
    /** Opens the full-page product editor; null = a new product. */
    onOpenProduct: (String?) -> Unit,
    onSavePlanLine: (String?, String, String, Long, Recurrence, CostNature?, YearMonth?, Int?) -> Unit,
    onArchivePlanLine: (String) -> Unit,
    onSetPeriod: (YearMonth, YearMonth) -> Unit,
    onManageScope: () -> Unit,
) {
    var editor by remember { mutableStateOf<PlanEditor?>(null) }
    val categories = remember(state.categories) { state.categories.associateBy { it.id } }
    val project = state.projects.firstOrNull { it.id == state.selectedProjectId }
    val months = state.plan?.months ?: PlanAssembly.months(project, state.planItems, LocalDate.now())
    val materialPrices = state.stockItems.associate { it.id to it.basePriceRupiah }
    val lines = state.planItems.filterNot { it.archived }.groupBy { LineSection.of(it, categories) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle(stringResource(R.string.plan_title), stringResource(R.string.plan_subtitle)) }
        if (project == null) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_project_title),
                    stringResource(R.string.setup_project_body),
                    stringResource(R.string.scope_add_project),
                    onManageScope,
                    learn = HelpArticle.START_SCOPE,
                )
            }
            return@LazyColumn
        }
        item {
            val locale = appLocale()
            val format = DateTimeFormatter.ofPattern("MMM yyyy", locale)
            ReportCard(stringResource(R.string.plan_period)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.plan_period_value, months.first().format(format), months.last().format(format), months.size),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { editor = PlanEditor.Period }) { Text(stringResource(R.string.action_edit)) }
                }
            }
        }
        state.plan?.let { plan -> item { PlanFeasibilityCard(plan) } }

        item {
            PlanSectionCard(
                title = stringResource(R.string.plan_section_products),
                hint = stringResource(R.string.plan_section_products_hint),
                onAdd = { onOpenProduct(null) },
            ) {
                state.products.forEach { row ->
                    val cost = ProductCosting.cost(row.spec, materialPrices)
                    val economics = stringResource(
                        R.string.plan_product_row,
                        (row.mainUnit?.priceRupiah ?: 0).toRupiah(),
                        row.mainUnit?.name ?: row.product.unit,
                        (cost.perSaleUnit.firstOrNull()?.hppRupiah ?: 0L).toRupiah(),
                        row.product.targetPerMonthText ?: "-",
                    )
                    // An HPP missing a price or a recipe is a floor, not the HPP: say so (Task 2 review M-3).
                    val parts = listOfNotNull(
                        economics,
                        stringResource(R.string.plan_product_sell_from_value, row.product.sellFromMonth).takeIf { row.product.sellFromMonth > 1 },
                        // hppLines has one line per sale unit when complete; only an incomplete HPP asks for its single warning line.
                        if (!cost.complete) hppLines(cost).first() else null,
                    )
                    PlanRow(title = row.product.name, subtitle = parts.joinToString(" · "), onClick = { onOpenProduct(row.product.id) })
                }
            }
        }
        LineSection.entries.forEach { section ->
            item {
                PlanSectionCard(
                    title = stringResource(section.titleRes),
                    hint = stringResource(section.hintRes),
                    onAdd = { editor = PlanEditor.Line(section, null) },
                ) {
                    lines[section].orEmpty().forEach { line ->
                        PlanRow(
                            title = line.name,
                            subtitle = lineSubtitle(line, categories[line.categoryId]),
                            onClick = { editor = PlanEditor.Line(section, line) },
                        )
                    }
                }
            }
        }
    }

    when (val current = editor) {
        null -> Unit
        is PlanEditor.Line -> PlanLineSheet(
            section = current.section,
            existing = current.existing,
            categories = state.categories.filter { it.active && CategoryRole.parse(it.role) in current.section.roles },
            onDismiss = { editor = null },
            onSave = { categoryId, name, amount, recurrence, nature, month, life ->
                onSavePlanLine(current.existing?.id, categoryId, name, amount, recurrence, nature, month, life)
                editor = null
            },
            onDelete = current.existing?.let { { onArchivePlanLine(it.id); editor = null } },
        )
        PlanEditor.Period -> PeriodSheet(
            start = months.first(),
            end = months.last(),
            onDismiss = { editor = null },
            onSave = { start, end -> onSetPeriod(start, end); editor = null },
        )
    }
}

@Composable
private fun lineSubtitle(line: BudgetItemEntity, category: CategoryEntity?): String {
    val amount = line.plannedTotalRupiah.toRupiah()
    val month = line.plannedDateEpochDay?.let {
        YearMonth.from(LocalDate.ofEpochDay(it)).format(DateTimeFormatter.ofPattern("MMM yyyy", appLocale()))
    }
    val parts = listOfNotNull(
        category?.name,
        if (line.recurrence == Recurrence.MONTHLY.name) stringResource(R.string.plan_per_month, amount) else amount,
        month,
        line.usefulLifeMonths?.let { stringResource(R.string.plan_life_value, it) },
        when (line.costNature) {
            CostNature.FIXED.name -> stringResource(R.string.cost_nature_fixed)
            CostNature.VARIABLE.name -> stringResource(R.string.cost_nature_variable)
            else -> null
        }.takeIf { line.recurrence != Recurrence.MONTHLY.name },
    )
    return parts.joinToString(" · ")
}

@Composable
private fun PlanSectionCard(title: String, hint: String, onAdd: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.action_add))
                }
            }
            content()
        }
    }
}

@Composable
private fun PlanRow(title: String, subtitle: String, onClick: () -> Unit) {
    HorizontalDivider()
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlanLineSheet(
    section: LineSection,
    existing: BudgetItemEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Recurrence, CostNature?, YearMonth?, Int?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    // A fixed-cost line starts on a fixed-nature category, not on whichever expense sorts first
    // (that was "Bahan Baku", a variable cost, found by hand on the emulator).
    var categoryId by rememberSaveable {
        mutableStateOf(
            existing?.categoryId
                ?: categories.firstOrNull { section == LineSection.FIXED && it.costNature == CostNature.FIXED.name }?.id
                ?: categories.firstOrNull()?.id,
        )
    }
    var amount by rememberSaveable { mutableStateOf(existing?.plannedTotalRupiah?.toString().orEmpty()) }
    var monthly by rememberSaveable { mutableStateOf(existing?.recurrence == Recurrence.MONTHLY.name) }
    var nature by rememberSaveable { mutableStateOf(existing?.costNature) }
    var date by rememberSaveable { mutableStateOf(existing?.plannedDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() }.orEmpty()) }
    var life by rememberSaveable {
        mutableStateOf((existing?.usefulLifeMonths ?: PlanAssembly.DEFAULT_ASSET_LIFE_MONTHS.takeIf { section == LineSection.ASSET })?.toString().orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }
    val errLine = stringResource(R.string.plan_err_line)
    val category = categories.firstOrNull { it.id == categoryId }
    val isExpense = CategoryRole.parse(category?.role) == CategoryRole.EXPENSE

    MankeuSheet(
        title = stringResource(section.titleRes),
        onDismiss = onDismiss,
        onConfirm = {
            val parsedAmount = amount.toLongOrNull()
            val parsedLife = life.toIntOrNull()
            error = when {
                name.isBlank() || categoryId == null || parsedAmount == null || parsedAmount <= 0 -> errLine
                section == LineSection.ASSET && (parsedLife == null || parsedLife <= 0) -> errLine
                else -> null
            }
            if (error == null) {
                val recurrence = when (section) {
                    LineSection.FIXED -> Recurrence.MONTHLY
                    LineSection.OTHER -> if (monthly) Recurrence.MONTHLY else Recurrence.ONCE
                    else -> Recurrence.ONCE
                }
                onSave(
                    categoryId!!,
                    name.trim(),
                    parsedAmount!!,
                    recurrence,
                    when {
                        section == LineSection.FIXED -> CostNature.FIXED
                        isExpense -> CostNature.entries.firstOrNull { it.name == nature }
                        else -> null
                    },
                    date.takeIf { it.isNotBlank() && recurrence == Recurrence.ONCE }?.let { YearMonth.from(LocalDate.parse(it)) },
                    parsedLife.takeIf { section == LineSection.ASSET },
                )
            }
        },
    ) {
        Text(stringResource(section.hintRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = name, onValueChange = { name = it }, singleLine = true,
            label = { Text(stringResource(R.string.plan_line_name)) }, modifier = Modifier.fillMaxWidth(),
        )
        ChoiceMenu(
            fieldLabel = stringResource(R.string.entry_category),
            label = category?.name ?: stringResource(R.string.entry_category_pick),
            options = categories.map { it.id to it.name },
            onSelected = { categoryId = it },
        )
        MoneyField(
            stringResource(if (section == LineSection.FIXED) R.string.plan_line_amount_month else R.string.plan_line_amount),
            amount,
        ) { amount = it }
        if (section == LineSection.OTHER) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !monthly, onClick = { monthly = false }, label = { Text(stringResource(R.string.plan_once)) })
                FilterChip(selected = monthly, onClick = { monthly = true }, label = { Text(stringResource(R.string.plan_monthly)) })
            }
            if (isExpense) {
                Text(stringResource(R.string.cost_nature_label), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = nature == null, onClick = { nature = null }, label = { Text(stringResource(R.string.cost_nature_inherit)) })
                    FilterChip(selected = nature == CostNature.FIXED.name, onClick = { nature = CostNature.FIXED.name }, label = { Text(stringResource(R.string.cost_nature_fixed)) })
                    FilterChip(selected = nature == CostNature.VARIABLE.name, onClick = { nature = CostNature.VARIABLE.name }, label = { Text(stringResource(R.string.cost_nature_variable)) })
                }
            }
        }
        if (section == LineSection.ASSET) {
            OutlinedTextField(
                value = life, onValueChange = { life = it.filter(Char::isDigit) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.plan_line_life)) }, modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    val a = amount.toLongOrNull()
                    val l = life.toIntOrNull()
                    if (a != null && l != null && l > 0) Text(stringResource(R.string.plan_line_life_supporting, (a / l).toRupiah()))
                },
            )
        }
        if (section != LineSection.FIXED && !(section == LineSection.OTHER && monthly)) {
            DateField(value = date, label = stringResource(R.string.plan_line_month), clearable = true, onValueChange = { date = it })
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        onDelete?.let {
            TextButton(onClick = it) { Text(stringResource(R.string.plan_remove), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun PeriodSheet(start: YearMonth, end: YearMonth, onDismiss: () -> Unit, onSave: (YearMonth, YearMonth) -> Unit) {
    var from by rememberSaveable { mutableStateOf(start.atDay(1).toString()) }
    var to by rememberSaveable { mutableStateOf(end.atDay(1).toString()) }
    val first = YearMonth.from(LocalDate.parse(from))
    val last = YearMonth.from(LocalDate.parse(to))
    val valid = !last.isBefore(first)
    MankeuSheet(
        title = stringResource(R.string.plan_period),
        onDismiss = onDismiss,
        confirmEnabled = valid,
        onConfirm = { onSave(first, last) },
    ) {
        Text(stringResource(R.string.plan_period_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DateField(value = from, label = stringResource(R.string.plan_period_start), onValueChange = { from = it })
        DateField(value = to, label = stringResource(R.string.plan_period_end), onValueChange = { to = it })
        if (!valid) Text(stringResource(R.string.plan_period_invalid), color = MaterialTheme.colorScheme.error)
    }
}
