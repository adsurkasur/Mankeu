package com.mankeu.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.ProductRecord
import com.mankeu.data.StockHistoryEntry
import com.mankeu.domain.ProductMovementType
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.toDisplayDate
import java.time.LocalDate

/**
 * Riwayat stok (cleanup P1): everything that makes up a product's stock, newest first, with the stock
 * after each line. A wrong production or count is deleted here after a confirmation and recorded
 * again; sales only show, since they are changed in Catatan. A full page, as long lists are (#13).
 */
@Composable
fun StockHistoryScreen(record: ProductRecord, onDelete: (String) -> Unit, modifier: Modifier = Modifier) {
    val unit = record.product.unit
    val history = record.history.asReversed()
    var confirmId by rememberSaveable { mutableStateOf<String?>(null) }

    history.firstOrNull { it.id == confirmId }?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmId = null },
            title = { Text(stringResource(R.string.stock_history_delete_title, headline(entry), LocalDate.ofEpochDay(entry.dateEpochDay).toDisplayDate())) },
            text = { Text(stringResource(R.string.stock_history_delete_body)) },
            confirmButton = {
                TextButton(onClick = { confirmId = null; onDelete(entry.id) }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.product_stock_line, record.stock.toQuantityText(), unit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(if (history.isEmpty()) R.string.stock_history_empty else R.string.stock_history_fix_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(history, key = { it.type.name + it.id }) { entry ->
            HistoryRow(entry, unit, onDelete = { confirmId = entry.id }.takeIf { entry.type != ProductMovementType.SALE })
        }
    }
}

@Composable
private fun headline(entry: StockHistoryEntry): String = when (entry.type) {
    ProductMovementType.PRODUCTION -> entry.batchesText?.let { stringResource(R.string.stock_history_production_batches, it.replace('.', ',')) }
        ?: stringResource(R.string.stock_history_production)
    ProductMovementType.ADJUSTMENT -> stringResource(R.string.stock_history_count)
    ProductMovementType.SALE -> stringResource(R.string.stock_history_sold, entry.soldText.orEmpty())
}

@Composable
private fun HistoryRow(entry: StockHistoryEntry, unit: String, onDelete: (() -> Unit)?) {
    val semantic = LocalMankeuSemanticColors.current
    val amount = entry.quantity.toQuantityText()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(headline(entry), fontWeight = FontWeight.Bold)
                Text(
                    listOf(LocalDate.ofEpochDay(entry.dateEpochDay).toDisplayDate(), entry.notes).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    when (entry.type) {
                        ProductMovementType.PRODUCTION -> stringResource(R.string.stock_history_added, amount, unit)
                        ProductMovementType.SALE -> stringResource(R.string.stock_history_removed, amount, unit)
                        ProductMovementType.ADJUSTMENT -> stringResource(R.string.stock_history_counted, amount, unit)
                    },
                    color = if (entry.type == ProductMovementType.SALE) semantic.danger else MaterialTheme.colorScheme.onSurface,
                )
                Text(stringResource(R.string.stock_history_after, entry.stockAfter.toQuantityText(), unit), style = MaterialTheme.typography.bodySmall)
                if (onDelete == null) {
                    Text(stringResource(R.string.stock_history_sale_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.stock_history_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
