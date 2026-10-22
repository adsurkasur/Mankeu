package com.mankeu.sync

import androidx.room.withTransaction
import com.mankeu.auth.DriveTokenProvider
import com.mankeu.backup.PasswordBackupCipher
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

/**
 * Sync as the owner sees it (DEC-024): switched on once per phone with the sync passphrase, then run
 * on app start, a few seconds after a change and from "Sinkronkan sekarang". One round at a time.
 * Merges and skipped edits are reported as a notice on the phone where they happened.
 */
class SyncController(
    private val database: AppDatabase,
    private val engine: SyncEngine,
    private val drive: GoogleDriveBackupClient,
    private val tokens: DriveTokenProvider,
    private val passphrases: SyncPassphraseStore,
) {
    sealed interface State {
        data object Off : State
        data object Running : State
        data class Ready(val lastSyncAt: Long?) : State
        data class Failed(val message: String, val lastSyncAt: Long?) : State
    }

    private val _state = MutableStateFlow<State>(State.Off)
    val state: StateFlow<State> = _state
    private val running = Mutex()

    suspend fun refresh() {
        _state.value = if (engine.isEnabled() && passphrases.exists()) State.Ready(lastSyncAt()) else State.Off
    }

    /**
     * Checks [passphrase] against what other phones already stored, then switches sync on. A
     * second phone with a different passphrase is refused here, before anything is written.
     */
    suspend fun enable(passphrase: CharArray, fallbackEmail: String?) {
        val transport = drive.asSyncTransport { tokens.token(fallbackEmail) }
        val me = engine.deviceId()
        transport.list().firstOrNull { it.deviceId != me }?.let { other ->
            runCatching { PasswordBackupCipher.decrypt(transport.download(other.id), passphrase) }
                .onFailure { throw WrongSyncPassphrase() }
        }
        passphrases.save(passphrase)
        engine.enable()
        syncNow(fallbackEmail)
    }

    suspend fun disable() {
        engine.disable()
        passphrases.clear()
        _state.value = State.Off
    }

    /** One round; returns null when sync is off or a round is already running. */
    suspend fun syncNow(fallbackEmail: String?): SyncCoordinator.Report? {
        if (!engine.isEnabled()) return null
        val passphrase = passphrases.load() ?: run {
            _state.value = State.Failed(PASSPHRASE_LOST, lastSyncAt())
            return null
        }
        if (!running.tryLock()) return null
        return try {
            _state.value = State.Running
            val report = SyncCoordinator(engine, drive.asSyncTransport { tokens.token(fallbackEmail) }).syncOnce(passphrase)
            val now = System.currentTimeMillis()
            database.syncDao().setMeta(LAST_SYNC_AT, now.toString())
            notice(report)
            _state.value = State.Ready(now)
            report
        } catch (failure: Exception) {
            _state.value = State.Failed(failure.message ?: failure.javaClass.simpleName, lastSyncAt())
            throw failure
        } finally {
            passphrase.fill(' ')
            running.unlock()
        }
    }

    private suspend fun lastSyncAt(): Long? = database.syncDao().meta(LAST_SYNC_AT)?.toLongOrNull()

    /** Written with change capture off: it describes this phone's merge, not something to send on. */
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

    class WrongSyncPassphrase : IllegalArgumentException(
        "Kata sandi sync tidak cocok dengan yang dipakai HP lain. Gunakan kata sandi sync yang sama di semua HP.",
    )

    private companion object {
        const val LAST_SYNC_AT = "last_sync_at"
        const val PASSPHRASE_LOST = "Kata sandi sync perlu diketik ulang di HP ini. Matikan lalu nyalakan sync lagi."
    }
}
