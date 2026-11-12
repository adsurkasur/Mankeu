package com.mankeu.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mankeu.R
import com.mankeu.domain.HppChange

@Composable
internal fun PriceImpactRows(change: HppChange) {
    if (!change.before.complete || !change.after.complete) {
        Text("${change.product.name}: ${stringResource(R.string.price_alert_incomplete)}")
    } else {
        change.before.perSaleUnit.zip(change.after.perSaleUnit).forEach { (before, after) ->
            Text(stringResource(R.string.price_alert_row, change.product.name,
                before.hppRupiah.toRupiah(), after.hppRupiah.toRupiah(), after.unit.name,
                before.profitRupiah.toRupiah(), after.profitRupiah.toRupiah()))
        }
    }
}
