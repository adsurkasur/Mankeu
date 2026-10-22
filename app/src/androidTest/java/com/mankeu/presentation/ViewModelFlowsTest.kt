package com.mankeu.presentation

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.auth.FirebaseAuthManager
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.AppDatabase
import com.mankeu.data.FinanceRepository
import com.mankeu.importer.FinanceWorkbookImporter
import com.mankeu.sync.RemoteChangeSet
import com.mankeu.sync.SyncController
import com.mankeu.sync.SyncEngine
import com.mankeu.sync.SyncPassphraseStore
import com.mankeu.sync.SyncSchema
import com.mankeu.sync.SyncTransport
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Collections

/**
 * The ViewModel paths the review found untested (2026-10-22): sync runs by itself on start and
 * after a change, and an import preview that was replaced or cancelled never overwrites the
 * current one. Real database, real XLSX files, the sync store in memory.
 */
@RunWith(AndroidJUnit4::class)
class ViewModelFlowsTest {
    private class Store : SyncTransport {
        val uploads: MutableList<RemoteChangeSet> = Collections.synchronizedList(mutableListOf())
        override suspend fun list() = uploads.toList()
        override suspend fun upload(deviceId: String, pushNumber: Long, encrypted: ByteArray) {
            uploads += RemoteChangeSet("$deviceId-$pushNumber", deviceId, pushNumber)
        }
        override suspend fun download(id: String) = error("one phone only")
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private val passphrases = SyncPassphraseStore(ApplicationProvider.getApplicationContext(), "sync-test-vm")
    private val store = Store()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(SyncSchema.callback).build()
        repository = FinanceRepository(database)
    }

    @After
    fun tearDown() {
        passphrases.clear()
        database.close()
    }

    @Test
    fun syncRunsOnStartAndAgainShortlyAfterAChange() = runBlocking {
        val sync = SyncController(database, SyncEngine(database), passphrases) { store }
        sync.enable("sandi-sync-uji".toCharArray(), null)

        val viewModel = viewModel(sync)
        waitFor("first round on start") { store.uploads.size == 1 }

        mainThread { viewModel.createBusiness("Warung Bu Ani", "Utama") }
        waitFor("round about 5 s after the change", timeoutMillis = 15_000) { store.uploads.size == 2 }
        assertTrue(sync.state.value is SyncController.State.Ready)
    }

    @Test
    fun aReplacedPreviewNeverOverwritesTheNewOne() {
        // Created before the ViewModel, which then opens it on start as the app does.
        runBlocking { repository.createBusinessWithUnit("Penggilingan", "Utama") }
        val viewModel = viewModel(null)
        waitFor("unit selected on start") { runBlocking { repository.currentWorkspace(null).projectId } != null }
        Thread.sleep(500)
        val big = asset("N1_penggilingan_Pabrik_Klaten.xlsx")
        val small = asset("M1_warung_Warung_Bu_Ani.xlsx")

        mainThread {
            viewModel.previewWorkbook(context.contentResolver, Uri.fromFile(big))
            viewModel.previewWorkbook(context.contentResolver, Uri.fromFile(small))
        }
        waitFor("small preview ready") { viewModel.importState.value is ImportUiState.Ready }
        Thread.sleep(8_000) // long enough for the big file's parse to have finished

        val state = viewModel.importState.value
        assertTrue("got $state", state is ImportUiState.Ready)
        assertEquals(547, (state as ImportUiState.Ready).preview.transactions.size)
    }

    @Test
    fun cancellingAPreviewLeavesTheImportIdle() {
        val viewModel = viewModel(null)
        val big = asset("N1_penggilingan_Pabrik_Klaten.xlsx")

        mainThread {
            viewModel.previewWorkbook(context.contentResolver, Uri.fromFile(big))
            viewModel.resetImportState()
        }
        Thread.sleep(8_000)

        assertEquals(ImportUiState.Idle, viewModel.importState.value)
    }

    private fun viewModel(sync: SyncController?): AppViewModel {
        lateinit var created: AppViewModel
        mainThread {
            created = AppViewModel(
                repository,
                FinanceWorkbookImporter(),
                EncryptedBackupManager(database, File(context.cacheDir, "vm-flows-recovery.aabk")),
                FirebaseAuthManager(),
                GoogleDriveBackupClient(),
                syncController = sync,
            )
        }
        return created
    }

    private fun asset(name: String): File = File(context.cacheDir, name).also { file ->
        InstrumentationRegistry.getInstrumentation().context.assets.open("simulasi/$name").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
    }

    private fun mainThread(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private fun waitFor(what: String, timeoutMillis: Long = 10_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            if (System.currentTimeMillis() > end) throw AssertionError("timed out waiting for $what")
            Thread.sleep(100)
        }
    }
}
