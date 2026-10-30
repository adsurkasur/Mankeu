package com.mankeu.presentation.components.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.mankeu.R
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space
import com.mankeu.presentation.components.MankeuSheet

/**
 * A picked or defaulted value shown as a settings-style row: label left, value right, ›
 * (audit P2 §4). Outlined boxes are for typing only.
 *
 * The whole row is one clickable with role Button, so TalkBack reads "label, value, button" and a
 * double-tap opens the picker. The read-only text fields this replaces only took focus under
 * TalkBack (Task 5 note), because their opener was an unlabelled layer drawn over the field.
 *
 * Rows carry no side padding of their own: [FullPageForm] pads its content by `Space.Edge`, so a
 * row lines up with the typed fields above and below it.
 */
@Composable
fun ListRowField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (supporting == null) Space.Row else Space.RowTwoLine)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Small),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MankeuType.Body)
            if (supporting != null) Text(supporting, style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            value,
            style = MankeuType.Body,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One repeated item as a compact row ("Singkong · 50 kg" … "Rp1.500.000"), audit P2 §5. Tapping
 * opens a [LineEditSheet]; a card full of boxes per item is what made the note and product editor
 * look like a tax form.
 */
@Composable
fun LineRow(
    title: String,
    detail: String?,
    amount: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Space.Row)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Space.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Field),
    ) {
        Text(
            if (detail.isNullOrBlank()) title else "$title · $detail",
            style = MankeuType.Body,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (amount != null) Text(amount, style = MankeuType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}

/**
 * The small sheet that edits one [LineRow]: at most three fields (the caller's discipline), Simpan,
 * and a red Hapus when the line already exists.
 */
@Composable
fun LineEditSheet(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MankeuSheet(
        title = title,
        onDismiss = onDismiss,
        confirmEnabled = saveEnabled,
        onConfirm = onSave,
        startAction = onDelete?.let { delete ->
            {
                TextButton(onClick = delete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        content = content,
    )
}
