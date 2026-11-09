package com.mankeu.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.screens.MoneyFormMode

/**
 * U6: Single record action button (+) that opens a clean choice sheet.
 * Replaces the temporary 5-item expanding speed dial to prevent layout jitter and touch target crowding.
 */
@Composable
fun RecordSpeedDial(
    onRecord: (MoneyFormMode) -> Unit,
    modifier: Modifier = Modifier,
    /** Fase 2 (DEC-029 §11 §12): product sales go through the note, from the same button. */
    onSaleNote: () -> Unit = {},
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val feedback = LocalFeedback.current

    MankeuTooltip(stringResource(R.string.fab_record)) {
        FloatingActionButton(
            onClick = {
                feedback(FeedbackEvent.TAP)
                showSheet = true
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = modifier,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.fab_record),
            )
        }
    }

    if (showSheet) {
        RecordChoiceSheet(
            onDismiss = { showSheet = false },
            onSaleNote = {
                showSheet = false
                onSaleNote()
            },
            onRecord = { mode ->
                showSheet = false
                onRecord(mode)
            },
        )
    }
}

@Composable
fun RecordChoiceSheet(
    onDismiss: () -> Unit,
    onSaleNote: () -> Unit,
    onRecord: (MoneyFormMode) -> Unit,
) {
    val semantic = LocalMankeuSemanticColors.current
    val feedback = LocalFeedback.current

    MankeuSheet(
        title = stringResource(R.string.fab_record),
        onDismiss = onDismiss,
        confirmLabel = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RecordChoiceRow(
                title = stringResource(R.string.fab_sale_note),
                subtitle = stringResource(R.string.record_choice_sale_sub),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                iconTint = MaterialTheme.colorScheme.secondary,
                onClick = {
                    feedback(FeedbackEvent.TAP)
                    onSaleNote()
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            RecordChoiceRow(
                title = stringResource(R.string.fab_record_expense),
                subtitle = stringResource(R.string.record_choice_expense_sub),
                icon = Icons.Default.ArrowUpward,
                iconTint = semantic.danger,
                onClick = {
                    feedback(FeedbackEvent.TAP)
                    onRecord(MoneyFormMode.PURCHASE)
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            RecordChoiceRow(
                title = stringResource(R.string.fab_record_income),
                subtitle = stringResource(R.string.record_choice_in_sub),
                icon = Icons.Default.ArrowDownward,
                iconTint = semantic.success,
                onClick = {
                    feedback(FeedbackEvent.TAP)
                    onRecord(MoneyFormMode.OTHER_IN)
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            RecordChoiceRow(
                title = stringResource(R.string.fab_other_out),
                subtitle = stringResource(R.string.record_choice_out_sub),
                icon = Icons.Default.ArrowUpward,
                iconTint = semantic.danger,
                onClick = {
                    feedback(FeedbackEvent.TAP)
                    onRecord(MoneyFormMode.OTHER_OUT)
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            RecordChoiceRow(
                title = stringResource(R.string.fab_capital),
                subtitle = stringResource(R.string.record_choice_capital_sub),
                icon = Icons.Default.AccountBalance,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = {
                    feedback(FeedbackEvent.TAP)
                    onRecord(MoneyFormMode.CAPITAL)
                },
            )
        }
    }
}

@Composable
private fun RecordChoiceRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
