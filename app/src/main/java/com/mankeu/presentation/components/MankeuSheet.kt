package com.mankeu.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback

/**
 * The app's one modal surface.
 *
 * Everything that asks a question used to be an `AlertDialog` — a box in the middle of the screen
 * with its actions at the top of the thumb's reach — while the transaction form was already a
 * bottom sheet. Two different answers to the same question in one app. This is the single
 * definition.
 *
 * Three things about its layout are deliberate:
 *
 * - **The actions never scroll away.** The content column takes the space that is left over
 *   (`weight(1f, fill = false)`, so a short sheet still wraps its content) and scrolls inside it,
 *   while the action row sits below it. Save and Batal are therefore always on screen,
 *   including with the keyboard up.
 * - **Only the buttons, back and the scrim close it.** Dragging is off (sheetGesturesEnabled = false),
 *   so scrolling a long form back to the top never drags the sheet away; there is no drag handle
 *   to suggest otherwise.
 * - **The keyboard pushes, it does not cover.** imePadding() shrinks the sheet by the keyboard's
 *   height and the scrollable content brings the focused field into view. The content column is
 *   not animateContentSize'd: that animation chased every frame of the keyboard's own animation
 *   and made the sheet bounce.
 *
 * [confirmLabel] `null` renders a single dismiss action, for sheets that only present information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MankeuSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String? = stringResource(R.string.action_save),
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissLabel: String = stringResource(R.string.action_cancel),
    // Multi-step sheets need the dismiss button to step back while the scrim and back still close it.
    onDismissClick: () -> Unit = onDismiss,
    onConfirm: (() -> Unit)? = null,
    // Left of the row, apart from Batal/Simpan: e.g. Hapus in LineEditSheet.
    startAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Kept internal: `rememberModalBottomSheetState` is experimental, and as a default parameter it
    // is evaluated at every call site, which would force @OptIn on every caller.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val feedback = LocalFeedback.current
    ModalBottomSheet(
        // Scrim tap and back all land here; the buttons below call their own handlers.
        onDismissRequest = { feedback(FeedbackEvent.TAP); onDismiss() },
        sheetState = sheetState,
        modifier = modifier,
        sheetGesturesEnabled = false,
        dragHandle = null,
        // Material 3's own bottom-sheet container tone. Stated rather than left to the default so
        // the sheet keeps a visible step above the page in both themes without a drop shadow,
        // which is a light-mode device and turns into a hard rim on a dark surface.
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                startAction?.invoke()
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismissClick) { Text(dismissLabel) }
                if (confirmLabel != null && onConfirm != null) {
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        colors = if (destructive) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                    ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
