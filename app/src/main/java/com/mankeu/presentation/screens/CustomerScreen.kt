package com.mankeu.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mankeu.R
import com.mankeu.data.ContactDraft
import com.mankeu.data.ContactEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.SaleRecord
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.MankeuCard
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.ShareText
import com.mankeu.presentation.Space
import com.mankeu.presentation.UiText
import com.mankeu.presentation.components.form.LineRow
import com.mankeu.presentation.components.form.ListRowField
import com.mankeu.presentation.resolve
import com.mankeu.presentation.toDisplayDate
import com.mankeu.presentation.toRupiah
import java.time.LocalDate

/**
 * The customer page (U5, audit L21): what they still owe, Terima bayar and Kirim pengingat, then
 * their notes (open first, oldest open on top) and Ubah data. Beranda's Tagihan rows and Kontak
 * open it. Terima bayar pays the open notes oldest first through the repository.
 */
@Composable
fun CustomerScreen(
    contact: ContactEntity,
    /** This customer's notes in the whole business (contacts are per business, DEC-029 §9). */
    sales: List<SaleRecord>,
    products: List<ProductRecord>,
    others: List<ContactEntity>,
    businessName: String,
    onOpenNote: (String) -> Unit,
    /** Pays [saleIds] (open notes, oldest first) with one amount. */
    onPay: (saleIds: List<String>, amountRupiah: Long, date: LocalDate, onDone: () -> Unit, onError: (UiText) -> Unit) -> Unit,
    onSend: (text: String, whatsAppLink: String?) -> Unit,
    onSaveContact: (id: String?, draft: ContactDraft, onSaved: (String) -> Unit, onError: (UiText) -> Unit) -> Unit,
    onArchiveContact: (id: String, onDone: () -> Unit, onError: (UiText) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var paying by rememberSaveable { mutableStateOf(false) }
    var sending by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val semantic = LocalMankeuSemanticColors.current
    val open = sales.filter { it.outstanding > 0 }.sortedWith(compareBy({ it.sale.dateEpochDay }, { it.sale.createdAtEpochMillis }))
    val paid = sales.filter { it.outstanding == 0L }.sortedByDescending { it.sale.dateEpochDay }
    val owed = open.sumOf { it.outstanding }
    val words = shareWords()

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Edge), verticalArrangement = Arrangement.spacedBy(Space.Field)) {
        item {
            MankeuCard {
                Column {
                    Text(stringResource(R.string.customer_owes), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(owed.toRupiah(), style = MankeuType.Figure, color = if (owed > 0) semantic.warning else MaterialTheme.colorScheme.onSurface)
                }
                if (owed > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                        Button(onClick = { paying = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.customer_receive)) }
                        OutlinedButton(onClick = { sending = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.customer_remind)) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.customer_notes), style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (sales.isEmpty()) {
            item { Text(stringResource(R.string.customer_no_notes), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(open + paid, key = { it.sale.id }) { note ->
            Column {
                LineRow(
                    title = LocalDate.ofEpochDay(note.sale.dateEpochDay).toDisplayDate(),
                    detail = if (note.outstanding > 0) stringResource(R.string.sale_left, note.outstanding.toRupiah()) else stringResource(R.string.sale_status_paid),
                    amount = note.total.toRupiah(),
                    onClick = { onOpenNote(note.sale.id) },
                )
                HorizontalDivider()
            }
        }
        item {
            ListRowField(label = stringResource(R.string.customer_edit), value = contact.phone.orEmpty(), onClick = { editing = true })
        }
    }

    if (paying) {
        PaySheet(
            outstanding = owed,
            earliest = open.firstOrNull()?.let { LocalDate.ofEpochDay(it.sale.dateEpochDay) } ?: LocalDate.now(),
            title = stringResource(R.string.customer_receive_title, contact.name),
            onDismiss = { paying = false },
            onPay = { amount, date, onDone, onError -> onPay(open.map { it.sale.id }, amount, date, onDone, onError) },
        )
    }
    if (sending) {
        SendChoice(
            text = ShareText.reminder(businessName, contact.name, open.map { it.toShareNote(products) }, words),
            phone = contact.phone,
            onSend = onSend,
            onDismiss = { sending = false },
        )
    }
    if (editing) {
        var error by remember { mutableStateOf<UiText?>(null) }
        ContactSheet(
            existing = contact,
            others = others.filter { it.id != contact.id },
            error = error?.resolve(),
            initialName = "",
            onDismiss = { editing = false },
            onSave = { draft -> error = null; onSaveContact(contact.id, draft, { editing = false }, { error = it }) },
            onArchive = { error = null; onArchiveContact(contact.id, { editing = false }, { error = it }) },
        )
    }
}
