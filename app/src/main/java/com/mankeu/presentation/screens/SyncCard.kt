package com.mankeu.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mankeu.R
import com.mankeu.presentation.appLocale
import com.mankeu.presentation.components.MankeuSheet
import com.mankeu.sync.SyncController
import com.mankeu.sync.SyncPassphraseLost
import com.mankeu.sync.WrongSyncPassphrase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * "Sinkronisasi antar-HP" in Pengaturan (DEC-024): how to switch it on, whether it is on, when it
 * last ran, and what went wrong. Everything else happens on its own.
 */
@Composable
fun SyncCard(
    state: SyncController.State,
    signedIn: Boolean,
    onSignIn: () -> Unit,
    onEnable: (CharArray) -> Unit,
    onDisable: () -> Unit,
    onSyncNow: () -> Unit,
) {
    var askPassphrase by remember { mutableStateOf(false) }
    var confirmOff by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.sync_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            when (state) {
                SyncController.State.Off -> {
                    Text(stringResource(R.string.sync_off_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (signedIn) {
                        Button(onClick = { askPassphrase = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sync_turn_on)) }
                    } else {
                        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sync_sign_in_first)) }
                    }
                }
                SyncController.State.Running -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.sync_running))
                }
                is SyncController.State.Ready -> {
                    Text(lastSyncText(state.lastSyncAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SyncActions(onSyncNow) { confirmOff = true }
                }
                is SyncController.State.Failed -> {
                    val reason = when (state.cause) {
                        is WrongSyncPassphrase -> stringResource(R.string.sync_wrong_passphrase)
                        is SyncPassphraseLost -> stringResource(R.string.sync_passphrase_lost)
                        else -> stringResource(R.string.sync_failed, state.cause.message ?: state.cause.javaClass.simpleName)
                    }
                    Text(reason, color = MaterialTheme.colorScheme.error)
                    Text(lastSyncText(state.lastSyncAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SyncActions(onSyncNow) { confirmOff = true }
                }
            }
        }
    }

    if (askPassphrase) {
        var first by remember { mutableStateOf("") }
        var second by remember { mutableStateOf("") }
        val valid = first.length >= 8 && first == second
        MankeuSheet(
            title = stringResource(R.string.sync_passphrase_title),
            onDismiss = { askPassphrase = false },
            confirmLabel = stringResource(R.string.sync_turn_on),
            confirmEnabled = valid,
            onConfirm = { askPassphrase = false; onEnable(first.toCharArray()) },
        ) {
            Text(stringResource(R.string.sync_passphrase_body))
            OutlinedTextField(
                value = first,
                onValueChange = { first = it },
                label = { Text(stringResource(R.string.sync_passphrase)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = second,
                onValueChange = { second = it },
                label = { Text(stringResource(R.string.sync_passphrase_repeat)) },
                singleLine = true,
                isError = second.isNotEmpty() && second != first,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (confirmOff) {
        MankeuSheet(
            title = stringResource(R.string.sync_turn_off_title),
            onDismiss = { confirmOff = false },
            confirmLabel = stringResource(R.string.sync_turn_off),
            destructive = true,
            onConfirm = { confirmOff = false; onDisable() },
        ) {
            Text(stringResource(R.string.sync_turn_off_body))
        }
    }
}

@Composable
private fun SyncActions(onSyncNow: () -> Unit, onTurnOff: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onSyncNow, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.sync_now)) }
        OutlinedButton(onClick = onTurnOff) { Text(stringResource(R.string.sync_turn_off)) }
    }
}

@Composable
private fun lastSyncText(at: Long?): String = if (at == null) {
    stringResource(R.string.sync_never)
} else {
    stringResource(
        R.string.sync_last,
        DateTimeFormatter.ofPattern("d MMM, HH.mm", appLocale()).format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())),
    )
}
