package com.mankeu.sync

import com.mankeu.backup.PasswordBackupCipher
import com.mankeu.backup.PortableBackupCodec

/** One encrypted change set as the remote store lists it. */
data class RemoteChangeSet(val id: String, val deviceId: String, val pushNumber: Long)

/**
 * Where change sets live. The production store is Google Drive's `appDataFolder`
 * ([com.mankeu.backup.cloud.GoogleDriveBackupClient.asSyncTransport]); tests use an in-memory one.
 * The store only ever sees ciphertext.
 */
interface SyncTransport {
    suspend fun list(): List<RemoteChangeSet>
    suspend fun upload(deviceId: String, pushNumber: Long, encrypted: ByteArray)
    suspend fun download(id: String): ByteArray
}

/**
 * One sync round (DEC-013): pull and apply every other device's change sets this device has not
 * applied yet, in push order, then push this device's pending changes.
 *
 * Run by [SyncController] (DEC-024): on app start, a few seconds after a local change, and from the
 * "Sinkronkan sekarang" button, while sync is switched on.
 */
class SyncCoordinator(
    private val engine: SyncEngine,
    private val transport: SyncTransport,
) {
    data class Report(val pulledSets: Int, val applied: Int, val skippedOlder: Int, val rejected: Int, val pushed: Boolean, val merged: Int = 0)

    suspend fun syncOnce(passphrase: CharArray): Report {
        val me = engine.deviceId()
        var pulled = 0
        var applied = 0
        var older = 0
        var rejected = 0
        var merged = 0
        transport.list()
            .filter { it.deviceId != me }
            .filter { it.pushNumber > engine.remoteCursor(it.deviceId) }
            .sortedWith(compareBy({ it.deviceId }, { it.pushNumber }))
            .forEach { remote ->
                val changeSet = PortableBackupCodec.decode(PasswordBackupCipher.decrypt(transport.download(remote.id), passphrase))
                val header = engine.header(changeSet)
                require(header.deviceId == remote.deviceId && header.pushNumber == remote.pushNumber) {
                    "Isi perubahan tidak cocok dengan namanya; berkas dilewati demi keamanan."
                }
                val result = engine.applyRemote(changeSet)
                engine.setRemoteCursor(remote.deviceId, remote.pushNumber)
                pulled++
                applied += result.applied
                older += result.skippedOlder
                rejected += result.rejected
                merged += result.merged
            }
        val local = engine.collectLocalChanges()
        if (local != null) {
            transport.upload(me, local.pushNumber, PasswordBackupCipher.encrypt(PortableBackupCodec.encode(local.changeSet), passphrase))
            engine.acknowledge(local)
        }
        return Report(pulled, applied, older, rejected, pushed = local != null, merged = merged)
    }
}
