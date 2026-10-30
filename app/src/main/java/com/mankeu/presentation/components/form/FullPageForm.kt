package com.mankeu.presentation.components.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.mankeu.R
import com.mankeu.presentation.MankeuType
import com.mankeu.presentation.Space
import com.mankeu.presentation.components.MankeuSheet

/**
 * The page every form with more than three fields uses (audit P2 §8): ← and title on top, the
 * fields in a scrolling column, the live [ResultBar] and a wide Simpan pinned at the bottom above
 * the keyboard.
 *
 * System Back and ← go through the same path: closed at once when nothing was entered, otherwise
 * "Buang perubahan?". The [BackHandler] is composed after the shell's own handler in `MankeuApp`,
 * so it is the one that runs while the page is open; no screen has to remember its own.
 */
@Composable
fun FullPageForm(
    title: String,
    dirty: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = true,
    result: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val close = { if (dirty) confirmDiscard = true else onClose() }
    BackHandler(onBack = close)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { PageTopBar(title, onBack = close) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = Space.Edge, vertical = Space.Small),
                verticalArrangement = Arrangement.spacedBy(Space.Small),
            ) {
                result?.invoke()
                // The label is always "Simpan" (P2 §8): one word for one action on every form.
                Button(onClick = onSave, enabled = saveEnabled, modifier = Modifier.fillMaxWidth().heightIn(min = Space.Row)) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.Edge, vertical = Space.Field),
            verticalArrangement = Arrangement.spacedBy(Space.Field),
            content = content,
        )
    }
    if (confirmDiscard) {
        MankeuSheet(
            title = stringResource(R.string.form_discard_title),
            onDismiss = { confirmDiscard = false },
            confirmLabel = stringResource(R.string.form_discard_confirm),
            dismissLabel = stringResource(R.string.form_discard_keep),
            destructive = true,
            onConfirm = { confirmDiscard = false; onClose() },
        ) { Text(stringResource(R.string.form_discard_body)) }
    }
}

/** The live result above Simpan (P2 §7): "Total Rp600.000" with "untung Rp120.000" beside it. */
@Composable
fun ResultBar(text: String, modifier: Modifier = Modifier, detail: String? = null) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraSmall) {
        Column(Modifier.padding(horizontal = Space.Edge, vertical = Space.Small)) {
            Text(text, style = MankeuType.Body, fontWeight = FontWeight.SemiBold)
            if (detail != null) Text(detail, style = MankeuType.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** ← and title, shared by [FullPageForm] and [PickerPage]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, style = MankeuType.Title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}
