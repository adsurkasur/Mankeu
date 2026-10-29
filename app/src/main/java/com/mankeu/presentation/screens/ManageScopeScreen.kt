package com.mankeu.presentation.screens

import com.mankeu.presentation.guide.HelpArticle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import com.mankeu.presentation.components.SheetBackHandler
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.data.BusinessEntity
import com.mankeu.data.ProjectEntity
import com.mankeu.presentation.AppUiState
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.presentation.components.DateField
import com.mankeu.presentation.components.EmptyCard
import com.mankeu.presentation.components.SectionTitle
import androidx.compose.foundation.layout.Box
import com.mankeu.presentation.toDisplayDate
import java.time.LocalDate

/**
 * The scope tree lives one tap from the header that shows it, as a sheet rather than a pushed
 * screen: picking a farm or a field is a momentary detour from whatever the user was doing, and a
 * full-screen route made it feel like leaving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScopeSheet(
    state: AppUiState,
    onDismiss: () -> Unit,
    onSelectBusiness: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onCreateBusiness: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameBusiness: (String) -> Unit,
    onRemoveBusiness: (String) -> Unit,
    onUpdateProject: (String, String, String, LocalDate?, LocalDate?) -> Unit,
    onRemoveProject: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        // Back closes the sheet first, as in MankeuSheet (cleanup P6).
        SheetBackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxWidth()) {
            ManageScopeScreen(
                state = state,
                modifier = Modifier.navigationBarsPadding(),
                onSelectBusiness = onSelectBusiness,
                onSelectProject = onSelectProject,
                onCreateBusiness = onCreateBusiness,
                onCreateProject = onCreateProject,
                onRenameBusiness = onRenameBusiness,
                onRemoveBusiness = onRemoveBusiness,
                onUpdateProject = onUpdateProject,
                onRemoveProject = onRemoveProject,
            )
        }
    }
}

/**
 * Managing the whole scope tree, not just the two currently-selected rows.
 *
 * The scope header can only switch between things that already exist, and Berkas dan Data could
 * only add one more or rename the active one. Neither could answer "what farms do I have?" or
 * "retire last season's field" — so a second field created by accident stayed in the picker
 * forever. This screen lists every farm and every field of the selected farm, and makes select,
 * edit and archive first-class next to add.
 *
 * Archive, never delete: every posted journal entry hangs off a book under a field under a farm,
 * so removing rows would break the audit trail the product is built on. Archiving hides the scope
 * and leaves the history intact and exportable.
 */
@Composable
fun ManageScopeScreen(
    state: AppUiState,
    onSelectBusiness: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onCreateBusiness: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameBusiness: (String) -> Unit,
    onRemoveBusiness: (String) -> Unit,
    onUpdateProject: (String, String, String, LocalDate?, LocalDate?) -> Unit,
    onRemoveProject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // With nothing yet, the only thing to do here is name the business: open its form straight away.
    var addBusiness by rememberSaveable { mutableStateOf(state.businesses.isEmpty()) }
    var addProject by rememberSaveable { mutableStateOf(false) }
    var renameBusiness by remember { mutableStateOf<BusinessEntity?>(null) }
    var removeBusiness by remember { mutableStateOf<BusinessEntity?>(null) }
    var editProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var removeProject by remember { mutableStateOf<ProjectEntity?>(null) }

    val activeBusiness = state.businesses.firstOrNull { it.id == state.selectedBusinessId }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(
                    title = stringResource(R.string.scope_manage_title),
                    subtitle = stringResource(R.string.scope_manage_subtitle),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { Text(stringResource(R.string.scope_section_business), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
        if (state.businesses.isEmpty()) {
            item { EmptyCard(stringResource(R.string.scope_empty_business), stringResource(R.string.scope_empty_business_body), learn = HelpArticle.START_SCOPE) }
        } else {
            items(state.businesses, key = { it.id }) { business ->
                ScopeRow(
                    title = business.name,
                    // Only the selected farm's fields are loaded, so a count is only honest for it.
                    subtitle = if (business.id == state.selectedBusinessId) {
                        stringResource(R.string.scope_business_fields, state.projects.size)
                    } else {
                        null
                    },
                    selected = business.id == state.selectedBusinessId,
                    onSelect = { onSelectBusiness(business.id) },
                    onEdit = { renameBusiness = business },
                    onRemove = { removeBusiness = business }.takeIf { state.businesses.size > 1 },
                )
            }
        }
        item {
            FilledTonalButton(
                onClick = { addBusiness = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scope_add_business))
            }
        }

        // Nothing below this line means anything until a business exists: the heading would read
        // "Unit pada " with a hole where the name goes, over a card repeating the empty-state copy.
        if (activeBusiness != null) {
            item { Spacer(Modifier.width(0.dp)) }
            item {
                Text(
                    stringResource(R.string.scope_section_project, activeBusiness.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
            }
            if (state.projects.isEmpty()) {
                item { EmptyCard(stringResource(R.string.scope_empty_project), stringResource(R.string.scope_empty_project_body)) }
            } else {
                items(state.projects, key = { it.id }) { project ->
                    ScopeRow(
                        title = project.name,
                        subtitle = project.summaryLine(),
                        selected = project.id == state.selectedProjectId,
                        onSelect = { onSelectProject(project.id) },
                        onEdit = { editProject = project },
                        onRemove = { removeProject = project },
                    )
                }
            }
            item {
                FilledTonalButton(
                    onClick = { addProject = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scope_add_project))
                }
            }
        }
    }

    if (addBusiness) {
        AddBusinessDialog(
            onDismiss = { addBusiness = false },
            onSave = { name ->
                onCreateBusiness(name)
                addBusiness = false
            },
        )
    }
    if (addProject) {
        NameDialog(stringResource(R.string.dialog_add_project), stringResource(R.string.dialog_project_name), { addProject = false }) {
            onCreateProject(it); addProject = false
        }
    }
    renameBusiness?.let { business ->
        NameDialog(
            stringResource(R.string.data_rename_business),
            stringResource(R.string.dialog_business_name),
            { renameBusiness = null },
            business.name,
        ) { onRenameBusiness(it); renameBusiness = null }
    }
    editProject?.let { project ->
        ProjectDialog(project, { editProject = null }) { name, note, start, end ->
            onUpdateProject(project.id, name, note, start, end)
            editProject = null
        }
    }
    removeBusiness?.let { business ->
        ConfirmRemoveSheet(business.name, { removeBusiness = null }) {
            onRemoveBusiness(business.id); removeBusiness = null
        }
    }
    removeProject?.let { project ->
        ConfirmRemoveSheet(project.name, { removeProject = null }) {
            onRemoveProject(project.id); removeProject = null
        }
    }
}

/** "12 Nov 2026 – 3 Mar 2027", "Mulai 12 Nov 2026", or the note, whichever the field actually has. */
@Composable
private fun ProjectEntity.summaryLine(): String? {
    val start = startDateEpochDay?.let(LocalDate::ofEpochDay)?.toDisplayDate()
    val end = endDateEpochDay?.let(LocalDate::ofEpochDay)?.toDisplayDate()
    val period = when {
        start != null && end != null -> stringResource(R.string.scope_project_period, start, end)
        start != null -> stringResource(R.string.scope_project_period_open, start)
        else -> null
    }
    return listOfNotNull(period, description.ifBlank { null }).joinToString(" • ").ifBlank { null }
}

@Composable
private fun ScopeRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.Black,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                    subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (selected) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        label = { Text(stringResource(R.string.scope_active), fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary,
                            disabledLabelColor = MaterialTheme.colorScheme.onPrimary,
                            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!selected) {
                    OutlinedButton(
                        onClick = onSelect,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.scope_select),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.scope_edit),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
                if (onRemove != null) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(R.string.scope_remove),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate?, LocalDate?) -> Unit,
) {
    var name by rememberSaveable(project.id) { mutableStateOf(project.name) }
    var note by rememberSaveable(project.id) { mutableStateOf(project.description) }
    var start by rememberSaveable(project.id) {
        mutableStateOf(project.startDateEpochDay?.let(LocalDate::ofEpochDay)?.toString().orEmpty())
    }
    var end by rememberSaveable(project.id) {
        mutableStateOf(project.endDateEpochDay?.let(LocalDate::ofEpochDay)?.toString().orEmpty())
    }
    MankeuSheet(
        title = stringResource(R.string.scope_project_edit_title),
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(),
        onConfirm = {
            onSave(
                name,
                note,
                start.takeIf(String::isNotBlank)?.let(LocalDate::parse),
                end.takeIf(String::isNotBlank)?.let(LocalDate::parse),
            )
        },
    ) {
        OutlinedTextField(
            name,
            { name = it },
            label = { Text(stringResource(R.string.scope_project_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            note,
            { note = it },
            label = { Text(stringResource(R.string.scope_project_note)) },
            supportingText = { Text(stringResource(R.string.scope_project_note_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        DateField(value = start, label = stringResource(R.string.scope_project_start), clearable = true) { start = it }
        DateField(value = end, label = stringResource(R.string.scope_project_end), clearable = true) { end = it }
    }
}

@Composable
private fun ConfirmRemoveSheet(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    MankeuSheet(
        title = stringResource(R.string.scope_remove_title, name),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.scope_remove),
        destructive = true,
        onConfirm = onConfirm,
    ) {
        Text(stringResource(R.string.scope_remove_body))
    }
}

@Composable
private fun AddBusinessDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }

    MankeuSheet(
        title = stringResource(R.string.dialog_add_business),
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank(),
        onConfirm = { onSave(name.trim()) },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.dialog_business_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // The sector picker that used to sit here is gone (D-063). Every business now starts from
        // one general category list, and the list is editable from Pengaturan, so there is nothing
        // to get wrong at creation time and nothing that cannot be changed later.
        Text(
            stringResource(R.string.dialog_business_categories_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
