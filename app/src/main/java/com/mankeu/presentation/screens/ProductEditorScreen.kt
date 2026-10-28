package com.mankeu.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.mankeu.presentation.ChoiceMenu
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.components.MoneyField
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

/**
 * The product editor (spec #5 #6 #13): one batch recipe, the units it is sold in, and the HPP and
 * profit of each unit while typing. A full page with Simpan fixed at the bottom, because the form
 * is long. Final validation stays in `FinanceRepository.saveProduct`; this only keeps the button
 * off until the minimum is there.
 */
@Composable
fun ProductEditorScreen(
    existing: ProductRecord?,
    materials: List<StockItem>,
    onSave: (ProductDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf(existing?.product?.name.orEmpty()) }
    var baseUnit by rememberSaveable { mutableStateOf(existing?.product?.unit.orEmpty()) }
    var batchYield by rememberSaveable { mutableStateOf(existing?.product?.batchYieldText?.toInputNumber().orEmpty()) }
    var target by rememberSaveable { mutableStateOf(existing?.product?.targetPerMonthText?.toInputNumber().orEmpty()) }
    var sellFrom by rememberSaveable { mutableStateOf((existing?.product?.sellFromMonth ?: 1).toString()) }
    // The first unit's name copies the base unit until the owner types one of their own.
    var firstUnitFollows by rememberSaveable { mutableStateOf(existing == null) }
    var planOpen by rememberSaveable { mutableStateOf(target.isNotBlank() || sellFrom != "1") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
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
            .ifEmpty { listOf(UnitInput(null, content = "1")) }).toMutableStateList()
    }

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
    val cost = yieldQuantity?.let { ProductCosting.cost(ProductSpec("", name, baseUnit, it, validUnits.map { v -> v.second }, recipe), prices) }
    val unitCosts = cost?.let { c -> validUnits.map { it.first }.zip(c.perSaleUnit).toMap() }.orEmpty()

    val canSave = name.isNotBlank() && baseUnit.isNotBlank() && yieldQuantity != null &&
        units.any { it.name.isNotBlank() && (it.price.toLongOrNull() ?: 0) > 0 }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.scope_remove_title, name.ifBlank { existing?.product?.name.orEmpty() })) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text(stringResource(R.string.editor_name)) }, modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = baseUnit,
                        onValueChange = {
                            baseUnit = it
                            if (firstUnitFollows) units[0] = units[0].copy(name = it.trim())
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.editor_base_unit)) },
                        supportingText = { Text(stringResource(R.string.editor_base_unit_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DecimalField(stringResource(R.string.editor_batch_yield, unitLabel), batchYield) { batchYield = it }
                }
            }

            item { SectionHeading(stringResource(R.string.editor_recipe)) }
            itemsIndexed(lines) { index, line ->
                RowCard(onRemove = { lines.removeAt(index) }) {
                    if (line.materialId != null) {
                        val material = materials.firstOrNull { it.id == line.materialId }
                        ChoiceMenu(
                            fieldLabel = stringResource(R.string.editor_material),
                            label = materialName(line),
                            options = materials.map { it.id to it.name },
                            onSelected = { id -> lines[index] = line.copy(materialId = id, name = materials.first { it.id == id }.name) },
                        )
                        DecimalField(stringResource(R.string.editor_quantity_per_batch, material?.unit ?: stringResource(R.string.editor_unit_fallback)), line.quantity) {
                            lines[index] = line.copy(quantity = it)
                        }
                        TransactionCalculation.parseQuantity(line.quantity)?.let { quantity ->
                            Text(
                                ProductCosting.lineCost(RecipeLine(line.name, line.materialId, quantity), prices)?.toRupiah()
                                    ?: stringResource(R.string.editor_no_price),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = line.name, onValueChange = { lines[index] = line.copy(name = it) }, singleLine = true,
                            label = { Text(stringResource(R.string.editor_cost_name)) }, modifier = Modifier.fillMaxWidth(),
                        )
                        MoneyField(stringResource(R.string.editor_cost_amount), line.amount) { lines[index] = line.copy(amount = it) }
                    }
                }
            }
            item {
                Row {
                    if (materials.isNotEmpty()) {
                        TextButton(onClick = { lines.add(LineInput(materials.first().id, materials.first().name)) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text(stringResource(R.string.editor_add_material))
                        }
                    }
                    TextButton(onClick = { lines.add(LineInput(null)) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text(stringResource(R.string.editor_add_cost))
                    }
                }
            }
            cost?.let { c ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.editor_batch_total, c.batchCostRupiah.toRupiah(), c.hppPerBaseUnitRupiah.toRupiah(), unitLabel),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (!c.complete && c.hasRecipe) {
                            Text(stringResource(R.string.product_hpp_incomplete, c.unpricedLines.joinToString(", ")), color = semantic.danger)
                        }
                    }
                }
            }

            item { SectionHeading(stringResource(R.string.editor_units)) }
            itemsIndexed(units) { index, unit ->
                RowCard(onRemove = if (units.size > 1) ({ units.removeAt(index); if (index == 0) firstUnitFollows = false }) else null) {
                    OutlinedTextField(
                        value = unit.name,
                        onValueChange = {
                            units[index] = unit.copy(name = it)
                            if (index == 0) firstUnitFollows = false
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.editor_unit_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DecimalField(stringResource(R.string.editor_unit_content, unitLabel), unit.content, Modifier.weight(1f)) {
                            units[index] = unit.copy(content = it)
                        }
                        MoneyField(stringResource(R.string.editor_unit_price), unit.price, Modifier.weight(1f)) { units[index] = unit.copy(price = it) }
                    }
                    unitCosts[index]?.takeIf { unit.price.isNotBlank() }?.let { uc ->
                        Text(
                            stringResource(R.string.editor_unit_result, uc.hppRupiah.toRupiah(), uc.profitRupiah.toRupiah()),
                            fontWeight = FontWeight.Bold,
                            color = if (uc.profitRupiah <= 0) semantic.danger else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            item {
                TextButton(onClick = { units.add(UnitInput(null)) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.editor_unit_add))
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().clickable { planOpen = !planOpen }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.editor_plan_section), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(if (planOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
            }
            if (planOpen) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val mainUnit = units.firstOrNull()?.name?.trim()?.ifBlank { null } ?: unitLabel
                        DecimalField(stringResource(R.string.editor_target, mainUnit), target) { target = it }
                        OutlinedTextField(
                            value = sellFrom, onValueChange = { sellFrom = it.filter(Char::isDigit).take(3) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(stringResource(R.string.plan_product_sell_from)) },
                            supportingText = { Text(stringResource(R.string.plan_product_sell_from_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            if (onDelete != null) {
                item {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.editor_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Button(
            onClick = {
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
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(16.dp).imePadding(),
        ) { Text(stringResource(R.string.action_save)) }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
}

@Composable
private fun RowCard(onRemove: (() -> Unit)?, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
            if (onRemove != null) {
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.editor_remove_line)) }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun DecimalField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeDecimal(it)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}
