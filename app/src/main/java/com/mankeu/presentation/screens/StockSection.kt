package com.mankeu.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.StockCalculation
import com.mankeu.domain.StockItem
import com.mankeu.domain.StockMovementType
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.toRupiah
import java.math.BigDecimal

/** Inventory summary: value on hand, item count, low-stock warning. [onManage] null hides the button. */
@Composable
internal fun StockCard(items: List<StockItem>, onManage: (() -> Unit)?) {
    val summary = StockCalculation.summarize(items)
    val semantic = LocalMankeuSemanticColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.stock_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            if (items.isEmpty()) {
                Text(stringResource(R.string.stock_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.stock_value), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(summary.totalInventoryValueRupiah.toRupiah(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.stock_item_count, summary.totalItems))
                if (summary.lowStockCount > 0) {
                    Surface(shape = RoundedCornerShape(12.dp), color = semantic.dangerContainer) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = semantic.onDangerContainer)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.stock_low_count, summary.lowStockCount),
                                color = semantic.onDangerContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    Text(stringResource(R.string.stock_all_ok), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onManage != null) {
                FilledTonalButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stock_manage))
                }
            }
        }
    }
}

/**
 * Add and movement sheets of the material list. Kept outside the LazyColumn, whose items are
 * disposed when scrolled away, so a sheet does not close while the list moves under it.
 */
@Composable
internal fun MaterialSheets(
    items: List<StockItem>,
    /** The selected unit's name; null when none is selected, and every new item is then shared. */
    unitName: String?,
    adding: Boolean,
    movementItemId: String?,
    onCloseAdd: () -> Unit,
    onCloseMovement: () -> Unit,
    onCreateStockItem: (String, String, Long, BigDecimal, BigDecimal, Boolean) -> Unit,
    onRecordStockMovement: (String, StockMovementType, BigDecimal, String) -> Unit,
) {
    if (adding) {
        AddStockItemSheet(
            unitName = unitName,
            onDismiss = onCloseAdd,
            onSave = { name, unit, price, initial, minAlert, shared ->
                onCreateStockItem(name, unit, price, initial, minAlert, shared)
                onCloseAdd()
            },
        )
    }
    items.firstOrNull { it.id == movementItemId }?.let { item ->
        StockMovementSheet(
            item = item,
            onDismiss = onCloseMovement,
            onSave = { type, quantity, notes ->
                onRecordStockMovement(item.id, type, quantity, notes)
                onCloseMovement()
            },
        )
    }
}

/**
 * The "Bahan & barang" part of the Produk tab: inventory summary and every material with its level
 * and value. Stock belongs to the selected unit or, when shared, to every unit of the business
 * (schema 10); never to a Proyeksi/Realisasi book. The sheets are [MaterialSheets].
 */
internal fun LazyListScope.materialItems(items: List<StockItem>, onAddItem: () -> Unit, onOpenItem: (String) -> Unit) {
    item { StockCard(items = items, onManage = null) }
    item {
        Button(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.stock_add_item))
        }
    }
    if (items.isNotEmpty()) {
        item {
            Text(stringResource(R.string.stock_tap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(items, key = { it.id }) { item ->
        val semantic = LocalMankeuSemanticColors.current
        Card(
            modifier = Modifier.fillMaxWidth()
                .clickable { onOpenItem(item.id) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    val detail = stringResource(
                        R.string.stock_row_detail,
                        item.currentStock.toQuantityText(),
                        item.unit,
                        StockCalculation.computeInventoryValue(listOf(item)).toRupiah(),
                    )
                    Text(
                        if (item.shared) detail + " · " + stringResource(R.string.stock_shared_badge) else detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.isLowStock) {
                    Surface(shape = RoundedCornerShape(8.dp), color = semantic.dangerContainer) {
                        Text(
                            stringResource(R.string.stock_low_badge),
                            color = semantic.onDangerContainer,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddStockItemSheet(
    unitName: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String, priceRupiah: Long, initial: BigDecimal, minAlert: BigDecimal, shared: Boolean) -> Unit,
) {
    var shared by rememberSaveable { mutableStateOf(unitName == null) }
    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var initial by rememberSaveable { mutableStateOf("") }
    var minAlert by rememberSaveable { mutableStateOf("") }
    MankeuSheet(
        title = stringResource(R.string.stock_add_item),
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && unit.isNotBlank(),
        onConfirm = {
            onSave(name, unit, price.toLongOrNull() ?: 0, initial.toQuantity(), minAlert.toQuantity(), shared)
        },
    ) {
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.stock_item_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(unit, { unit = it }, label = { Text(stringResource(R.string.stock_item_unit)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        MoneyField(stringResource(R.string.stock_item_price), price) { price = it }
        QuantityField(stringResource(R.string.stock_item_initial), initial) { initial = it }
        QuantityField(stringResource(R.string.stock_item_min), minAlert, hint = stringResource(R.string.stock_item_min_hint)) { minAlert = it }
        Text(stringResource(R.string.stock_owner_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (unitName != null) {
                FilterChip(selected = !shared, onClick = { shared = false }, label = { Text(stringResource(R.string.stock_owner_unit, unitName)) })
            }
            FilterChip(selected = shared, onClick = { shared = true }, label = { Text(stringResource(R.string.stock_owner_shared)) })
        }
        Text(stringResource(R.string.stock_owner_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StockMovementSheet(
    item: StockItem,
    onDismiss: () -> Unit,
    onSave: (StockMovementType, BigDecimal, String) -> Unit,
) {
    var type by rememberSaveable(item.id) { mutableStateOf(StockMovementType.IN) }
    var quantity by rememberSaveable(item.id) { mutableStateOf("") }
    var notes by rememberSaveable(item.id) { mutableStateOf("") }
    val parsed = quantity.replace(',', '.').toBigDecimalOrNull()
    val valid = parsed != null && (parsed.signum() > 0 || (parsed.signum() == 0 && type == StockMovementType.ADJUSTMENT))
    MankeuSheet(
        title = stringResource(R.string.stock_movement_title, item.name),
        onDismiss = onDismiss,
        confirmEnabled = valid,
        onConfirm = { parsed?.let { onSave(type, it, notes.trim()) } },
    ) {
        Text(stringResource(R.string.stock_current, item.currentStock.toQuantityText(), item.unit), fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                StockMovementType.IN to R.string.stock_type_in,
                StockMovementType.OUT to R.string.stock_type_out,
                StockMovementType.ADJUSTMENT to R.string.stock_type_adjust,
            ).forEach { (option, label) ->
                FilterChip(selected = type == option, onClick = { type = option }, label = { Text(stringResource(label)) })
            }
        }
        QuantityField(
            stringResource(if (type == StockMovementType.ADJUSTMENT) R.string.stock_qty_adjust else R.string.stock_qty),
            quantity,
        ) { quantity = it }
        OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.stock_notes)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun QuantityField(label: String, value: String, hint: String? = null, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeDecimal(it)) },
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun String.toQuantity(): BigDecimal = TransactionCalculation.parseQuantity(this) ?: BigDecimal.ZERO

internal fun BigDecimal.toQuantityText(): String = stripTrailingZeros().toPlainString().replace('.', ',')
