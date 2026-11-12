package com.mankeu.presentation

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mankeu.auth.GoogleIdentity
import com.mankeu.backup.EncryptedBackupManager
import com.mankeu.backup.cloud.GoogleDriveBackupClient
import com.mankeu.data.*
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import com.mankeu.importer.FinanceWorkbookImporter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class LedgerWindowViewModelTest {
    @Test fun historyResetsOnUnitSwitchAndNeverLeaksPreviousUnitRows() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = FinanceRepository(db)
        val (business, a) = repo.createBusinessWithUnit("Jendela VM", "A")
        val b = repo.createProject(business, "B")
        val aBook = repo.books(a).first().first { it.mode == BookMode.REALIZATION.name }.id
        val bBook = repo.books(b).first().first { it.mode == BookMode.REALIZATION.name }.id
        val today = LocalDate.now()
        repo.inTransaction {
            repeat(200) { repo.recordEvent(EventDraft(business, a, aBook, FinancialEventType.CASH_EXPENSE, 1_000, today, "Biaya A", "Biaya")) }
            repo.recordEvent(EventDraft(business, a, aBook, FinancialEventType.CASH_EXPENSE, 2_000, today.minusDays(1), "Lama A", "Biaya"))
            repo.recordEvent(EventDraft(business, b, bBook, FinancialEventType.CASH_EXPENSE, 3_000, today, "Biaya B", "Biaya"))
        }
        val store = ViewModelStore()
        val vm = AppViewModel(repo, FinanceWorkbookImporter(), EncryptedBackupManager(db, File(context.cacheDir, "ledger-window.aabk")),
            GoogleIdentity(context), GoogleDriveBackupClient(), rememberedSelection = WorkspaceSelection(business, a))
        store.put("ledger", vm)
        val states = Channel<AppUiState>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.Main) { vm.uiState.collect { states.send(it) } }
        suspend fun awaitState(predicate: (AppUiState) -> Boolean): AppUiState = withTimeout(15_000) {
            while (true) {
                val state = states.receive()
                assertTrue("Cross-unit events", state.events.all { it.projectId == state.selectedProjectId })
                assertTrue("Cross-unit notes", state.sales.all { it.sale.projectId == state.selectedProjectId })
                if (predicate(state)) return@withTimeout state
            }
            @Suppress("UNREACHABLE_CODE") error("No state")
        }
        try {
            val initial = awaitState { it.selectedProjectId == a && it.events.size == 200 }
            assertTrue(initial.ledgerHasEarlier)
            vm.loadEarlierLedger()
            val older = awaitState { it.selectedProjectId == a && it.events.size == 201 }
            assertFalse(older.ledgerHasEarlier)
            val original = older.events.single { it.description == "Lama A" }
            assertEquals(2_000L, original.amountRupiah)
            vm.selectProject(b)
            awaitState { it.selectedProjectId == b && it.events.size == 1 }
            vm.selectProject(a)
            val returned = awaitState { it.selectedProjectId == a && it.events.size == 200 }
            assertTrue(returned.ledgerHasEarlier)
        } finally {
            collector.cancelAndJoin()
            withContext(Dispatchers.Main) { store.clear() }
            db.close()
        }
    }
}
