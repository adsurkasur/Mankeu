package com.mankeu.presentation.components.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.mankeu.R
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space

/**
 * Choosing from a long list (products, materials, contacts): audit P2 §3. A search box on top, the
 * matches as plain rows, and — when the typed name matches nothing exactly — "Tambah '…'" first,
 * so a new name is added from where it was looked for instead of on another screen.
 *
 * Back closes the page without picking.
 */
@Composable
fun <T> PickerPage(
    title: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onPick: (T) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    itemDetail: ((T) -> String?)? = null,
    onAdd: ((String) -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    BackHandler(onBack = onClose)
    val typed = query.trim()
    val shown = if (typed.isEmpty()) items else items.filter { itemLabel(it).contains(typed, ignoreCase = true) }
    val offerAdd = onAdd != null && typed.isNotEmpty() && items.none { itemLabel(it).equals(typed, ignoreCase = true) }
    Scaffold(modifier = modifier.fillMaxSize(), topBar = { PageTopBar(title, onBack = onClose) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.picker_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Space.Edge, vertical = Space.Small),
            )
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = Space.Edge)) {
                if (offerAdd) {
                    item {
                        PickerRow(onClick = { onAdd?.invoke(typed) }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.picker_add, typed), style = MankeuType.Body, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider()
                    }
                }
                items(shown) { item ->
                    PickerRow(onClick = { onPick(item) }) {
                        Column {
                            Text(itemLabel(item), style = MankeuType.Body)
                            itemDetail?.invoke(item)?.let {
                                Text(it, style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    HorizontalDivider()
                }
                if (shown.isEmpty() && !offerAdd) {
                    item {
                        Text(
                            stringResource(R.string.picker_empty),
                            style = MankeuType.Caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Space.Edge),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Space.Row).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Field),
    ) { content() }
}
