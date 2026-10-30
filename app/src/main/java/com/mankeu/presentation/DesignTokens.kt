package com.mankeu.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spacing, type and card tokens of the UX phase (audit P3, DEC-030).
 *
 * Screens used to pick paddings (14, 18, 20, 22 dp) and title sizes per card, so nothing lined up
 * and every card shouted. New and rebuilt screens take their numbers from here only.
 *
 * Colour roles live in [MankeuSemanticColors]: primary (blue) = action only, `success` = money in /
 * profit, `danger` = money out / loss / delete, `warning` = needs attention (unpaid, low stock, due).
 */
object Space {
    /** Inside a row: icon to text, chip to chip. */
    val Small = 8.dp
    /** Between two fields of a form. */
    val Field = 12.dp
    /** Screen edge and card padding. */
    val Edge = 16.dp
    /** Between sections. */
    val Section = 24.dp
    /** A one-line list row. Touch targets are never under 48 dp (M3 buttons already ensure it). */
    val Row = 56.dp
    /** A two-line list row. */
    val RowTwoLine = 72.dp
    /** The typed number between − and + of a Stepper: room for four digits. */
    val NumberField = 80.dp
}

/**
 * The four text sizes. [Figure] is the one main number of a card (at most one per card);
 * [Caption] is drawn in `onSurfaceVariant`.
 */
object MankeuType {
    val Title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val Figure = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    val Body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    val Caption = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
}

/**
 * The one card style: a tonal surface, no outline, no shadow. Cards are for summaries (at most
 * three per screen); items go in plain lists.
 */
@Composable
fun MankeuCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val body: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.fillMaxWidth().padding(Space.Edge), verticalArrangement = Arrangement.spacedBy(Space.Field), content = content)
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, elevation = elevation, content = body)
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, elevation = elevation, content = body)
    }
}
