package com.mankeu.presentation.components.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.mankeu.presentation.Space
import com.mankeu.presentation.components.MoneyField
import com.mankeu.presentation.toRupiah

/** A quick amount that fits this form: "Sisa tagihan", "Harga terakhir". */
data class MoneyChip(val label: String, val amount: Long)

/**
 * A rupiah amount as a `Long`: [MoneyField] (digits only, "1.250.000" while typing) plus the
 * context chips of audit P2 §6. Blank and a lone "0" both read as `null`, so a caller decides what
 * an empty amount means instead of silently storing 0.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyInput(
    label: String,
    value: Long?,
    onValueChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    chips: List<MoneyChip> = emptyList(),
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.Small)) {
        MoneyField(
            label = label,
            value = value?.toString().orEmpty(),
            supportingText = supportingText,
            isError = isError,
            imeAction = imeAction,
        ) { onValueChange(it.toLongOrNull()) }
        if (chips.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                chips.forEach { chip ->
                    SuggestionChip(onClick = { onValueChange(chip.amount) }, label = { Text("${chip.label} ${chip.amount.toRupiah()}") })
                }
            }
        }
    }
}
