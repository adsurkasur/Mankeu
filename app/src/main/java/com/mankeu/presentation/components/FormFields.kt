package com.mankeu.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.components.form.ListRowField
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
    raw.replace(TRAILING_SEN, "").filter(Char::isDigit).dropWhile { it == '0' }.take(MAX_RUPIAH_DIGITS)

/**
 * ",50" or ".5" at the end is sen (U1 M5): a pasted "1.250.000,50" used to become 125000050.
 * Rupiah are whole, so the sen are dropped. Three digits after a separator stay: that is a
 * thousands group ("1.250", "1,250,000").
 */
private val TRAILING_SEN = Regex("[.,]\\d{1,2}\\D*$")

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

/** "1250000" -> "1.250.000". Only grouping: the input is already digits ([sanitizeRupiah]). */
fun groupThousands(digits: String): String = digits.reversed().chunked(3).joinToString(".").reversed()

/**
 * Shows the dots while typing (audit P2 §6) without putting them in the value: the field's text
 * stays the bare digits every caller already parses. A dot sits in front of every digit whose
 * distance from the end is a multiple of three.
 */
internal object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = groupThousands(digits)
        return TransformedText(
            AnnotatedString(grouped),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    offset + (1 until offset).count { (digits.length - it) % 3 == 0 }

                override fun transformedToOriginal(offset: Int): Int =
                    offset - grouped.take(offset).count { it == '.' }
            },
        )
    }
}

/** A rupiah amount field: `Rp` prefix, numeric keyboard, [sanitizeRupiah] applied on every key, thousands dotted. */
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
        visualTransformation = ThousandsTransformation,
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
 * Shown as a [ListRowField] ("Tanggal … Hari ini ›", audit P2 §4), not a read-only text field: the
 * old field opened its picker from a transparent layer that TalkBack could focus but not activate
 * (Task 5 / U1 carry). The row is one clickable with role Button, so a double-tap opens the picker.
 * [value] stays an ISO string so nothing downstream changes; the row shows "Hari ini" or the
 * owner's own date format. With [clearable] a set date gets a small × beside the row.
 */
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
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ListRowField(
            label = label,
            value = parsed?.let { dateWords(it) } ?: stringResource(R.string.date_none),
            onClick = { open = true },
            modifier = Modifier.weight(1f),
        )
        if (clearable && parsed != null) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.date_clear, label))
            }
        }
    }
    if (open) {
        MankeuDatePicker(initial = parsed, onDismiss = { open = false }) { onValueChange(it.toString()); open = false }
    }
}

/** "Hari ini", "Kemarin", else "30 Okt 2026". */
@Composable
fun dateWords(date: LocalDate): String = when (date) {
    LocalDate.now() -> stringResource(R.string.date_today)
    LocalDate.now().minusDays(1) -> stringResource(R.string.date_yesterday)
    else -> date.toDisplayDate()
}

/** The Material 3 date dialog on [initial] (today when null); shared by [DateField] and "Pilih tanggal" chips. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MankeuDatePicker(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val initialMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    val feedback = LocalFeedback.current
    // A tick per day picked, not for the initial selection the picker opens on.
    LaunchedEffect(pickerState) {
        snapshotFlow { pickerState.selectedDateMillis }.drop(1).collect { feedback(FeedbackEvent.TAP) }
    }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis -> onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}
