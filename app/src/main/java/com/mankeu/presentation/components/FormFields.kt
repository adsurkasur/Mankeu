package com.mankeu.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.toDisplayDate
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.flow.drop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The two input types this app gets wrong most easily — money and dates — in one place.
 *
 * Both used to be written per screen. The entry form got a date picker and digit-only,
 * overflow-safe amounts in round 4 while other dialogs kept typing ISO
 * dates into a bare text field and accepting `0001200000` as an amount. Same control, same rules,
 * one definition.
 */

/**
 * `Long.MAX_VALUE` is 19 digits. 15 covers every realistic rupiah figure (up to Rp999 trillion)
 * and makes an overflow unreachable from the keyboard.
 */
private const val MAX_RUPIAH_DIGITS = 15

/**
 * Digits only, no leading zeros. Money is stored as exact integer rupiah, so there is no decimal
 * separator to disagree about and no float to round. Leading zeros are dropped because typing over
 * a pre-filled "0" used to leave `0500000` — it parses, but it reads as a different number.
 */
fun sanitizeRupiah(raw: String): String =
    raw.filter(Char::isDigit).dropWhile { it == '0' }.take(MAX_RUPIAH_DIGITS)

/**
 * Digits plus at most one decimal separator, normalised to the comma an Indonesian keyboard
 * produces. Callers normalise back to a dot before parsing.
 */
fun sanitizeDecimal(raw: String): String {
    val builder = StringBuilder()
    var separatorUsed = false
    raw.forEach { char ->
        when {
            char.isDigit() -> builder.append(char)
            (char == ',' || char == '.') && !separatorUsed && builder.isNotEmpty() -> {
                separatorUsed = true
                builder.append(',')
            }
        }
    }
    return builder.toString().take(12)
}

/** A rupiah amount field: `Rp` prefix, numeric keyboard, [sanitizeRupiah] applied on every key. */
@Composable
fun MoneyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    // Next by default so the keyboard walks the form; the last field of a form passes Done.
    imeAction: ImeAction = ImeAction.Next,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeRupiah(it)) },
        label = { Text(label) },
        prefix = { Text("Rp ") },
        supportingText = supportingText,
        isError = isError,
        singleLine = true,
        // Number, not Decimal: rupiah is stored as whole integers, so a decimal key would only
        // offer a separator the field then strips.
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Date entry through the Material 3 date picker rather than a typed `YYYY-MM-DD` field.
 *
 * The typed field demanded ISO format from people who write dates as `3/8`, offered no calendar to
 * check the day against, and surfaced a typo only after the whole form had been filled in. [value]
 * stays an ISO string so nothing downstream changes; the field displays the owner's own format.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val parsed = remember(value) { runCatching { LocalDate.parse(value) }.getOrNull() }
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = parsed?.toDisplayDate().orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (clearable && value.isNotBlank()) {
                        MankeuTooltip(stringResource(R.string.action_cancel)) {
                            IconButton(onClick = { onValueChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                            }
                        }
                    }
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // A read-only OutlinedTextField still consumes taps, so the opener is a transparent layer
        // above it rather than a clickable on the field. The layer stops short of the trailing area
        // when a clear button is present, so that button keeps its own hit target.
        Box(
            Modifier
                .matchParentSize()
                .padding(end = if (clearable && value.isNotBlank()) 96.dp else 0.dp)
                .clickable { open = true },
        )
    }
    if (open) {
        val initialMillis = (parsed ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        val feedback = LocalFeedback.current
        // A tick per day picked, not for the initial selection the picker opens on.
        LaunchedEffect(pickerState) {
            snapshotFlow { pickerState.selectedDateMillis }.drop(1).collect { feedback(FeedbackEvent.TAP) }
        }
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
