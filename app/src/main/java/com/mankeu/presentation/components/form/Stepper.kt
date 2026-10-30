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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space

/**
 * − n + for small counts (batches, boxes): audit P2 §3. The number stays typeable for the rare
 * large count; whatever is typed is held to [min]..[max], and a blank field keeps the last value.
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
    Row(
        modifier.heightIn(min = Space.Row),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.Small),
    ) {
        if (label != null) Text(label, style = MankeuType.Body, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onValueChange(value - 1) }, enabled = value > min) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.stepper_decrease))
        }
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { raw -> raw.filter(Char::isDigit).take(6).toIntOrNull()?.let { onValueChange(it.coerceIn(min, max)) } },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp),
        )
        FilledTonalIconButton(onClick = { onValueChange(value + 1) }, enabled = value < max) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.stepper_increase))
        }
    }
}
