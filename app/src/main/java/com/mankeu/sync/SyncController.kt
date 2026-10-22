package com.mankeu.sync

import androidx.room.withTransaction
import com.mankeu.backup.PasswordBackupCipher
import com.mankeu.data.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

/**
 * Sync as the owner sees it (DEC-024): switched on once per phone with the sync passphrase, then run
 * on app start, a few seconds after a change and from "Sinkronkan sekarang". One round at a time.
 * Merges and skipped edits are reported as a notice on the phone where they happened.
 *
 * [transportFor] builds the store for the signed-in account (Drive in the app, memory in tests); its
 * argument is the account e-mail to fall back on when Google Sign-In has no cached account.
 */
class SyncController(
    private val database: AppDatabase,
    private val engine: SyncEngine,
    private val passphrases: SyncPassphraseStore,
    private val transportFor: (fallbackEmail: String?) -> SyncTransport,
) {
    sealed interface State {
        data object Off : State
        data object Running : State
        data class Ready(val lastSyncAt: Long?) : State
        /** [cause] is a [WrongSyncPassphrase], a [SyncPassphraseLost], or whatever the round threw. */
        data class Failed(val cause: Throwable, val lastSyncAt: Long?) : State
    }

    private val _state = MutableStateFlow<State>(State.Off)
    val state: StateFlow<State> = _state
    private val running = Mutex()

    suspend fun refresh() {
        _state.value = if (engine.isEnabled() && passphrases.exists()) State.Ready(lastSyncAt()) else State.Off
    }

    /**
     * Checks [passphrase] against a change set another phone already stored, then switches sync on.
     * Only a failed decryption is a [WrongSyncPassphrase]; a download error is thrown as itself and
     * nothing is switched on. The first round is the caller's to start, so its failure (a network
     * blip) leaves sync on instead of throwing the passphrase away.
     */
    suspend fun enable(passphrase: CharArray, fallbackEmail: String?) {
        val transport = transportFor(fallbackEmail)
        val me = engine.deviceId()
        transport.list().firstOrNull { it.deviceId != me }?.let { other ->
            val encrypted = transport.download(other.id)
            runCatching { PasswordBackupCipher.decrypt(encrypted, passphrase) }.getOrElse { throw WrongSyncPassphrase() }
        }
        passphrases.save(passphrase)
        engine.enable()
        _state.value = State.Ready(lastSyncAt())
    }

    suspend fun disable() {
        engine.disable()
        passphrases.clear()
        _state.value = State.Off
    }

    /** One round; returns null when sync is off or a round is already running. */
    suspend fun syncNow(fallbackEmail: String?): SyncCoordinator.Report? {
        if (!engine.isEnabled()) return null
        if (!running.tryLock()) return null
        var passphrase: CharArray? = null
        return try {
            passphrase = passphrases.load() ?: throw SyncPassphraseLost()
            _state.value = State.Running
            val report = SyncCoordinator(engine, transportFor(fallbackEmail)).syncOnce(passphrase)
            val now = System.currentTimeMillis()
            database.syncDao().setMeta(LAST_SYNC_AT, now.toString())
            notice(report)
            _state.value = State.Ready(now)
            report
        } catch (cancelled: CancellationException) {
            // Not a failure: the app is closing or the round was superseded.
            _state.value = State.Ready(lastSyncAt())
            throw cancelled
        } catch (failure: Exception) {
            _state.value = State.Failed(failure, lastSyncAt())
            throw failure
        } finally {
            passphrase?.fill(' ')
            running.unlock()
        }
    }

    private suspend fun lastSyncAt(): Long? = database.syncDao().meta(LAST_SYNC_AT)?.toLongOrNull()

    /**
     * Written with change capture off: it describes this phone's merge, not something to send on.
     * Stored in Indonesian like the app's other SYSTEM notices (data rows, not UI strings).
     */
    private suspend fun notice(report: SyncCoordinator.Report) {
        val lines = buildList {
            if (report.merged > 0) add("• ${report.merged} kategori, barang, atau produk dengan nama sama dari HP lain digabung dengan yang ada di HP ini.")
            if (report.skippedOlder > 0) add("• ${report.skippedOlder} perubahan dari HP lain tidak dipakai karena di HP ini ada perubahan yang lebih baru pada catatan yang sama.")
            if (report.rejected > 0) add("• ${report.rejected} perubahan dari HP lain tidak bisa diterapkan.")
        }
        if (lines.isEmpty()) return
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            SyncSchema.quietly(db) {
                db.execSQL(
                    "INSERT INTO import_notices(id, batchId, severity, title, explanation, amountRupiah, sourceEvidence, reviewed, action, createdAtEpochMillis) " +
                        "VALUES (?, NULL, 'SYSTEM', ?, ?, NULL, 'Sinkronisasi', 0, NULL, ?)",
                    arrayOf<Any>(UUID.randomUUID().toString(), "Hasil sinkronisasi perlu dicek", lines.joinToString("\n"), System.currentTimeMillis()),
                )
            }
        }
    }

    private companion object {
        const val LAST_SYNC_AT = "last_sync_at"
    }
}
