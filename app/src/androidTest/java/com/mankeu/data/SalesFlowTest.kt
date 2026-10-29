package com.mankeu.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mankeu.domain.BookMode
import com.mankeu.domain.FinancialEventType
import com.mankeu.domain.ProductMovementType
import com.mankeu.domain.SaleStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Golden F2 criteria (plan Task 3): notes, payments, consignment and supplier credit through the
 * repository, checked by what the lists, stock and cash then show.
 */
@RunWith(AndroidJUnit4::class)
class SalesFlowTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: FinanceRepository
    private lateinit var businessId: String
    private lateinit var projectId: String
    private lateinit var bookId: String
    private lateinit var keripik: String
    private lateinit var kerupuk: String
    private val day = LocalDate.of(2026, 10, 5)

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = FinanceRepository(db)
        val (b, p) = repo.createBusinessWithUnit("Keripik Bu Sri", "Umum")
        businessId = b
        projectId = p
        bookId = repo.books(p).first().single { it.mode == BookMode.REALIZATION.name }.id
        keripik = repo.saveProduct(p, null, ProductDraft(
            name = "Keripik", baseUnit = "bungkus", batchYieldText = "400",
            units = listOf(SaleUnitDraft(name = "dus", contentText = "40", priceRupiah = 300_000), SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 8_000)),
            recipe = listOf(RecipeLineDraft("Bumbu & bahan", amountRupiah = 2_400_000)),
        ))
        kerupuk = repo.saveProduct(p, null, ProductDraft(
            name = "Kerupuk", baseUnit = "bungkus", batchYieldText = "100",
            units = listOf(SaleUnitDraft(name = "bungkus", contentText = "1", priceRupiah = 10_000)),
            recipe = listOf(RecipeLineDraft("Bahan", amountRupiah = 500_000)),
        ))
        repo.recordProduction(keripik, BigDecimal(2), LocalDate.of(2026, 10, 1), "")
        repo.recordProduction(kerupuk, BigDecimal.ONE, LocalDate.of(2026, 10, 1), "")
    }

    @After fun tearDown() = db.close()

    private suspend fun product(id: String) = repo.products(projectId).first().single { it.product.id == id }
    private suspend fun note(id: String) = repo.sales(projectId).first().single { it.sale.id == id }
    private suspend fun cash() = repo.accountingSummary(bookId).first().cashBalanceRupiah
    private fun assertQty(expected: Int, actual: BigDecimal) = assertEquals("$expected vs $actual", 0, BigDecimal(expected).compareTo(actual))
    private fun failure(block: suspend () -> Any): String? = runBlocking { runCatching { block() }.exceptionOrNull()?.message }

    @Test fun goldenNotesConsignmentAndSupplierCredit() = runBlocking {
        val tokoA = repo.saveContact(businessId, null, ContactDraft("Toko A", phone = "0812"))
        val tokoB = repo.saveContact(businessId, null, ContactDraft("Toko B"))
        val pemasok = repo.saveContact(businessId, null, ContactDraft("Pemasok Kemasan", isCustomer = false, isSupplier = true))
        assertEquals(listOf("Pemasok Kemasan", "Toko A", "Toko B"), repo.contacts(businessId).first().map { it.name })

        // Nota ke Toko A, 2 produk, belum dibayar -> belum, dan tampil di daftar tagihan.
        val notaA = repo.saveSale(projectId, null, SaleDraft(
            contactId = tokoA, date = day, dueDate = day.plusDays(14), discountRupiah = 100_000, notes = "",
            lines = listOf(SaleLineDraft(keripik, "dus", "5", 300_000), SaleLineDraft(kerupuk, "bungkus", "10", 10_000)),
            paidNowRupiah = 0,
        ))
        note(notaA).let {
            assertEquals(1_500_000L to SaleStatus.UNPAID, it.total to it.status)
            assertEquals(listOf(1_200_000L, 50_000L), it.lines.map { l -> l.hppRupiah })
            assertEquals("Toko A", it.contact?.name)
            assertTrue(it.payments.isEmpty())
        }
        assertEquals(listOf(notaA), repo.sales(projectId).first().filter { it.outstanding > 0 }.map { it.sale.id })
        assertQty(600, product(keripik).stock)

        // Bayar 1.000.000 -> sebagian; bayar sisanya -> lunas; bayar lebih ditolak.
        repo.recordSalePayment(notaA, 1_000_000, day.plusDays(1))
        assertEquals(SaleStatus.PARTIAL to 500_000L, note(notaA).let { it.status to it.outstanding })
        assertEquals("Pembayaran Rp600.000 melebihi sisa tagihan Rp500.000.", failure { repo.recordSalePayment(notaA, 600_000, day.plusDays(2)) })
        repo.recordSalePayment(notaA, 500_000, day.plusDays(2))
        assertEquals(SaleStatus.PAID to 0L, note(notaA).let { it.status to it.outstanding })
        assertEquals("Nota ini sudah lunas.", failure { repo.recordSalePayment(notaA, 1, day.plusDays(3)) })
        assertEquals(1_500_000L, cash())

        // Titip 10 dus ke Toko B -> stok turun 400, dititipkan 400.
        val titip = repo.sendConsignment(projectId, tokoB, day, listOf(ConsignmentLineDraft(keripik, "dus", "10", 280_000)))
        assertQty(200, product(keripik).stock)
        assertQty(400, product(keripik).consigned)

        // Laku 6 -> nota baru, stok tidak turun lagi, sisa titip 160.
        val lineId = repo.consignments(projectId).first().single().lines.single().line.id
        val laku = repo.reportConsignmentSold(titip, listOf(lineId to "6"), day.plusDays(7), 0)
        note(laku).let {
            assertEquals(titip, it.sale.consignmentId)
            assertEquals(tokoB, it.sale.contactId)
            assertEquals(1_680_000L to SaleStatus.UNPAID, it.total to it.status)
            assertEquals(1_440_000L, it.hpp)
        }
        assertQty(200, product(keripik).stock)
        assertQty(160, product(keripik).consigned)
        assertQty(160, repo.consignments(projectId).first().single().lines.single().remainingBase)
        assertQty(160, repo.consignments(projectId).first().balances().getValue(tokoB to keripik))
        assertEquals("Laku 5 dus Keripik melebihi sisa titipan 4 dus.", failure { repo.reportConsignmentSold(titip, listOf(lineId to "5"), day.plusDays(8), 0) })

        // Setor penuh -> nota lunas.
        repo.recordSalePayment(laku, 1_680_000, day.plusDays(9))
        assertEquals(SaleStatus.PAID, note(laku).status)

        // Tarik 4 -> stok +160, titipan 0.
        repo.returnConsignment(titip, lineId, "4", day.plusDays(10))
        assertQty(360, product(keripik).stock)
        assertQty(0, product(keripik).consigned)
        assertEquals("Tarik 1 dus Keripik melebihi sisa titipan 0 dus.", failure { repo.returnConsignment(titip, lineId, "1", day.plusDays(11)) })

        // Riwayat stok: sales from notes (not the consignment sell-through) plus send and return.
        assertEquals(
            listOf(ProductMovementType.PRODUCTION, ProductMovementType.SALE, ProductMovementType.CONSIGN_OUT, ProductMovementType.CONSIGN_RETURN),
            product(keripik).history.map { it.type },
        )

        // Belanja tempo 300.000 -> utang 300.000; bayar 100.000 -> sisa 200.000; kas berkurang hanya 100.000.
        val cashBefore = cash()
        val kemasan = db.financeDao().getCategories(businessId).single { it.name == "Bahan Baku & Stok" }
        val tempo = repo.recordEvent(EventDraft(businessId, projectId, bookId, FinancialEventType.CREDIT_PURCHASE, 300_000, day, "Kemasan", kemasan.name, categoryId = kemasan.id, contactId = pemasok, dueDate = day.plusDays(30)))
        assertEquals(cashBefore, cash())
        assertEquals(300_000L to day.plusDays(30), repo.payables(businessId).first().single().let { it.outstanding to it.purchase.dueDate })
        repo.recordSupplierPayment(tempo, 100_000, day.plusDays(3))
        repo.payables(businessId).first().single().let {
            assertEquals(200_000L to SaleStatus.PARTIAL, it.outstanding to it.status)
            assertEquals("Pemasok Kemasan", it.contact?.name)
            assertEquals(tempo, it.payments.single().settlesEventId)
        }
        assertEquals(cashBefore - 100_000, cash())
        assertEquals("Pembayaran Rp250.000 melebihi sisa utang Rp200.000.", failure { repo.recordSupplierPayment(tempo, 250_000, day.plusDays(4)) })
        assertEquals("Belanja tempo ini punya 1 pembayaran. Hapus pembayarannya dulu.", failure { repo.deleteEvent(tempo) })

        // Nota tanpa pelanggan dengan dibayar < total ditolak; lunas boleh tanpa pelanggan.
        val umum = SaleDraft(null, day, null, 0, "", listOf(SaleLineDraft(kerupuk, "bungkus", "2", 10_000)), paidNowRupiah = 10_000)
        assertEquals("Pilih pelanggan untuk nota yang belum lunas.", failure { repo.saveSale(projectId, null, umum) })
        val lunas = repo.saveSale(projectId, null, umum.copy(paidNowRupiah = 20_000))
        assertEquals(SaleStatus.PAID to null, note(lunas).let { it.status to it.contact })
        assertEquals(LocalDate.of(2026, 10, 5), note(lunas).payments.single().date)
    }

    @Test fun contactNamesAreUniqueWhateverTheirCaseAndNeedARole() = runBlocking {
        val toko = repo.saveContact(businessId, null, ContactDraft("Toko A"))
        assertEquals("Kontak \"toko a\" sudah ada.", failure { repo.saveContact(businessId, null, ContactDraft("toko a")) })
        assertEquals("Pilih minimal satu peran: pelanggan atau pemasok.", failure { repo.saveContact(businessId, null, ContactDraft("Toko C", isCustomer = false)) })
        repo.saveContact(businessId, toko, ContactDraft("TOKO A", phone = "0813", isSupplier = true))
        repo.archiveContact(toko)
        assertTrue(repo.contacts(businessId).first().isEmpty())
        // The archived contact holds the name; adding it again brings it back.
        assertEquals(toko, repo.saveContact(businessId, null, ContactDraft("Toko A")))
        assertEquals(listOf("Toko A"), repo.contacts(businessId).first().map { it.name })
    }

    @Test fun noteValidationAndEditKeepPayments() = runBlocking {
        val toko = repo.saveContact(businessId, null, ContactDraft("Toko A"))
        fun draft(vararg lines: SaleLineDraft, discount: Long = 0, paid: Long = 0) = SaleDraft(toko, day, null, discount, "", lines.toList(), paid)
        assertEquals("Isi minimal satu baris produk.", failure { repo.saveSale(projectId, null, draft()) })
        assertEquals("\"karung\" bukan satuan jual Keripik. Pilih: bungkus, dus.", failure { repo.saveSale(projectId, null, draft(SaleLineDraft(keripik, "karung", "1", 1))) })
        assertEquals("Jumlah Keripik harus angka lebih dari nol.", failure { repo.saveSale(projectId, null, draft(SaleLineDraft(keripik, "dus", "0", 300_000))) })
        assertEquals("Harga Keripik per dus harus lebih dari nol.", failure { repo.saveSale(projectId, null, draft(SaleLineDraft(keripik, "dus", "1", 0))) })
        assertEquals("Diskon harus antara Rp0 dan subtotal Rp300.000.", failure { repo.saveSale(projectId, null, draft(SaleLineDraft(keripik, "dus", "1", 300_000), discount = 300_001)) })
        assertEquals("Dibayar sekarang harus antara Rp0 dan total Rp300.000.", failure { repo.saveSale(projectId, null, draft(SaleLineDraft(keripik, "dus", "1", 300_000), paid = 300_001)) })

        val id = repo.saveSale(projectId, null, draft(SaleLineDraft(keripik, "Dus", "2", 300_000), paid = 250_000))
        assertEquals("dus", note(id).lines.single().unitName)
        assertQty(720, product(keripik).stock)
        // Edit: lines replaced, HPP at today's cost, payments kept; the total cannot drop below what was paid.
        repo.saveProduct(projectId, keripik, product(keripik).let { r ->
            ProductDraft(r.product.name, r.product.unit, r.product.batchYieldText, r.units.sortedBy { it.sortOrder }.map { SaleUnitDraft(it.id, it.name, it.contentText, it.priceRupiah) }, listOf(RecipeLineDraft("Bumbu & bahan", amountRupiah = 4_000_000)))
        })
        repo.saveSale(projectId, id, draft(SaleLineDraft(keripik, "bungkus", "30", 9_000), paid = 0))
        note(id).let {
            assertEquals(270_000L to 250_000L, it.total to it.paid)
            assertEquals(300_000L, it.lines.single().hppRupiah)
            assertFalse(it.sale.hppEstimated)
        }
        assertQty(770, product(keripik).stock)
        assertEquals("Total nota Rp200.000 lebih kecil dari yang sudah dibayar Rp250.000. Hapus pembayarannya dulu.", failure { repo.saveSale(projectId, id, draft(SaleLineDraft(keripik, "bungkus", "20", 10_000))) })
        assertEquals(1, repo.deleteSale(id))
        assertTrue(repo.sales(projectId).first().isEmpty())
        assertTrue(db.financeDao().getEventsForBook(bookId).isEmpty())
        assertQty(800, product(keripik).stock)
    }

    /** DEC-029 M4: an old-form product sale is a paid one-line note; Ubah and Hapus in the old form act on the whole sale. */
    @Test fun anOldFormProductSaleIsANoteThatFollowsItsPayment() = runBlocking {
        fun sale(qty: String, amount: Long) = EventDraft(businessId, projectId, bookId, FinancialEventType.CASH_INCOME, amount, day, "Jual", "Penjualan & Jasa", quantityText = qty, unit = "dus", productId = keripik)
        val payment = repo.recordEvent(sale("2", 600_000))
        note(LegacySaleShape.saleId(payment)).let {
            assertEquals(SaleStatus.PAID to 600_000L, it.status to it.total)
            assertEquals(480_000L, it.lines.single().hppRupiah)
            assertFalse(it.sale.hppEstimated)
            assertEquals(listOf(payment), it.payments.map { p -> p.id })
        }
        assertQty(720, product(keripik).stock)

        repo.updateEvent(payment, sale("3", 900_000))
        note(LegacySaleShape.saleId(payment)).let { assertEquals("3" to 900_000L, it.lines.single().quantityText to it.total) }
        assertQty(680, product(keripik).stock)

        repo.deleteEvent(payment)
        assertTrue(repo.sales(projectId).first().isEmpty())
        assertQty(800, product(keripik).stock)
    }

    /** DEC-029 M3: a note payment edited or deleted from the old form stays within the note and leaves the note. */
    @Test fun aNotePaymentEditedInTheOldFormStaysWithinTheNote() = runBlocking {
        val toko = repo.saveContact(businessId, null, ContactDraft("Toko A"))
        val id = repo.saveSale(projectId, null, SaleDraft(toko, day, null, 0, "", listOf(SaleLineDraft(keripik, "dus", "2", 300_000)), 0))
        val first = repo.recordSalePayment(id, 200_000, day)
        repo.recordSalePayment(id, 100_000, day)
        fun edit(amount: Long) = EventDraft(businessId, projectId, bookId, FinancialEventType.CASH_INCOME, amount, day, "Bayar", "Penjualan")

        assertEquals("Pembayaran Rp600.000 melebihi sisa tagihan nota Rp500.000.", failure { repo.updateEvent(first, edit(600_000)) })
        assertEquals("Ini pembayaran nota. Ubah produk dan jumlahnya lewat notanya.", failure { repo.updateEvent(first, edit(100_000).copy(productId = keripik, quantityText = "1", unit = "dus")) })
        assertEquals("Ini pembayaran nota; jenisnya tetap uang masuk.", failure { repo.updateEvent(first, edit(100_000).copy(type = FinancialEventType.CASH_EXPENSE)) })
        repo.updateEvent(first, edit(500_000))
        assertEquals(SaleStatus.PAID to id, note(id).status to db.financeDao().getEvent(first)!!.saleId)

        repo.deleteEvent(first)
        assertEquals(SaleStatus.PARTIAL to 500_000L, note(id).let { it.status to it.outstanding })
        assertQty(720, product(keripik).stock)
    }

    /** A migrated (estimated) note keeps itself when its payment is deleted from the old form (Task 2 ruling). */
    @Test fun deletingAMigratedPaymentLeavesTheNoteUnpaid() = runBlocking {
        val payment = repo.recordEvent(EventDraft(businessId, projectId, bookId, FinancialEventType.CASH_INCOME, 600_000, day, "Jual", "Penjualan & Jasa", quantityText = "2", unit = "dus", productId = keripik))
        db.openHelper.writableDatabase.execSQL("UPDATE sales SET hppEstimated = 1")

        repo.deleteEvent(payment)

        note(LegacySaleShape.saleId(payment)).let { assertEquals(SaleStatus.UNPAID to 600_000L, it.status to it.outstanding) }
        assertNull(db.financeDao().getEvent(payment))
    }

    @Test fun creditPurchaseRulesAndMaterialPrice() = runBlocking {
        val pemasok = repo.saveContact(businessId, null, ContactDraft("Pemasok", isCustomer = false, isSupplier = true))
        val minyak = repo.createStockItem(businessId, projectId, false, "Minyak", "liter", 25_000, BigDecimal.ZERO, BigDecimal.ZERO)
        val bahan = db.financeDao().getCategories(businessId).single { it.name == "Bahan Baku & Stok" }
        val modal = db.financeDao().getCategories(businessId).single { it.name == "Setoran Modal Pemilik" }
        fun tempo(amount: Long) = EventDraft(businessId, projectId, bookId, FinancialEventType.CREDIT_PURCHASE, amount, day, "Minyak", bahan.name, categoryId = bahan.id, contactId = pemasok, quantityText = "20", unit = "liter", stockItemId = minyak)

        assertEquals("Pilih pemasok untuk belanja yang dibayar nanti.", failure { repo.recordEvent(tempo(1).copy(contactId = null)) })
        assertEquals("Belanja dibayar nanti hanya untuk kategori biaya.", failure { repo.recordEvent(tempo(1).copy(categoryId = modal.id, category = modal.name)) })
        assertEquals("Jatuh tempo hanya untuk belanja yang dibayar nanti.", failure { repo.recordEvent(tempo(1).copy(type = FinancialEventType.CASH_EXPENSE, dueDate = day)) })

        val id = repo.recordEvent(tempo(600_000))
        assertEquals(30_000L, repo.materialPrice(minyak))
        repo.recordSupplierPayment(id, 200_000, day)
        assertEquals("Total belanja tidak boleh lebih kecil dari yang sudah dibayar Rp200.000.", failure { repo.updateEvent(id, tempo(150_000)) })
        assertEquals("Belanja tempo ini sudah ada pembayarannya. Hapus pembayarannya dulu untuk mengubah jenisnya.", failure { repo.updateEvent(id, tempo(600_000).copy(type = FinancialEventType.CASH_EXPENSE, contactId = null)) })
        repo.updateEvent(id, tempo(500_000).copy(dueDate = day.plusDays(7)))
        repo.payables(businessId).first().single().let {
            assertEquals(300_000L, it.outstanding)
            assertEquals(day.plusDays(7), it.purchase.dueDate)
            assertTrue(it.overdue(day.plusDays(8)))
            assertFalse(it.overdue(day.plusDays(7)))
        }
    }
}
