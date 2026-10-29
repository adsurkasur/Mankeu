package com.mankeu.sync

/** Another phone's change set does not open with this phone's sync passphrase. The UI words it. */
class WrongSyncPassphrase : IllegalArgumentException("sync passphrase mismatch")

/**
 * A change set from a phone below [SyncEngine.MIN_SYNC_SCHEMA] (DEC-029). The round skips it instead of
 * stopping on it forever; that phone republishes everything once it is updated (Migration13To14).
 */
class OutdatedChangeSet : IllegalArgumentException("Perubahan dari perangkat lain memakai versi aplikasi lama. Perbarui aplikasi di semua HP, lalu sinkronkan lagi.")

/** The stored passphrase cannot be read (new phone, reset Keystore); sync must be switched on again. */
class SyncPassphraseLost : IllegalStateException("sync passphrase unavailable")
