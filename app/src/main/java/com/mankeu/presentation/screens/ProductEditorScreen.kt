package com.mankeu.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.mankeu.R
import com.mankeu.data.ProductDraft
import com.mankeu.data.ProductRecord
import com.mankeu.data.RecipeLineDraft
import com.mankeu.data.SaleUnitDraft
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.ProductSpec
import com.mankeu.domain.RecipeLine
import com.mankeu.domain.SaleUnit
import com.mankeu.domain.StockItem
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space
import com.mankeu.presentation.components.form.FullPageForm
import com.mankeu.presentation.components.form.LineEditSheet
import com.mankeu.presentation.components.form.LineRow
import com.mankeu.presentation.components.form.MoneyInput
import com.mankeu.presentation.components.form.ResultBar
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.toRupiah

/** One recipe line while typed. [materialId] null = a free cost (gas, wages); fields stay text until saved. */
private data class LineInput(val materialId: String?, val name: String = "", val quantity: String = "", val amount: String = "")

private data class UnitInput(val id: String?, val name: String = "", val content: String = "", val price: String = "")

/**
 * Rows as flat strings, so they survive rotation and process death without Parcelize (not a
 * dependency here). Ids are never blank, so "" stands for null.
 */
private fun <T> rowsSaver(width: Int, write: (T) -> List<String>, read: (List<String>) -> T) =
    listSaver<SnapshotStateList<T>, String>(
        save = { rows -> rows.flatMap(write) },
        restore = { flat -> flat.chunked(width).map(read).toMutableStateList() },
    )

private val lineSaver = rowsSaver<LineInput>(4, { listOf(it.materialId.orEmpty(), it.name, it.quantity, it.amount) }) {
    LineInput(it[0].ifEmpty { null }, it[1], it[2], it[3])
}
private val unitSaver = rowsSaver<UnitInput>(4, { listOf(it.id.orEmpty(), it.name, it.content, it.price) }) {
    UnitInput(it[0].ifEmpty { null }, it[1], it[2], it[3])
}

private fun String.toInputNumber() = replace('.', ',')

/** The base units a product is usually counted in; picked, with "Lainnya" for anything else. */
private val COMMON_BASE_UNITS = listOf("bungkus", "pcs", "porsi", "botol", "kg")

/**
 * The product editor (spec #5 #6 #13, U5 audit L08): name, one batch's recipe as compact rows
 * ("Singkong · 50 kg · Rp1.500.000") edited in a two-field sheet, what one batch yields, and the
 * sale units as rows ("dus · isi 40 · Rp300.000 · untung Rp60.000") edited in a three-field sheet.
 * HPP and profit per unit are live above Simpan. The plan fields (target, sell-from month) only
 * show when the editor is opened from Rencana usaha ([showPlan]); elsewhere they are kept as
 * stored. Final validation stays in `FinanceRepository.saveProduct`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditorScreen(
    existing: ProductRecord?,
    materials: List<StockItem>,
    onSave: (ProductDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String = "",
    onClose: () -> Unit = {},
    showPlan: Boolean = false,
    snackbarHost: SnackbarHostState? = null,
) {
    var name by rememberSaveable { mutableStateOf(existing?.product?.name.orEmpty()) }
    var baseUnit by rememberSaveable { mutableStateOf(existing?.product?.unit ?: COMMON_BASE_UNITS.first()) }
    var otherUnit by rememberSaveable { mutableStateOf(existing != null && existing.product.unit !in COMMON_BASE_UNITS) }
    var batchYield by rememberSaveable { mutableStateOf(existing?.product?.batchYieldText?.toInputNumber().orEmpty()) }
    var target by rememberSaveable { mutableStateOf(existing?.product?.targetPerMonthText?.toInputNumber().orEmpty()) }
    var sellFrom by rememberSaveable { mutableStateOf((existing?.product?.sellFromMonth ?: 1).toString()) }
    // The first unit's name copies the base unit until the owner gives it one of their own.
    var firstUnitFollows by rememberSaveable { mutableStateOf(existing == null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // The line or unit open in its sheet: index, or -1 for a new one; null = no sheet.
    var editingLine by rememberSaveable { mutableStateOf<Int?>(null) }
    var newLineIsMaterial by rememberSaveable { mutableStateOf(true) }
    var editingUnit by rememberSaveable { mutableStateOf<Int?>(null) }
    val lines = rememberSaveable(saver = lineSaver) {
        existing?.recipe.orEmpty().sortedBy { it.sortOrder }.map {
            if (it.stockItemId != null) {
                LineInput(it.stockItemId, it.name, it.quantityText?.toInputNumber().orEmpty())
            } else {
                LineInput(null, it.name, amount = it.amountRupiah?.toString().orEmpty())
            }
        }.toMutableStateList()
    }
    val units = rememberSaveable(saver = unitSaver) {
        (existing?.units.orEmpty().sortedBy { it.sortOrder }.map { UnitInput(it.id, it.name, it.contentText.toInputNumber(), it.priceRupiah.toString()) }
            .ifEmpty { listOf(UnitInput(null, name = baseUnit, content = "1")) }).toMutableStateList()
    }
    val snapshot = listOf(name, baseUnit, batchYield, target, sellFrom, lines.toList(), units.toList()).toString()
    val initial = rememberSaveable { snapshot }

    val semantic = LocalMankeuSemanticColors.current
    val unitLabel = baseUnit.trim().ifBlank { stringResource(R.string.editor_unit_fallback) }
    val materialName = { line: LineInput -> materials.firstOrNull { it.id == line.materialId }?.name ?: line.name }
    val prices = materials.associate { it.id to it.basePriceRupiah }

    // Only numbers that parse to > 0 reach the costing: a yield of 0 would divide by zero, and a
    // half-typed line is a line without cost, not a crash.
    val yieldQuantity = TransactionCalculation.parseQuantity(batchYield)
    val recipe = lines.mapNotNull { line ->
        if (line.materialId != null) {
            TransactionCalculation.parseQuantity(line.quantity)?.let { RecipeLine(materialName(line), line.materialId, it) }
        } else {
            line.amount.toLongOrNull()?.takeIf { it > 0 }?.let { RecipeLine(line.name.trim(), amountRupiah = it) }
        }
    }
    val validUnits = units.withIndex().mapNotNull { (index, u) ->
        TransactionCalculation.parseQuantity(u.content)?.let { index to SaleUnit(u.id.orEmpty(), u.name, it, u.price.toLongOrNull() ?: 0) }
    }
    val existingLoss = existing?.product?.batchLossText?.replace(',', '.')?.toBigDecimal() ?: java.math.BigDecimal.ZERO
    val cost = yieldQuantity?.takeIf { it > existingLoss }?.let { ProductCosting.cost(ProductSpec("", name, baseUnit, it, validUnits.map { v -> v.second }, recipe, batchLoss = existingLoss), prices) }
    val roundedRows = recipe.mapNotNull { ProductCosting.lineCost(it, prices) }
        .fold(java.math.BigDecimal.ZERO) { sum, value -> sum + java.math.BigDecimal.valueOf(value) }
    val roundingDifference = cost?.hasRecipe == true && roundedRows.compareTo(java.math.BigDecimal.valueOf(cost.batchCostRupiah)) != 0
    val unitCosts = cost?.let { c -> validUnits.map { it.first }.zip(c.perSaleUnit).toMap() }.orEmpty()
    val saveBlockedReason = when {
        name.isBlank() -> stringResource(R.string.form_need_name)
        baseUnit.isBlank() -> stringResource(R.string.form_need_unit)
        yieldQuantity == null -> stringResource(R.string.form_need_yield)
        yieldQuantity <= existingLoss -> stringResource(R.string.form_yield_above_loss)
        units.none { it.name.isNotBlank() } -> stringResource(R.string.form_need_sale_unit)
        units.none { it.name.isNotBlank() && (it.price.toLongOrNull() ?: 0) > 0 } -> stringResource(R.string.form_need_sale_price)
        else -> null
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.scope_remove_title, name.ifBlank { existing?.product?.name.orEmpty() })) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = semantic.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    FullPageForm(
        title = title,
        dirty = snapshot != initial,
        onClose = onClose,
        modifier = modifier,
        snackbarHost = snackbarHost,
        saveBlockedReason = saveBlockedReason,
        onSave = {
            onSave(
                ProductDraft(
                    name = name.trim(),
                    baseUnit = baseUnit.trim(),
                    batchYieldText = batchYield,
                    units = units.filter { it.name.isNotBlank() }.map { SaleUnitDraft(it.id, it.name.trim(), it.content, it.price.toLongOrNull() ?: 0) },
                    recipe = lines.map {
                        if (it.materialId != null) RecipeLineDraft(materialName(it), it.materialId, quantityText = it.quantity)
                        else RecipeLineDraft(it.name.trim(), amountRupiah = it.amount.toLongOrNull())
                    },
                    targetPerMonthText = target.ifBlank { null },
                    sellFromMonth = sellFrom.toIntOrNull() ?: 1,
                ),
            )
        },
        result = cost?.takeIf { it.hasRecipe }?.let { c ->
            {
                ResultBar(
                    text = stringResource(R.string.editor_result_hpp, c.hppPerBaseUnitRupiah.toRupiah(), unitLabel),
                    detail = stringResource(R.string.editor_result_batch, c.batchCostRupiah.toRupiah()),
                )
            }
        },
    ) {
        OutlinedTextField(
            value = name, onValueChange = { name = it }, singleLine = true,
            label = { Text(stringResource(R.string.editor_name)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        // What goes into one batch.
        SectionLabel(stringResource(R.string.editor_recipe))
        Column {
            lines.forEachIndexed { index, line ->
                val lineCost = if (line.materialId != null) {
                    TransactionCalculation.parseQuantity(line.quantity)?.let { q -> ProductCosting.lineCost(RecipeLine(line.name, line.materialId, q), prices) }
                } else {
                    line.amount.toLongOrNull()
                }
                LineRow(
                    title = materialName(line).ifBlank { "—" },
                    detail = line.materialId?.let { id -> "${line.quantity} ${materials.firstOrNull { it.id == id }?.unit.orEmpty()}".trim() },
                    amount = lineCost?.toRupiah() ?: stringResource(R.string.editor_no_price),
                    onClick = { editingLine = index },
                )
                HorizontalDivider()
            }
        }
        if (roundingDifference) {
            Text(stringResource(R.string.editor_rounding_explanation), style = MankeuType.Caption)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
            if (materials.isNotEmpty()) {
                TextButton(onClick = { newLineIsMaterial = true; editingLine = -1 }) { Text(stringResource(R.string.editor_add_material_row)) }
            }
            TextButton(onClick = { newLineIsMaterial = false; editingLine = -1 }) { Text(stringResource(R.string.editor_add_cost_row)) }
        }
        if (cost != null && !cost.complete && cost.hasRecipe) {
            Text(stringResource(R.string.product_hpp_incomplete, cost.unpricedLines.joinToString(", ")), style = MankeuType.Caption, color = semantic.danger)
        }

        // What one batch makes.
        OutlinedTextField(
            value = batchYield,
            onValueChange = { batchYield = sanitizeDecimal(it) },
            label = { Text(stringResource(R.string.editor_yield)) },
            suffix = { Text(unitLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
            val setBase = { unit: String ->
                baseUnit = unit
                if (firstUnitFollows && units.isNotEmpty()) units[0] = units[0].copy(name = unit.trim())
            }
            COMMON_BASE_UNITS.forEach { option ->
                FilterChip(selected = !otherUnit && baseUnit == option, onClick = { otherUnit = false; setBase(option) }, label = { Text(option) })
            }
            FilterChip(selected = otherUnit, onClick = { otherUnit = true }, label = { Text(stringResource(R.string.stock_item_unit_other)) })
        }
        if (otherUnit) {
            OutlinedTextField(
                value = baseUnit,
                onValueChange = {
                    baseUnit = it
                    if (firstUnitFollows && units.isNotEmpty()) units[0] = units[0].copy(name = it.trim())
                },
                singleLine = true,
                label = { Text(stringResource(R.string.editor_base_unit)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // What it is sold in.
        SectionLabel(stringResource(R.string.editor_units))
        Column {
            units.forEachIndexed { index, unit ->
                val uc = unitCosts[index]?.takeIf { unit.price.isNotBlank() }
                LineRow(
                    title = unit.name.ifBlank { "—" },
                    detail = listOfNotNull(
                        stringResource(R.string.editor_unit_content_short, unit.content, unitLabel).takeIf { unit.content != "1" },
                        uc?.let { stringResource(if (it.profitRupiah < 0) R.string.sale_result_loss else R.string.sale_result_profit, kotlin.math.abs(it.profitRupiah).toRupiah()) },
                    ).joinToString(" · ").ifBlank { null },
                    amount = unit.price.toLongOrNull()?.toRupiah() ?: stringResource(R.string.editor_price_missing),
                    onClick = { editingUnit = index },
                )
                HorizontalDivider()
            }
        }
        TextButton(onClick = { editingUnit = -1 }) { Text(stringResource(R.string.editor_add_unit_row)) }

        if (showPlan) {
            SectionLabel(stringResource(R.string.editor_plan_section))
            val mainUnit = units.firstOrNull()?.name?.trim()?.ifBlank { null } ?: unitLabel
            OutlinedTextField(
                value = target, onValueChange = { target = sanitizeDecimal(it) }, singleLine = true,
                label = { Text(stringResource(R.string.editor_target, mainUnit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sellFrom, onValueChange = { sellFrom = it.filter(Char::isDigit).take(3) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.plan_product_sell_from)) },
                supportingText = { Text(stringResource(R.string.plan_product_sell_from_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (onDelete != null) {
            TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.editor_delete), color = semantic.danger) }
        }
    }

    editingLine?.let { index ->
        val line = lines.getOrNull(index)
            ?: if (newLineIsMaterial) {
                // A new material line starts on the first material the recipe does not use yet.
                val unused = materials.firstOrNull { m -> lines.none { it.materialId == m.id } } ?: materials.first()
                LineInput(unused.id, unused.name)
            } else {
                LineInput(null)
            }
        RecipeLineSheet(
            line = line,
            materials = materials,
            onDismiss = { editingLine = null },
            onSave = { if (index in lines.indices) lines[index] = it else lines.add(it); editingLine = null },
            onDelete = if (index in lines.indices) ({ lines.removeAt(index); editingLine = null }) else null,
        )
    }
    editingUnit?.let { index ->
        val unit = units.getOrNull(index) ?: UnitInput(null)
        SaleUnitSheet(
            unit = unit,
            baseUnit = unitLabel,
            onDismiss = { editingUnit = null },
            onSave = {
                if (index in units.indices) units[index] = it else units.add(it)
                if (index == 0) firstUnitFollows = false
                editingUnit = null
            },
            onDelete = if (index in units.indices && units.size > 1) ({ units.removeAt(index); if (index == 0) firstUnitFollows = false; editingUnit = null }) else null,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** One recipe line: a material (chips) and its quantity per batch, or a free cost (name and amount). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeLineSheet(line: LineInput, materials: List<StockItem>, onDismiss: () -> Unit, onSave: (LineInput) -> Unit, onDelete: (() -> Unit)?) {
    var draft by remember(line) { mutableStateOf(line) }
    val material = materials.firstOrNull { it.id == draft.materialId }
    LineEditSheet(
        title = stringResource(if (line.materialId != null) R.string.editor_material else R.string.editor_cost_title),
        onDismiss = onDismiss,
        onSave = { onSave(draft) },
        saveBlockedReason = when {
            draft.materialId != null && TransactionCalculation.parseQuantity(draft.quantity) == null -> stringResource(R.string.form_need_quantity)
            draft.materialId == null && draft.name.isBlank() -> stringResource(R.string.form_need_name)
            draft.materialId == null && (draft.amount.toLongOrNull() ?: 0) <= 0 -> stringResource(R.string.form_need_amount)
            else -> null
        },
        onDelete = onDelete,
    ) {
        if (draft.materialId != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                materials.forEach { m ->
                    FilterChip(selected = draft.materialId == m.id, onClick = { draft = draft.copy(materialId = m.id, name = m.name) }, label = { Text(m.name) })
                }
            }
            OutlinedTextField(
                value = draft.quantity,
                onValueChange = { draft = draft.copy(quantity = sanitizeDecimal(it)) },
                label = { Text(stringResource(R.string.editor_quantity_per_batch, material?.unit ?: stringResource(R.string.editor_unit_fallback))) },
                suffix = { Text(material?.unit.orEmpty()) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = draft.name, onValueChange = { draft = draft.copy(name = it) }, singleLine = true,
                label = { Text(stringResource(R.string.editor_cost_name)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            MoneyInput(
                label = stringResource(R.string.editor_cost_amount),
                value = draft.amount.toLongOrNull(),
                onValueChange = { draft = draft.copy(amount = it?.toString().orEmpty()) },
                imeAction = ImeAction.Done,
            )
        }
    }
}

/** One sale unit: its name, how many base units it holds, and its price. */
@Composable
private fun SaleUnitSheet(unit: UnitInput, baseUnit: String, onDismiss: () -> Unit, onSave: (UnitInput) -> Unit, onDelete: (() -> Unit)?) {
    var draft by remember(unit) { mutableStateOf(unit.copy(content = unit.content.ifBlank { "1" })) }
    LineEditSheet(
        title = stringResource(R.string.editor_unit_title),
        onDismiss = onDismiss,
        onSave = { onSave(draft) },
        saveBlockedReason = when {
            draft.name.isBlank() -> stringResource(R.string.form_need_name)
            TransactionCalculation.parseQuantity(draft.content) == null -> stringResource(R.string.form_need_content)
            (draft.price.toLongOrNull() ?: 0) <= 0 -> stringResource(R.string.form_need_price)
            else -> null
        },
        onDelete = onDelete,
    ) {
        OutlinedTextField(
            value = draft.name, onValueChange = { draft = draft.copy(name = it) }, singleLine = true,
            label = { Text(stringResource(R.string.editor_unit_name)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.content, onValueChange = { draft = draft.copy(content = sanitizeDecimal(it)) }, singleLine = true,
            label = { Text(stringResource(R.string.editor_unit_content)) },
            suffix = { Text(baseUnit) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        MoneyInput(
            label = stringResource(R.string.editor_unit_price),
            value = draft.price.toLongOrNull(),
            onValueChange = { draft = draft.copy(price = it?.toString().orEmpty()) },
            imeAction = ImeAction.Done,
        )
    }
}
