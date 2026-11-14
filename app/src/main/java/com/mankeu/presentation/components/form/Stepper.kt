package com.mankeu.presentation.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.mankeu.R
import com.mankeu.domain.TransactionCalculation
import com.mankeu.presentation.components.sanitizeDecimal
import java.math.BigDecimal
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space

/**
 * − n + for small counts (batches, boxes): audit P2 §3. The number stays typeable for the rare
 * large count: it can be cleared and retyped, what is typed is held to [min]..[max], and a field
 * left blank becomes [min] when focus leaves it.
 */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    min: Int = 0,
    max: Int = 9_999,
) {
    // Local text so the field can be blank while retyping; re-synced whenever the value changes.
    var text by remember(value) { mutableStateOf(value.toString()) }
    // "Kurangi Batch" rather than the same "Kurangi" for every stepper on the page.
    val decrease = listOfNotNull(stringResource(R.string.stepper_decrease), label).joinToString(" ")
    val increase = listOfNotNull(stringResource(R.string.stepper_increase), label).joinToString(" ")
    Row(
        modifier.heightIn(min = Space.Row),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Small),
    ) {
        if (label != null) Text(label, style = MankeuType.Body, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onValueChange(value - 1) }, enabled = value > min) {
            Icon(Icons.Default.Remove, contentDescription = decrease)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw.filter(Char::isDigit).take(6)
                text.toIntOrNull()?.let { onValueChange(it.coerceIn(min, max)) }
            },
            singleLine = true,
            textStyle = MankeuType.Body.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(Space.NumberField)
                .onFocusChanged { focus ->
                    if (!focus.isFocused) {
                        if (text.isBlank() && value != min) onValueChange(min)
                        text = (if (text.isBlank()) min else value).toString()
                    }
                }
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        )
        FilledTonalIconButton(onClick = { onValueChange(value + 1) }, enabled = value < max) {
            Icon(Icons.Default.Add, contentDescription = increase)
        }
    }
}

/** [text] plus [delta] as a quantity in the form's own format ("2,5"); null when the result would not be above zero. */
fun stepQuantity(text: String, delta: Int): String? {
    val next = (TransactionCalculation.parseQuantity(text) ?: BigDecimal.ZERO) + BigDecimal(delta)
    return next.takeIf { it.signum() > 0 }?.stripTrailingZeros()?.toPlainString()?.replace('.', ',')
}

/**
 * [Stepper] for a quantity kept as text (note lines, which may be "2,5 kg"): − and + step by one,
 * the number between them stays typeable with a decimal comma, and − stops above zero.
 */
@Composable
fun QuantityStepper(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val decrease = stringResource(R.string.stepper_decrease) + " " + label
    val increase = stringResource(R.string.stepper_increase) + " " + label
    Row(modifier.heightIn(min = Space.Row), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
        Text(label, style = MankeuType.Body, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { stepQuantity(value, -1)?.let(onValueChange) }, enabled = stepQuantity(value, -1) != null) {
            Icon(Icons.Default.Remove, contentDescription = decrease)
        }
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(sanitizeDecimal(it)) },
            singleLine = true,
            textStyle = MankeuType.Body.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = isError,
            modifier = fieldModifier.width(Space.NumberField).semantics { contentDescription = label },
        )
        FilledTonalIconButton(onClick = { stepQuantity(value, 1)?.let(onValueChange) }) {
            Icon(Icons.Default.Add, contentDescription = increase)
        }
    }
}
