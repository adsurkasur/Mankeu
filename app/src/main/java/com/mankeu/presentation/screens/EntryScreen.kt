package com.mankeu.presentation.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mankeu.domain.MoneyEvent
import com.mankeu.presentation.guide.HelpArticle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.EventCard
import com.mankeu.presentation.components.SectionTitle
import com.mankeu.presentation.BusinessWords
import com.mankeu.presentation.components.NoticeCard

/**
 * The Catat (record transaction) destination: the notes above, then the money records with search
 * and a direction filter. Recording and correcting go to full pages (U5): Ubah opens the record on
 * its page ([onEdit]), the empty card opens Uang masuk lain ([onNew]). U6 merges the two lists.
 */
@Composable
fun EntryScreen(
    state: AppUiState,
    onEdit: (MoneyEvent) -> Unit,
    onDelete: (String) -> Unit,
    onManageScope: () -> Unit,
    onNew: (MoneyFormMode) -> Unit = {},
    /** Fase 2: product sales are notes (DEC-029 §11); their list sits above the transactions (§12). */
    onOpenSale: (String) -> Unit = {},
    noteFilter: NoteFilter = NoteFilter.OPEN,
    onNoteFilter: (NoteFilter) -> Unit = {},
) {
    var search by rememberSaveable { mutableStateOf("") }
    var direction by rememberSaveable { mutableStateOf("ALL") }
    val feedback = LocalFeedback.current

    val visibleEvents = remember(state.events, search, direction) {
        state.events.filter { event ->
            val matchesText = search.isBlank() ||
                listOf(event.description, event.category, event.reference)
                    .filterNotNull()
                    .any { it.contains(search, ignoreCase = true) }
            val incoming = BusinessWords.isIncoming(event.type)
            matchesText && when (direction) {
                "IN" -> incoming == true
                "OUT" -> incoming == false
                else -> true
            }
        }
    }

    LazyColumn(
        // bottom clears the speed dial; see HomeScreen for why the list needs it itself.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "entry_title") {
            SectionTitle(
                title = stringResource(R.string.entry_title),
                subtitle = stringResource(R.string.ledger_subtitle, state.eventCount),
            )
        }

        if (state.selectedProjectId == null || state.projects.isEmpty()) {
            item(key = "entry_no_project") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.entry_no_project_banner_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Text(
                            stringResource(R.string.entry_no_project_banner_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Button(
                            onClick = onManageScope,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text(stringResource(R.string.entry_action_add_project), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        saleNoteItems(
            sales = state.sales,
            filter = noteFilter,
            onFilter = { feedback(FeedbackEvent.TAP); onNoteFilter(it) },
            onOpen = onOpenSale,
        )

        if (state.eventCount > 0) {
            item(key = "entry_search") {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(stringResource(R.string.home_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "entry_direction_filters") {
                val filters = listOf(
                    "ALL" to stringResource(R.string.money_all),
                    "IN" to stringResource(R.string.money_in_short),
                    "OUT" to stringResource(R.string.money_out_short),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters, key = { it.first }) { (value, label) ->
                        FilterChip(
                            selected = direction == value,
                            onClick = { feedback(FeedbackEvent.TAP); direction = value },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        when {
            state.eventCount == 0 -> item(key = "entry_empty") {
                EmptyCard(
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.home_empty_body),
                    action = stringResource(R.string.home_empty_action),
                    learn = HelpArticle.RECORD_INCOME,
                    onAction = { onNew(MoneyFormMode.OTHER_IN) },
                )
            }

            visibleEvents.isEmpty() -> item(key = "entry_no_match") {
                EmptyCard(
                    title = stringResource(R.string.home_no_match_title),
                    body = stringResource(R.string.home_no_match_body),
                )
            }

            else -> {
                items(visibleEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onEdit = onEdit,
                        onDelete = onDelete,
                    )
                }
                if (state.eventCount > state.events.size) {
                    item(key = "entry_truncated") {
                        NoticeCard(
                            title = stringResource(R.string.ledger_truncated_title),
                            body = stringResource(
                                R.string.ledger_truncated_body,
                                state.events.size,
                                state.eventCount,
                            ),
                            icon = Icons.Default.Info,
                        )
                    }
                }
            }
        }
    }
}
