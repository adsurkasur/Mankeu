package com.mankeu.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.data.AppDatabase
import com.mankeu.data.EventDraft
import com.mankeu.data.FinanceRepository
import com.mankeu.data.ProductDraft
import com.mankeu.data.RecipeLineDraft
import com.mankeu.data.SaleUnitDraft
import com.mankeu.domain.BookMode
import com.mankeu.domain.CategoryRole
import com.mankeu.domain.FinancialEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Two devices, one encrypted store (DEC-013). Runs the real engine, codec and cipher end to end;
 * only the transport is in memory.
 */
@RunWith(AndroidJUnit4::class)
class SyncEngineTest {
    private class Device {
        val database: AppDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(SyncSchema.callback)
            .allowMainThreadQueries()
            .build()
        val repository = FinanceRepository(database)
        val engine = SyncEngine(database)
    }

    private class MemoryTransport : SyncTransport {
        val files = mutableMapOf<String, Pair<RemoteChangeSet, ByteArray>>()
        override suspend fun list() = files.values.map { it.first }
        override suspend fun upload(deviceId: String, pushNumber: Long, encrypted: ByteArray) {
            val id = "$deviceId-$pushNumber"
            files[id] = RemoteChangeSet(id, deviceId, pushNumber) to encrypted
        }
        override suspend fun download(id: String) = files.getValue(id).second
    }

    private lateinit var a: Device
    private lateinit var b: Device
    private val store = MemoryTransport()
    private val passphrase = "sandi-sinkron-1".toCharArray()

    @Before
    fun setUp() = runBlocking {
        a = Device()
        b = Device()
        // Capture runs only while sync is on (schema 12, DEC-024).
        a.engine.enable()
        b.engine.enable()
    }

    @After
    fun tearDown() {
        a.database.close()
        b.database.close()
    }

    @Test
    fun changesTravelBothWaysWithoutEchoAndDeletesFollow() = runBlocking {
        val coordinatorA = SyncCoordinator(a.engine, store)
        val coordinatorB = SyncCoordinator(b.engine, store)
        val (businessId, projectId) = a.repository.run { createBusiness("Kedai").let { it to createProject(it, "Utama") } }
        val sale = a.record(businessId, projectId, FinancialEventType.CASH_INCOME, 750_000)

        assertTrue(coordinatorA.syncOnce(passphrase).pushed)
        val pulled = coordinatorB.syncOnce(passphrase)

        assertEquals(1, pulled.pulledSets)
        assertEquals(0, pulled.rejected)
        assertEquals(750_000L, b.cash(projectId))
        // B's own first round sent its (empty) full snapshot; applying A's changes queued nothing.
        assertFalse(coordinatorB.syncOnce(passphrase).pushed)

        b.record(businessId, projectId, FinancialEventType.CASH_EXPENSE, 50_000)
        coordinatorB.syncOnce(passphrase)
        coordinatorA.syncOnce(passphrase)
        assertEquals(700_000L, a.cash(projectId))

        a.repository.deleteEvent(sale)
        coordinatorA.syncOnce(passphrase)
        coordinatorB.syncOnce(passphrase)
        assertEquals(-50_000L, b.cash(projectId))
        assertTrue(b.database.financeDao().getEvent(sale) == null)
    }

    @Test
    fun aNewerUnpushedLocalEditWinsOverAnOlderRemoteOne() = runBlocking {
        val businessId = a.repository.createBusiness("Nama Awal")
        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        SyncCoordinator(b.engine, store).syncOnce(passphrase)

        a.repository.renameBusiness(businessId, "Nama dari A")
        val fromA = requireNotNull(a.engine.collectLocalChanges())
        b.repository.renameBusiness(businessId, "Nama dari B")
        // B's edit is the later one.
        b.database.openHelper.writableDatabase.execSQL("UPDATE sync_outbox SET changedAtEpochMillis = changedAtEpochMillis + 60000")

        val result = b.engine.applyRemote(fromA.changeSet)

        assertEquals(1, result.skippedOlder)
        assertEquals("Nama dari B", b.database.financeDao().getBusinesses().single().name)
    }

    @Test
    fun aWrongPassphraseNeverTouchesTheDatabase() = runBlocking {
        a.repository.createBusiness("Rahasia")
        SyncCoordinator(a.engine, store).syncOnce(passphrase)

        val failure = runCatching { SyncCoordinator(b.engine, store).syncOnce("bukan-sandinya".toCharArray()) }

        assertTrue(failure.isFailure)
        assertTrue(b.database.financeDao().getBusinesses().isEmpty())
    }

    /** Both phones add "Bensin Motor" offline and record with it; after sync each keeps one. */
    @Test
    fun theSameCategoryCreatedOfflineOnTwoPhonesIsMergedAndReferencesFollow() = runBlocking {
        val (businessId, projectId) = a.repository.run { createBusiness("Bengkel").let { it to createProject(it, "Utama") } }
        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        SyncCoordinator(b.engine, store).syncOnce(passphrase)
        val onA = a.repository.createCategory(businessId, "Bensin Motor", CategoryRole.EXPENSE)
        val onB = b.repository.createCategory(businessId, "bensin motor", CategoryRole.EXPENSE)
        val spentOnA = a.record(businessId, projectId, FinancialEventType.CASH_EXPENSE, 40_000, onA)
        val spentOnB = b.record(businessId, projectId, FinancialEventType.CASH_EXPENSE, 25_000, onB)

        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        val atB = SyncCoordinator(b.engine, store).syncOnce(passphrase)
        val atA = SyncCoordinator(a.engine, store).syncOnce(passphrase)

        assertEquals(1, atB.merged)
        assertEquals(1, atA.merged)
        assertEquals(0, atB.rejected + atA.rejected)
        for ((device, own) in listOf(a to onA, b to onB)) {
            val dao = device.database.financeDao()
            assertEquals(listOf(own), dao.getCategories(businessId).filter { it.normalizedName == "bensin motor" }.map { it.id })
            assertEquals(own, dao.getEvent(spentOnA)!!.categoryId)
            assertEquals(own, dao.getEvent(spentOnB)!!.categoryId)
            assertEquals(-65_000L, device.cash(projectId))
        }
    }

    /**
     * Final review I-5: both phones add "Minyak" offline and A buys some. On B, A's purchase must
     * point at B's own Minyak, so it prices B's material; that needs stock_items applied before
     * financial_events in one change set.
     */
    @Test
    fun aPurchaseOfATwinMaterialPointsAtTheLocalMaterialAfterSync() = runBlocking {
        val (businessId, projectId) = a.repository.run { createBusiness("Keripik").let { it to createProject(it, "Utama") } }
        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        SyncCoordinator(b.engine, store).syncOnce(passphrase)
        val onA = a.repository.createStockItem(businessId, projectId, false, "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        val onB = b.repository.createStockItem(businessId, projectId, false, "minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        val bookId = a.database.financeDao().getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        val purchase = a.repository.recordEvent(
            EventDraft(businessId, projectId, bookId, FinancialEventType.CASH_EXPENSE, 550_000, LocalDate.of(2026, 10, 20), "Minyak", "", quantityText = "20", unit = "liter", stockItemId = onA),
        )

        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        val atB = SyncCoordinator(b.engine, store).syncOnce(passphrase)

        assertEquals(1, atB.merged)
        assertEquals(onB, b.database.financeDao().getEvent(purchase)!!.stockItemId)
        assertEquals(27_500L, b.repository.materialPrice(onB))
    }

    /**
     * Cleanup P4: both phones add "Keripik" sold per "bungkus" with a "Bumbu" cost offline. After
     * sync each keeps one product with one "bungkus" and one "Bumbu"; a later edit on A (new unit,
     * new cost) arrives whole on B instead of losing the replaced recipe line.
     */
    @Test
    fun twinProductsMergeTheirSaleUnitsAndRecipeLinesAndLaterEditsFollow() = runBlocking {
        val (businessId, projectId) = a.repository.run { createBusiness("Keripik").let { it to createProject(it, "Utama") } }
        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        SyncCoordinator(b.engine, store).syncOnce(passphrase)
        fun draft(name: String, units: List<SaleUnitDraft>, bumbu: Long) = ProductDraft(name, "bungkus", "400", units, listOf(RecipeLineDraft("Bumbu", amountRupiah = bumbu)))
        val onA = a.repository.saveProduct(projectId, null, draft("Keripik", listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000)), 2_400_000))
        b.repository.saveProduct(projectId, null, draft("keripik", listOf(SaleUnitDraft(name = "Bungkus", contentText = "1", priceRupiah = 8_000)), 2_400_000))

        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        SyncCoordinator(b.engine, store).syncOnce(passphrase)
        SyncCoordinator(a.engine, store).syncOnce(passphrase)

        fun Device.shape() = runBlocking {
            val dao = database.financeDao()
            dao.getProducts(projectId).map { p ->
                Triple(p.name.lowercase(), dao.getProductUnits(p.id).sortedBy { it.sortOrder }.map { it.name.lowercase() }, dao.getRecipeLines(p.id).map { it.name to it.amountRupiah })
            }
        }
        for (device in listOf(a, b)) assertEquals(listOf(Triple("keripik", listOf("bungkus"), listOf("Bumbu" to 2_400_000L))), device.shape())

        val bungkus = a.database.financeDao().getProductUnits(onA).single()
        a.repository.saveProduct(projectId, onA, draft("Keripik", listOf(SaleUnitDraft(bungkus.id, "bungkus", "1", 8_000), SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000)), 2_500_000))
        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        val atB = SyncCoordinator(b.engine, store).syncOnce(passphrase)

        assertEquals(0, atB.rejected)
        assertEquals(listOf(Triple("keripik", listOf("bungkus", "dus"), listOf("Bumbu" to 2_500_000L))), b.shape())
    }

    /** DEC-029 §9: contacts merge by (business, name); a note naming A's twin points at B's own contact. */
    @Test
    fun theSameContactCreatedOnTwoPhonesIsMergedAndANoteFollows() = runBlocking {
        val (businessId, projectId) = a.repository.run { createBusiness("Keripik").let { it to createProject(it, "Utama") } }
        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        SyncCoordinator(b.engine, store).syncOnce(passphrase)
        fun Device.contact(id: String, name: String) = database.openHelper.writableDatabase.execSQL(
            "INSERT INTO contacts (id, businessId, name, normalizedName, isCustomer, isSupplier, archived, createdAtEpochMillis, updatedAtEpochMillis) VALUES (?, ?, ?, 'toko a', 1, 0, 0, 0, 0)",
            arrayOf<Any>(id, businessId, name),
        )
        a.contact("ca", "Toko A")
        b.contact("cb", "toko a")
        a.database.openHelper.writableDatabase.execSQL(
            "INSERT INTO sales (id, businessId, projectId, contactId, dateEpochDay, discountRupiah, notes, hppEstimated, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('nota', ?, ?, 'ca', 20000, 0, '', 0, 0, 0)",
            arrayOf<Any>(businessId, projectId),
        )

        SyncCoordinator(a.engine, store).syncOnce(passphrase)
        val atB = SyncCoordinator(b.engine, store).syncOnce(passphrase)

        assertEquals(1, atB.merged)
        b.database.openHelper.writableDatabase.query("SELECT (SELECT COUNT(*) FROM contacts), (SELECT contactId FROM sales WHERE id = 'nota')").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
            assertEquals("cb", c.getString(1))
        }
    }

    @Test
    fun nothingIsCapturedWhileSyncIsOff() = runBlocking {
        a.engine.disable()
        a.repository.createBusiness("Tanpa Sync")

        assertTrue(a.database.syncDao().pending().isEmpty())
    }

    private suspend fun Device.record(businessId: String, projectId: String, type: FinancialEventType, amount: Long, categoryId: String? = null): String {
        val bookId = database.financeDao().getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        return repository.recordEvent(
            EventDraft(businessId, projectId, bookId, type, amount, LocalDate.of(2026, 10, 20), "Uji $type", "", categoryId = categoryId),
        )
    }

    private suspend fun Device.cash(projectId: String): Long {
        val bookId = database.financeDao().getBooks(projectId).first { it.mode == BookMode.REALIZATION.name }.id
        return repository.accountingSummary(bookId).first().cashBalanceRupiah
    }
}
