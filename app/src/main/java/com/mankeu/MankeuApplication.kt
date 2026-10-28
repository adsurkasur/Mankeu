package com.mankeu

import android.app.Application
import com.mankeu.auth.DriveTokenProvider
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.sync.SyncController
import com.mankeu.sync.SyncEngine
import com.mankeu.sync.SyncPassphraseStore
import java.io.File

class MankeuApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val workbookImporter: FinanceWorkbookImporter by lazy { FinanceWorkbookImporter() }
    val backupManager: EncryptedBackupManager by lazy {
        EncryptedBackupManager(database, File(noBackupFilesDir, "umkm-before-restore.aabk"))
    }
    val identity by lazy { GoogleIdentity(this) }
    val cloudBackupClient by lazy { GoogleDriveBackupClient() }
    val syncController by lazy {
        val tokens = DriveTokenProvider(this)
        SyncController(database, SyncEngine(database), SyncPassphraseStore(this)) { email -> cloudBackupClient.asSyncTransport { tokens.token(email) } }
    }
}
