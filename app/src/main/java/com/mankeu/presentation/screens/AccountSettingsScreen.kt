package com.mankeu.presentation.screens

import android.content.Intent
import android.widget.Toast
import com.mankeu.presentation.components.MankeuAppLogo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.DeleteForever
import com.mankeu.presentation.components.MankeuSheet
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.core.net.toUri
import com.mankeu.BuildConfig
import com.mankeu.R
import com.mankeu.presentation.AppLanguage
import com.mankeu.presentation.LocalMankeuSemanticColors
import com.mankeu.presentation.FeedbackEvent
import com.mankeu.presentation.LocalFeedback
import com.mankeu.presentation.MotionMode
import com.mankeu.presentation.ThemeMode

import com.mankeu.auth.AuthUser
import com.mankeu.sync.SyncController
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Login

/**
 * Pengaturan: appearance, language, motion, and the account settings.
 *
 * Deliberately does **not** host Excel import/export or backup any more. The owner's review
 * called that placement out directly ("impor workbook di pengaturan, itu kaya aneh") — bringing
 * a season's records in from a spreadsheet is a data task done while working, not a
 * configuration change. Those actions now live on their own Berkas dan Data screen.
 *
 * It no longer carries a shortcut card to that screen either. The folder icon in the scope header
 * is on every main screen, so a second full-width entry point here was a duplicate route to a
 * destination the owner is never more than one tap from.
 */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    motionMode: MotionMode,
    onMotionModeChange: (MotionMode) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    accountName: String,
    onAccountNameChange: (String) -> Unit,
    authUser: AuthUser?,
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    /** "Hapus semua data" (DEC-020); the flag says whether Drive and the sign-in account go too. */
    onEraseAllData: (includeCloud: Boolean) -> Unit,
    isErasing: Boolean,
    syncState: SyncController.State,
    onEnableSync: (CharArray) -> Unit,
    onDisableSync: () -> Unit,
    onSyncNow: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // No SectionTitle here: OverlayScaffold already shows "Pengaturan" in the TopAppBar.
        item {
            AccountCard(
                accountName = accountName,
                onAccountNameChange = onAccountNameChange,
                authUser = authUser,
                onSignInGoogle = onSignInGoogle,
                onSignOut = onSignOut,
            )
        }
        item {
            SyncCard(
                state = syncState,
                signedIn = authUser != null,
                onSignIn = onSignInGoogle,
                onEnable = onEnableSync,
                onDisable = onDisableSync,
                onSyncNow = onSyncNow,
            )
        }

        // Pusat Bantuan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.help_center_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    // Was an OutlinedButton: a hairline border on a surface-coloured card gave
                    // this the visual weight of a caption, and the owner reported not seeing it as
                    // an action at all. FilledTonalButton is Material 3's own answer for a
                    // secondary action that must still read as a button — filled enough to find,
                    // quiet enough not to compete with the screen's primary content.
                    FilledTonalButton(
                        onClick = onOpenHelpCenter,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Text(stringResource(R.string.help_open_center), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SettingCard(
                title = stringResource(R.string.settings_display),
            ) {
                SegmentedChoice(
                    options = ThemeMode.entries.map { it to stringResource(it.labelRes) },
                    selected = themeMode,
                    onSelect = onThemeModeChange,
                )
            }
        }

        item {
            SettingCard(stringResource(R.string.settings_language)) {
                LanguagePicker(language = language, onLanguageChange = onLanguageChange)
            }
        }

        item {
            SettingCard(stringResource(R.string.settings_motion)) {
                SegmentedChoice(
                    options = MotionMode.entries.map { it to stringResource(it.labelRes) },
                    selected = motionMode,
                    onSelect = onMotionModeChange,
                )
                Text(
                    stringResource(R.string.settings_motion_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SettingCard(stringResource(R.string.settings_feedback)) {
                ToggleRow(
                    title = stringResource(R.string.settings_haptics),
                    help = stringResource(R.string.settings_haptics_help),
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsChange,
                )
                ToggleRow(
                    title = stringResource(R.string.settings_sound),
                    help = stringResource(R.string.settings_sound_help),
                    checked = soundEnabled,
                    onCheckedChange = onSoundChange,
                )
            }
        }

        item {
            EraseAllDataCard(signedIn = authUser != null, isErasing = isErasing, onErase = onEraseAllData)
        }

        // Contact card. Sits directly above About so the two "who made this / how do I reach them"
        // questions are answered in one place at the bottom of the screen.
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_contact_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        stringResource(R.string.settings_contact_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MailOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_contact_email),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    val context = LocalContext.current
                    val email = stringResource(R.string.settings_contact_email)
                    val subject = stringResource(R.string.settings_contact_subject, BuildConfig.VERSION_NAME)
                    val noAppMessage = stringResource(R.string.settings_contact_no_app)
                    FilledTonalButton(
                        onClick = {
                            // ACTION_SENDTO with a mailto: URI, so only mail apps match — a plain
                            // ACTION_SEND would offer every share target on the phone.
                            val intent = Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())
                                .putExtra(Intent.EXTRA_SUBJECT, subject)
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, noAppMessage, Toast.LENGTH_LONG).show() }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Text(stringResource(R.string.settings_contact_btn), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // About Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MankeuAppLogo(
                        size = 56.dp,
                    )
                    Text(
                        stringResource(R.string.app_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.about_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Deleting everything is the one action here that cannot be undone, so it asks twice: a sheet
 * that names what goes, then the word typed out. Typing beats a second "Ya" button, which a thumb
 * taps through without reading.
 */
@Composable
private fun EraseAllDataCard(signedIn: Boolean, isErasing: Boolean, onErase: (Boolean) -> Unit) {
    val feedback = LocalFeedback.current
    var confirming by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_erase_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(
                stringResource(R.string.settings_erase_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { feedback(FeedbackEvent.WARN); confirming = true },
                enabled = !isErasing,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_erase_btn), fontWeight = FontWeight.Bold)
            }
        }
    }
    if (confirming) {
        var includeCloud by rememberSaveable { mutableStateOf(true) }
        var typed by rememberSaveable { mutableStateOf("") }
        val word = stringResource(R.string.dialog_erase_word)
        MankeuSheet(
            title = stringResource(R.string.dialog_erase_title),
            onDismiss = { confirming = false },
            confirmLabel = stringResource(R.string.action_erase),
            confirmEnabled = typed.trim() == word && !isErasing,
            destructive = true,
            onConfirm = {
                onErase(signedIn && includeCloud)
                confirming = false
            },
        ) {
            Text(stringResource(R.string.dialog_erase_body))
            if (signedIn) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = includeCloud, role = Role.Checkbox, onValueChange = { includeCloud = it }),
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(checked = includeCloud, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.dialog_erase_cloud), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.dialog_erase_cloud_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = { Text(stringResource(R.string.dialog_erase_type, word)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AccountCard(
    accountName: String,
    onAccountNameChange: (String) -> Unit,
    authUser: AuthUser?,
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
) {
    val semantic = LocalMankeuSemanticColors.current
    val feedback = LocalFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        val avatarText = authUser?.displayName?.take(1)
                            ?: authUser?.email?.take(1)
                            ?: accountName.trim().take(1)
                        if (avatarText.isBlank()) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(
                                avatarText.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        authUser?.displayName ?: accountName.ifBlank { stringResource(R.string.account_title) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    if (authUser != null) {
                        Text(
                            authUser.email ?: stringResource(R.string.account_signed_in_as, authUser.uid.take(8)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = semantic.success,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.account_drive_active),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = semantic.success,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.account_signed_out_status),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = accountName,
                onValueChange = onAccountNameChange,
                label = { Text(stringResource(R.string.account_local_name)) },
                supportingText = { Text(stringResource(R.string.account_local_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (authUser == null) {
                Button(
                    onClick = {
                        feedback(FeedbackEvent.TAP)
                        onSignInGoogle()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.account_sign_in_google), fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.account_prototype_notice),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                stringResource(R.string.account_offline_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        feedback(FeedbackEvent.TAP)
                        onSignOut()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.account_sign_out), fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = semantic.successContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = semantic.onSuccessContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.account_prototype_notice),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.labelLarge,
                                color = semantic.onSuccessContainer,
                            )
                            Text(
                                stringResource(R.string.account_drive_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.onSuccessContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A labelled switch with its explanation underneath.
 *
 * The whole row toggles, not just the switch: a 48dp switch is a small target on a phone held in
 * one hand, and `toggleable` on the row keeps one accessibility node with the right state instead
 * of a label and a control that TalkBack reads as unrelated.
 */
@Composable
private fun ToggleRow(
    title: String,
    help: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val feedback = LocalFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { feedback(FeedbackEvent.TAP); onCheckedChange(it) },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(help, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Root cause of the owner-reported non-uniform Pengaturan card widths: this Card had no
    // modifier at all. Its content (a title Text plus a choice row with no fillMaxWidth of its own)
    // never asked for full width, so Compose sized the card to its intrinsic content — narrower
    // than AccountCard above it, which only happened to end up full-width because an internal
    // OutlinedTextField/Button forced fillMaxWidth incidentally. Pinning it explicitly here (and
    // on AccountCard) makes every Pengaturan card the same width for the same reason, not by
    // accident of unrelated content.
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            content()
        }
    }
}

/**
 * Material 3's own single-select control for a small, mutually exclusive set. Replaces a row of
 * `FilterChip`s, which are a *filter* affordance (multi-select, independently togglable) and
 * therefore said the wrong thing about a setting where exactly one value is always active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedChoice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Medium,
                    )
                },
            )
        }
    }
}

/**
 * Language selection as Material 3's exposed dropdown menu, which is the platform convention for
 * picking one value out of a named list. Filter chips were used here before; besides the
 * wrong-affordance problem above, a chip row does not scale — a third language would have wrapped
 * or scrolled off the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePicker(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = language.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.settings_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        if (option != language) onLanguageChange(option)
                    },
                    trailingIcon = {
                        if (option == language) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                )
            }
        }
    }
}
