package com.mankeu.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.ProductRecord
import com.mankeu.domain.ProductCost
import com.mankeu.domain.ProductCosting
import com.mankeu.domain.ProductStock
import com.mankeu.domain.StockItem
import com.mankeu.domain.StockMovementType
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.sanitizeDecimal
import com.mankeu.presentation.guide.HelpArticle
import com.mankeu.presentation.toRupiah
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The Produk tab (spec #5 #9): each product with HPP and profit per sale unit and its stock, the
 * short Produksi and Hitung stok sheets, then the materials as "Bahan & barang".
 */
@Composable
fun ProductsScreen(
    products: List<ProductRecord>,
    materials: List<StockItem>,
    hasBusiness: Boolean,
    unitName: String?,
    onManageScope: () -> Unit,
    onOpenProduct: (String?) -> Unit,
    onOpenHistory: (String) -> Unit,
    onProduce: (String, BigDecimal, LocalDate) -> Unit,
    onCountStock: (String, BigDecimal, LocalDate) -> Unit,
    onCreateStockItem: (String, String, Long, BigDecimal, BigDecimal, Boolean) -> Unit,
    onRecordStockMovement: (String, StockMovementType, BigDecimal, String) -> Unit,
    expanded: Boolean = false,
) {
    var produceId by rememberSaveable { mutableStateOf<String?>(null) }
    var countId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingMaterial by rememberSaveable { mutableStateOf(false) }
    var materialId by rememberSaveable { mutableStateOf<String?>(null) }
    val prices = materials.associate { it.id to it.basePriceRupiah }

    products.firstOrNull { it.product.id == produceId }?.let { ProductionSheet(it, onDismiss = { produceId = null }) { batches, date -> onProduce(it.product.id, batches, date); produceId = null } }
    products.firstOrNull { it.product.id == countId }?.let { CountStockSheet(it, onDismiss = { countId = null }) { counted, date -> onCountStock(it.product.id, counted, date); countId = null } }
    MaterialSheets(
        items = materials,
        unitName = unitName,
        adding = addingMaterial,
        movementItemId = materialId,
        onCloseAdd = { addingMaterial = false },
        onCloseMovement = { materialId = null },
        onCreateStockItem = onCreateStockItem,
        onRecordStockMovement = onRecordStockMovement,
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = if (expanded) 24.dp else 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!hasBusiness) {
            item {
                EmptyCard(
                    stringResource(R.string.setup_business_title),
                    stringResource(R.string.setup_business_body),
                    stringResource(R.string.scope_add_business),
                    onManageScope,
                    learn = HelpArticle.START_SCOPE,
                )
            }
            return@LazyColumn
        }
        if (products.isEmpty()) {
            item {
                EmptyCard(
                    stringResource(R.string.products_empty_title),
                    stringResource(R.string.products_empty_body),
                    stringResource(R.string.products_add),
                    { onOpenProduct(null) },
                )
            }
        } else {
            item {
                Button(onClick = { onOpenProduct(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.products_add))
                }
            }
        }
        items(products, key = { it.product.id }) { record ->
            ProductCard(
                record = record,
                cost = ProductCosting.cost(record.spec, prices),
                onOpen = { onOpenProduct(record.product.id) },
                onProduce = { produceId = record.product.id },
                onCount = { countId = record.product.id },
                onHistory = { onOpenHistory(record.product.id) },
            )
        }
        materialItems(materials, onAddItem = { addingMaterial = true }, onOpenItem = { materialId = it })
    }
}

/** "HPP Rp6.000/bungkus · untung Rp2.000" per unit, or why there is no HPP yet; shared with the plan rows. */
@Composable
internal fun hppLines(cost: ProductCost): List<String> = when {
    !cost.hasRecipe -> listOf(stringResource(R.string.product_no_recipe))
    !cost.complete -> listOf(stringResource(R.string.product_hpp_incomplete, cost.unpricedLines.joinToString(", ")))
    else -> cost.perSaleUnit.map { stringResource(R.string.product_hpp_line, it.hppRupiah.toRupiah(), it.unit.name, it.profitRupiah.toRupiah()) }
}

@Composable
private fun ProductCard(record: ProductRecord, cost: ProductCost, onOpen: () -> Unit, onProduce: () -> Unit, onCount: () -> Unit, onHistory: () -> Unit) {
    val semantic = LocalMankeuSemanticColors.current
    val spec = record.spec
    val stock = record.stock
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            hppLines(cost).forEach {
                Text(it, color = if (cost.complete) MaterialTheme.colorScheme.onSurface else semantic.danger)
            }
            val biggest = spec.saleUnits.filter { it.content > BigDecimal.ONE }.maxByOrNull { it.content }
            // "(0 dus + 0 bungkus)" says nothing: the split only shows once a whole big unit is on hand.
            val split = biggest?.let { unit -> ProductStock.split(stock, unit.content).takeIf { it.first.signum() > 0 }?.let { unit to it } }
                ?.let { (unit, parts) ->
                    " " + stringResource(R.string.product_stock_split, parts.first.toQuantityText(), unit.name, parts.second.toQuantityText(), spec.baseUnit)
                }.orEmpty()
            Text(
                stringResource(R.string.product_stock_line, stock.toQuantityText(), spec.baseUnit) + split,
                fontWeight = FontWeight.Bold,
            )
            if (stock.signum() < 0) {
                Text(stringResource(R.string.product_stock_negative_hint), color = semantic.danger, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onProduce, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.product_produce)) }
                FilledTonalButton(onClick = onCount, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.product_count_stock)) }
            }
            TextButton(onClick = onHistory) { Text(stringResource(R.string.product_stock_history)) }
        }
    }
}

@Composable
private fun ProductionSheet(record: ProductRecord, onDismiss: () -> Unit, onSave: (BigDecimal, LocalDate) -> Unit) {
    var batches by rememberSaveable(record.product.id) { mutableStateOf("1") }
    var date by rememberSaveable(record.product.id) { mutableStateOf(LocalDate.now().toString()) }
    val parsed = TransactionCalculation.parseQuantity(batches)
    MankeuSheet(
        title = stringResource(R.string.production_title, record.product.name),
        onDismiss = onDismiss,
        confirmEnabled = parsed != null,
        onConfirm = { parsed?.let { onSave(it, LocalDate.parse(date)) } },
    ) {
        QuantityInput(stringResource(R.string.production_batches), batches) { batches = it }
        parsed?.let {
            Text(
                stringResource(R.string.production_result, (it * record.spec.batchYield).toQuantityText(), record.product.unit),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DateField(value = date, label = stringResource(R.string.entry_date), onValueChange = { date = it })
    }
}

@Composable
private fun CountStockSheet(record: ProductRecord, onDismiss: () -> Unit, onSave: (BigDecimal, LocalDate) -> Unit) {
    var counted by rememberSaveable(record.product.id) { mutableStateOf(record.stock.takeIf { it.signum() >= 0 }?.toQuantityText().orEmpty()) }
    var date by rememberSaveable(record.product.id) { mutableStateOf(LocalDate.now().toString()) }
    // Zero is a real count (sold out); parseQuantity would refuse it.
    val parsed = counted.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
    MankeuSheet(
        title = stringResource(R.string.count_stock_title, record.product.name),
        onDismiss = onDismiss,
        confirmEnabled = parsed != null,
        onConfirm = { parsed?.let { onSave(it, LocalDate.parse(date)) } },
    ) {
        QuantityInput(stringResource(R.string.count_stock_field, record.product.unit), counted) { counted = it }
        DateField(value = date, label = stringResource(R.string.entry_date), onValueChange = { date = it })
    }
}

@Composable
private fun QuantityInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeDecimal(it)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
