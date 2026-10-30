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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.ContactDraft
import com.mankeu.data.ContactEntity
import com.mankeu.data.PayableRecord
import com.mankeu.data.SaleRecord
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.UiText
import com.mankeu.presentation.resolve
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.toRupiah

/**
 * Contacts (F2-2): customers and suppliers in one list, with what each still owes or is owed.
 * The same page picks a customer for a note ([onPick] set): tapping a row picks it, and
 * "Tambah kontak baru" creates one with the searched name and picks it once saved.
 */
@Composable
fun ContactsScreen(
    contacts: List<ContactEntity>,
    /** Notes and credit purchases of the whole business: contacts are per business (DEC-029 §9). */
    sales: List<SaleRecord>,
    payables: List<PayableRecord>,
    /** Saves ([id] null = new) and calls back with the stored id; refused, [onError] and the sheet stays open. */
    onSave: (id: String?, draft: ContactDraft, onSaved: (String) -> Unit, onError: (UiText) -> Unit) -> Unit,
    onArchive: (id: String, onDone: () -> Unit, onError: (UiText) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onPick: ((String) -> Unit)? = null,
    header: @Composable () -> Unit = {},
) {
    var search by rememberSaveable { mutableStateOf("") }
    // "" = a new contact, an id = that contact, null = no sheet.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val semantic = LocalMankeuSemanticColors.current
    val owes = sales.groupBy { it.sale.contactId }.mapValues { (_, rows) -> rows.sumOf { it.outstanding } }
    val owed = payables.groupBy { it.purchase.contactId }.mapValues { (_, rows) -> rows.sumOf { it.outstanding } }
    val visible = contacts.filter { search.isBlank() || it.name.contains(search.trim(), ignoreCase = true) || it.phone.orEmpty().contains(search.trim()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { header() }
        item {
            OutlinedTextField(
                value = search, onValueChange = { search = it }, singleLine = true,
                label = { Text(stringResource(R.string.contacts_search)) }, modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedButton(onClick = { editing = "" }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" " + stringResource(R.string.contacts_add))
            }
        }
        if (visible.isEmpty()) {
            item { Text(stringResource(R.string.contacts_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(visible, key = { it.id }) { contact ->
            Card(
                onClick = { if (onPick != null) onPick(contact.id) else editing = contact.id },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val roles = listOfNotNull(
                        stringResource(R.string.contact_customer).takeIf { contact.isCustomer },
                        stringResource(R.string.contact_supplier).takeIf { contact.isSupplier },
                    ) + listOfNotNull(contact.phone)
                    Text(roles.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    owes[contact.id]?.takeIf { it > 0 }?.let {
                        Text(stringResource(R.string.contact_owes, it.toRupiah()), color = semantic.onWarningContainer, fontWeight = FontWeight.Bold)
                    }
                    owed[contact.id]?.takeIf { it > 0 }?.let {
                        Text(stringResource(R.string.contact_owed, it.toRupiah()), color = semantic.danger, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    editing?.let { id ->
        val existing = contacts.firstOrNull { it.id == id }
        var error by remember(id) { mutableStateOf<UiText?>(null) }
        ContactSheet(
            existing = existing,
            others = contacts.filter { it.id != existing?.id },
            error = error?.resolve(),
            initialName = if (existing == null) search.trim() else "",
            onDismiss = { editing = null },
            onSave = { draft ->
                error = null
                onSave(existing?.id, draft, { savedId ->
                    editing = null
                    if (existing == null) onPick?.invoke(savedId)
                }, { error = it })
            },
            onArchive = existing?.let { c -> { error = null; onArchive(c.id, { editing = null }, { error = it }) } },
        )
    }
}

/** The contact form: short, so a sheet (#13). */
@Composable
private fun ContactSheet(
    existing: ContactEntity?,
    /** The business's other active contacts: a name already in use is caught here, before saving. */
    others: List<ContactEntity>,
    /** A refusal from the repository, shown in the sheet because the snackbar is behind it. */
    error: String?,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (ContactDraft) -> Unit,
    onArchive: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: initialName) }
    var phone by rememberSaveable { mutableStateOf(existing?.phone.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(existing?.address.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes.orEmpty()) }
    var customer by rememberSaveable { mutableStateOf(existing?.isCustomer ?: true) }
    var supplier by rememberSaveable { mutableStateOf(existing?.isSupplier ?: false) }
    val duplicate = others.any { it.name.trim().equals(name.trim(), ignoreCase = true) }
    MankeuSheet(
        title = stringResource(if (existing == null) R.string.contact_new_title else R.string.contact_edit_title),
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank() && !duplicate,
        onConfirm = { onSave(ContactDraft(name, phone, address, notes, customer, supplier)) },
    ) {
        OutlinedTextField(
            name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.contact_name)) },
            isError = duplicate,
            supportingText = if (duplicate) ({ Text(stringResource(R.string.contact_duplicate, name.trim())) }) else null,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = customer, onClick = { customer = !customer }, label = { Text(stringResource(R.string.contact_customer)) })
            FilterChip(selected = supplier, onClick = { supplier = !supplier }, label = { Text(stringResource(R.string.contact_supplier)) })
        }
        OutlinedTextField(
            phone, { phone = it }, singleLine = true, label = { Text(stringResource(R.string.contact_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.contact_address)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.contact_notes)) }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        if (onArchive != null) {
            TextButton(onClick = onArchive) { Text(stringResource(R.string.contact_archive), color = MaterialTheme.colorScheme.error) }
        }
    }
}
