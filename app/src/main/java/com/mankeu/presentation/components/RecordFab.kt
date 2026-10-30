package com.mankeu.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.domain.FinancialEventType
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback

/**
 * The two everyday actions, in the thumb zone.
 *
 * Recording money moving is the reason the app is opened, and until now it took two taps through
 * the bottom bar and then a choice halfway up a scrolling list — the top of the screen on a phone
 * held in one hand. A speed dial in the bottom-right corner puts "Catat Pemasukan" and "Catat
 * Pengeluaran" under the thumb from anywhere, and carries the same green/coral money direction the
 * entry form uses so the choice is recognisable before the label is read.
 *
 * Fase 2 adds "Nota penjualan" on top: a product sale is a note (DEC-029 §11). Otherwise: setoran modal, ambil pribadi and beli aset are money in or
 * out too, chosen by their category in the entry form (DEC-011 K-2), so they need no button of
 * their own.
 */
@Composable
fun RecordSpeedDial(
    onRecord: (FinancialEventType) -> Unit,
    modifier: Modifier = Modifier,
    /** Fase 2 (DEC-029 §11 §12): product sales go through the note, from the same button. */
    onSaleNote: () -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val motion = LocalMotion.current
    val semantic = LocalMankeuSemanticColors.current
    val feedback = LocalFeedback.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(maxOf(motion.fastMillis, 1), easing = FastOutSlowInEasing),
        label = "fabRotation",
    )
    val enter = remember(motion) {
        scaleIn(tween(maxOf(motion.fastMillis, 1), easing = FastOutSlowInEasing)) +
            fadeIn(tween(maxOf(motion.fastMillis, 1)))
    }
    val exit = remember(motion) {
        scaleOut(tween(maxOf(motion.fastMillis, 1), easing = FastOutSlowInEasing)) +
            fadeOut(tween(maxOf(motion.fastMillis, 1)))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
            ExtendedFloatingActionButton(
                onClick = {
                    expanded = false
                    onSaleNote()
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.fab_sale_note), fontWeight = FontWeight.Bold)
            }
        }
        AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
            ExtendedFloatingActionButton(
                onClick = {
                    expanded = false
                    onRecord(FinancialEventType.CASH_INCOME)
                },
                containerColor = semantic.successContainer,
                contentColor = semantic.onSuccessContainer,
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.fab_record_income), fontWeight = FontWeight.Bold)
            }
        }
        AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
            ExtendedFloatingActionButton(
                onClick = {
                    expanded = false
                    onRecord(FinancialEventType.CASH_EXPENSE)
                },
                containerColor = semantic.dangerContainer,
                contentColor = semantic.onDangerContainer,
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.fab_record_expense), fontWeight = FontWeight.Bold)
            }
        }
        MankeuTooltip(stringResource(if (expanded) R.string.fab_close else R.string.fab_record)) {
            FloatingActionButton(
                onClick = { feedback(FeedbackEvent.TAP); expanded = !expanded },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(if (expanded) R.string.fab_close else R.string.fab_record),
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
    }
}
