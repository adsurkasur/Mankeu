package com.mankeu.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.data.AppDatabase
import com.mankeu.data.EventDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

/**
 * Sync as the owner meets it (DEC-024): switching on, a wrong passphrase, a dropped connection, and
 * a round cancelled in the middle of an upload. Two phones with their own database and passphrase
 * store, one store in memory that can be told to fail or to hold an upload.
 */
@RunWith(AndroidJUnit4::class)
class SyncControllerTest {
    private class Store : SyncTransport {
        val files = linkedMapOf<String, Pair<RemoteChangeSet, ByteArray>>()
        var failDownloads = false
        var failUploads = false
        /** When set, the next upload waits for it, then completes (and stores) normally. */
        var hold: CompletableDeferred<Unit>? = null
        val uploadStarted = CompletableDeferred<Unit>()

        override suspend fun list() = files.values.map { it.first }
        override suspend fun upload(deviceId: String, pushNumber: Long, encrypted: ByteArray) {
            if (failUploads) throw IOException("koneksi putus")
            hold?.let { uploadStarted.complete(Unit); it.await() }
            val id = "$deviceId-$pushNumber"
            files[id] = RemoteChangeSet(id, deviceId, pushNumber) to encrypted
        }
        override suspend fun download(id: String): ByteArray {
            if (failDownloads) throw IOException("koneksi putus")
            return files.getValue(id).second
        }
    }

    private inner class Phone(name: String) {
        val database: AppDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(SyncSchema.callback)
            .build()
        val repository = FinanceRepository(database)
        val passphrases = SyncPassphraseStore(ApplicationProvider.getApplicationContext(), "sync-test-$name")
        val controller = SyncController(database, SyncEngine(database), passphrases) { store }
    }

    private val store = Store()
    private lateinit var a: Phone
    private lateinit var b: Phone
    private val secret = "sandi-sync-uji"

    @Before
    fun setUp() {
        a = Phone("a")
        b = Phone("b")
    }

    @After
    fun tearDown() {
        a.passphrases.clear()
        b.passphrases.clear()
        a.database.close()
        b.database.close()
    }

    @Test
    fun aSecondPhoneWithTheSamePassphraseReceivesTheFirstPhonesBooks() = runBlocking {
        val (business, unit) = a.repository.createBusinessWithUnit("Warung", "Utama")
        a.record(business, unit, 650_000)
        a.controller.enable(secret.toCharArray(), null)
        a.controller.syncNow(null)

        b.controller.enable(secret.toCharArray(), null)
        b.controller.syncNow(null)

        assertEquals(650_000L, b.cash(unit))
        assertTrue(b.controller.state.value is SyncController.State.Ready)
    }

    @Test
    fun aDifferentPassphraseIsRefusedAndNothingIsSwitchedOn() = runBlocking {
        a.repository.createBusiness("Warung")
        a.controller.enable(secret.toCharArray(), null)
        a.controller.syncNow(null)

        val failure = runCatching { b.controller.enable("sandi-lain-sekali".toCharArray(), null) }.exceptionOrNull()

        assertTrue("got $failure", failure is WrongSyncPassphrase)
        assertFalse(b.passphrases.exists())
        assertNull(b.controller.syncNow(null))
        assertTrue(b.database.financeDao().getBusinesses().isEmpty())
    }

    @Test
    fun aDroppedConnectionWhileCheckingIsNotCalledAWrongPassphrase() = runBlocking {
        a.repository.createBusiness("Warung")
        a.controller.enable(secret.toCharArray(), null)
        a.controller.syncNow(null)
        store.failDownloads = true

        val failure = runCatching { b.controller.enable(secret.toCharArray(), null) }.exceptionOrNull()

        assertTrue("got $failure", failure is IOException)
        assertFalse(b.passphrases.exists())
    }

    @Test
    fun aFailedFirstRoundKeepsSyncOnAndTheNextRoundDeliversEverything() = runBlocking {
        val (business, unit) = a.repository.createBusinessWithUnit("Warung", "Utama")
        a.record(business, unit, 100_000)
        a.controller.enable(secret.toCharArray(), null)
        store.failUploads = true

        assertTrue(runCatching { a.controller.syncNow(null) }.isFailure)
        assertTrue(a.controller.state.value is SyncController.State.Failed)
        assertTrue("passphrase kept", a.passphrases.exists())

        store.failUploads = false
        a.controller.syncNow(null)
        b.controller.enable(secret.toCharArray(), null)
        b.controller.syncNow(null)
        assertEquals(100_000L, b.cash(unit))
    }

    /**
     * Review 2026-10-22, bug 1: a round cancelled while its upload is in flight used to skip the
     * acknowledgement and reuse the push number, so a phone that had seen it never got the rest.
     */
    @Test
    fun aRoundCancelledDuringItsUploadStillCompletesAndLaterChangesArrive() = runBlocking {
        val (business, unit) = a.repository.createBusinessWithUnit("Warung", "Utama")
        a.controller.enable(secret.toCharArray(), null)
        a.controller.syncNow(null)
        b.controller.enable(secret.toCharArray(), null)
        b.controller.syncNow(null)

        a.record(business, unit, 300_000)
        store.hold = CompletableDeferred()
        val round = async(Dispatchers.Default) { a.controller.syncNow(null) }
        withTimeout(10_000) { store.uploadStarted.await() }
        round.cancel()
        store.hold!!.complete(Unit)
        runCatching { round.await() }
        store.hold = null

        assertTrue("acknowledged despite the cancel", a.database.syncDao().pending().isEmpty())
        assertFalse("a cancel is not a failure", a.controller.state.value is SyncController.State.Failed)
        b.controller.syncNow(null)
        a.record(business, unit, 50_000)
        a.controller.syncNow(null)
        b.controller.syncNow(null)
        assertEquals(350_000L, b.cash(unit))
    }

    @Test
    fun aRoundAlreadyRunningIsNotStartedTwice() = runBlocking {
        a.repository.createBusiness("Warung")
        a.controller.enable(secret.toCharArray(), null)
        store.hold = CompletableDeferred()
        val first = async(Dispatchers.Default) { a.controller.syncNow(null) }
        withTimeout(10_000) { store.uploadStarted.await() }

        assertNull(a.controller.syncNow(null))

        store.hold!!.complete(Unit)
        first.await()
        Unit
    }

    @Test
    fun aMergeOnThisPhoneIsReportedHereAndNotSentOn() = runBlocking {
        val (business, _) = a.repository.createBusinessWithUnit("Bengkel", "Utama")
        a.controller.enable(secret.toCharArray(), null)
        a.controller.syncNow(null)
        b.controller.enable(secret.toCharArray(), null)
        b.controller.syncNow(null)
        a.repository.createCategory(business, "Bensin Motor", com.mankeu.domain.CategoryRole.EXPENSE)
        b.repository.createCategory(business, "bensin motor", com.mankeu.domain.CategoryRole.EXPENSE)
        a.controller.syncNow(null)

        b.controller.syncNow(null)

        assertTrue(b.database.financeDao().observeNotices().first().any { it.title == "Hasil sinkronisasi perlu dicek" })
        assertTrue(b.database.syncDao().pending().none { it.tableName == "import_notices" })
    }

    private suspend fun Phone.record(business: String, unit: String, amount: Long) {
        val book = database.financeDao().getBooks(unit).first { it.mode == BookMode.REALIZATION.name }.id
        repository.recordEvent(EventDraft(business, unit, book, FinancialEventType.CASH_INCOME, amount, LocalDate.of(2026, 10, 22), "", ""))
    }

    private suspend fun Phone.cash(unit: String): Long {
        val book = database.financeDao().getBooks(unit).first { it.mode == BookMode.REALIZATION.name }.id
        return repository.accountingSummary(book).first().cashBalanceRupiah
    }
}
