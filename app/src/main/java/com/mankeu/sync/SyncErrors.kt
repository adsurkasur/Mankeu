package com.mankeu.sync

/** Another phone's change set does not open with this phone's sync passphrase. The UI words it. */
class WrongSyncPassphrase : IllegalArgumentException("sync passphrase mismatch")

/** The stored passphrase cannot be read (new phone, reset Keystore); sync must be switched on again. */
class SyncPassphraseLost : IllegalStateException("sync passphrase unavailable")
