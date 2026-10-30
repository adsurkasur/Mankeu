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

    /** Review m2/m4: a payment keeps its note's or purchase's book and unit, and is not dated before it. */
    @Test fun paymentsKeepTheirBookAndAreNotDatedBeforeTheirNoteOrPurchase() = runBlocking {
        val toko = repo.saveContact(businessId, null, ContactDraft("Toko A", isSupplier = true))
        val plan = repo.books(projectId).first().single { it.mode == BookMode.PROJECTION.name }.id
        val id = repo.saveSale(projectId, null, SaleDraft(toko, day, null, 0, "", listOf(SaleLineDraft(keripik, "dus", "1", 300_000)), 0))
        assertEquals("Tanggal bayar tidak boleh sebelum tanggal nota.", failure { repo.recordSalePayment(id, 100_000, day.minusDays(1)) })
        val payment = repo.recordSalePayment(id, 100_000, day)
        val edit = EventDraft(businessId, projectId, bookId, FinancialEventType.CASH_INCOME, 100_000, day, "Bayar", "Penjualan")
        assertEquals("Ini pembayaran nota; buku dan unitnya tetap.", failure { repo.updateEvent(payment, edit.copy(bookId = plan)) })
        assertEquals("Tanggal bayar tidak boleh sebelum tanggal nota.", failure { repo.updateEvent(payment, edit.copy(date = day.minusDays(1))) })
        // Task 4: nor can the note itself move to after a payment it already has.
        val noteDraft = SaleDraft(toko, day.plusDays(1), null, 0, "", listOf(SaleLineDraft(keripik, "dus", "1", 300_000)), 0)
        assertEquals("Tanggal bayar tidak boleh sebelum tanggal nota. Ubah atau hapus pembayarannya dulu.", failure { repo.saveSale(projectId, id, noteDraft) })
        assertEquals(day.toEpochDay(), note(id).sale.dateEpochDay)
        repo.saveSale(projectId, id, noteDraft.copy(date = day)) // the payment's own day is fine

        val bahan = db.financeDao().getCategories(businessId).single { it.name == "Bahan Baku & Stok" }
        val tempo = repo.recordEvent(EventDraft(businessId, projectId, bookId, FinancialEventType.CREDIT_PURCHASE, 300_000, day, "Kemasan", bahan.name, categoryId = bahan.id, contactId = toko))
        assertEquals("Tanggal bayar tidak boleh sebelum tanggal belanja.", failure { repo.recordSupplierPayment(tempo, 100_000, day.minusDays(1)) })
        val paid = repo.recordSupplierPayment(tempo, 100_000, day)
        val payEdit = EventDraft(businessId, projectId, bookId, FinancialEventType.CASH_EXPENSE, 100_000, day, "Bayar utang", bahan.name, categoryId = bahan.id)
        assertEquals("Ini pembayaran utang; buku dan unitnya tetap.", failure { repo.updateEvent(paid, payEdit.copy(bookId = plan)) })
        assertEquals("Tanggal bayar tidak boleh sebelum tanggal belanja.", failure { repo.updateEvent(paid, payEdit.copy(date = day.minusDays(1))) })
    }

    /**
     * Task 4 golden (Task 1 numbers, DEC-029 §4): profit when sold through the repository. October:
     * note A 3.800.000 − 100.000 discount (HPP 3.000.000), consignment sell-through 1.680.000 (HPP
     * 1.440.000), other income 50.000, variable spend 4.900.000 cash + 300.000 on credit, fixed
     * 1.200.000, and a Rp3,6 jt asset bought in September (36 months).
     */
    @Test fun goldenProfitWhenSoldSummary() = runBlocking {
        val dao = db.financeDao()
        val categories = dao.getCategories(businessId)
        fun role(role: com.mankeu.domain.CategoryRole) = categories.first { it.role == role.name }
        // F2-8: bahan is counted in HPP by default; kemasan is flagged by the owner; rent and power are not.
        val bahan = categories.single { it.name == "Bahan Baku & Stok" }
        assertTrue(bahan.includedInHpp)
        val kemasanId = repo.createCategory(businessId, "Kemasan", com.mankeu.domain.CategoryRole.EXPENSE, costNature = com.mankeu.domain.CostNature.VARIABLE)
        repo.setCategoryIncludedInHpp(kemasanId, true)
        val kemasan = dao.getCategory(kemasanId)!!
        val sewa = categories.single { it.name == "Sewa Tempat" }
        val listrik = categories.single { it.name == "Listrik, Air & Internet" }
        assertFalse(sewa.includedInHpp || listrik.includedInHpp)
        assertEquals("Hanya kategori pengeluaran yang bisa dihitung di HPP.", failure { repo.setCategoryIncludedInHpp(role(com.mankeu.domain.CategoryRole.REVENUE).id, true) })
        suspend fun event(type: FinancialEventType, amount: Long, date: LocalDate, category: CategoryEntity, nature: com.mankeu.domain.CostNature? = null, life: Int? = null, contact: String? = null) =
            repo.recordEvent(EventDraft(businessId, projectId, bookId, type, amount, date, category.name, category.name, categoryId = category.id, costNature = nature, usefulLifeMonths = life, contactId = contact))
        val toko = repo.saveContact(businessId, null, ContactDraft("Toko B"))
        val pemasok = repo.saveContact(businessId, null, ContactDraft("Pemasok", isCustomer = false, isSupplier = true))

        event(FinancialEventType.OWNER_CAPITAL, 10_000_000, LocalDate.of(2026, 9, 1), role(com.mankeu.domain.CategoryRole.CAPITAL_IN))
        event(FinancialEventType.ASSET_PURCHASE_CASH, 3_600_000, LocalDate.of(2026, 9, 10), role(com.mankeu.domain.CategoryRole.ASSET), life = 36)
        // Keripik HPP is Rp6.000 a bungkus (Rp2,4 jt / 400): 500 bungkus = HPP 3.000.000.
        val notaA = repo.saveSale(projectId, null, SaleDraft(toko, day, null, 100_000, "", listOf(SaleLineDraft(keripik, "bungkus", "500", 7_600)), 0))
        repo.recordSalePayment(notaA, 2_000_000, day.plusDays(1))
        val titip = repo.sendConsignment(projectId, toko, day, listOf(ConsignmentLineDraft(keripik, "dus", "6", 280_000)))
        val line = repo.consignments(projectId).first().single().lines.single().line.id
        repo.reportConsignmentSold(titip, listOf(line to "6"), LocalDate.of(2026, 10, 12), 0)
        event(FinancialEventType.CASH_INCOME, 50_000, LocalDate.of(2026, 10, 8), role(com.mankeu.domain.CategoryRole.REVENUE))
        event(FinancialEventType.CASH_EXPENSE, 4_900_000, LocalDate.of(2026, 10, 3), bahan)
        val tempo = event(FinancialEventType.CREDIT_PURCHASE, 300_000, LocalDate.of(2026, 10, 4), kemasan, contact = pemasok)
        repo.recordSupplierPayment(tempo, 100_000, LocalDate.of(2026, 10, 20))
        event(FinancialEventType.CASH_EXPENSE, 1_000_000, LocalDate.of(2026, 10, 2), sewa)
        event(FinancialEventType.CASH_EXPENSE, 200_000, LocalDate.of(2026, 10, 2), listrik)

        // October, as Beranda's and Laporan's "this month" card reads it.
        val inputs = repo.profitInputs(bookId).first()
        val october = inputs.months().single { it.month == java.time.YearMonth.of(2026, 10) }
        assertEquals(-310_000L, october.profitRupiah)
        assertEquals(listOf(5_380_000L, 50_000L, 4_440_000L, 1_200_000L, 100_000L, 5_200_000L, 760_000L),
            october.let { listOf(it.salesRupiah, it.otherIncomeRupiah, it.hppRupiah, it.fixedCostRupiah, it.depreciationRupiah, it.productionSpendRupiah, it.productionVarianceRupiah) })
        val card = com.mankeu.domain.ActualBepCalculator.analyse(PlanAssembly.products(repo.products(projectId).first(), emptyMap()), PlanAssembly.actualMonth(october, repo.products(projectId).first()))
        assertEquals(-310_000L, card.profitRupiah)
        assertQty(740, card.unitsSold.getValue(keripik).multiply(BigDecimal(40))) // 500 bungkus + 6 dus, in dus

        // The whole book, seen in October 2026: September adds one month of depreciation (review m-4: the month is pinned).
        val wear = 200_000L
        val october2026 = FinanceRepository(db, currentMonth = { java.time.YearMonth.of(2026, 10) })
        val s = october2026.accountingSummary(bookId).first()
        // U2: profit exists per month only; the all-time summary carries no profit lines for Realisasi.
        assertEquals(listOf(0L, 0L, 0L), listOf(s.operatingIncomeRupiah, s.operatingExpenseRupiah, s.profitRupiah))
        val allMonths = october2026.monthlySummaries(bookId).first()
        assertEquals(5_430_000L, allMonths.sumOf { it.incomeRupiah })
        assertEquals(4_440_000L + 1_200_000L + wear, allMonths.sumOf { it.hppRupiah + it.otherCostRupiah })
        assertEquals(-410_000L, allMonths.sumOf { it.profitRupiah })
        assertEquals(wear, allMonths.sumOf { it.profit.depreciationRupiah })
        // Seen in December, the asset has worn two more months (F2-8 fix round: through the current month).
        assertEquals(-610_000L, FinanceRepository(db, currentMonth = { java.time.YearMonth.of(2026, 12) }).monthlySummaries(bookId).first().sumOf { it.profitRupiah })
        // Komparasi's cash basis (review I-1): note payment + other income in; cash spend + supplier payment out.
        assertEquals(2_050_000L to 6_200_000L, s.operatingCashInRupiah to s.operatingCashOutRupiah)
        assertEquals(5_380_000L - 2_000_000L, s.receivableRupiah)
        assertEquals(200_000L, s.payableRupiah)
        // Cash counts only cash: note payment 2 jt + other income − cash spend − supplier payment.
        assertEquals(2_000_000L + 50_000L - 4_900_000L - 1_200_000L - 100_000L, s.operatingCashFlowRupiah)
        assertEquals(-3_600_000L, s.investingCashFlowRupiah)
        assertEquals(10_000_000L, s.financingCashFlowRupiah)
        assertEquals(10_000_000L - 3_600_000L - 4_150_000L, s.cashBalanceRupiah)
        // The cost chart: credit purchase at purchase date, the supplier payment not again.
        assertEquals(4_900_000L + 300_000L + 1_200_000L, repo.categoryTotals(bookId).first().sumOf { it.totalRupiah })
        // PDF and xlsx read the same summary.
        val exported = october2026.buildProjectExportBundle(projectId).books.single { it.book.mode == BookMode.REALIZATION.name }
        assertEquals(s, exported.summary)

        // U2: the one monthly summary, which every screen and the export show for October.
        val m = october2026.monthlySummary(bookId).first()
        assertEquals(october, m.profit)
        assertEquals(listOf(-310_000L, 4_440_000L, 1_300_000L, 5_200_000L, 760_000L), listOf(m.profitRupiah, m.hppRupiah, m.otherCostRupiah, m.productionSpendRupiah, m.productionVarianceRupiah))
        assertEquals(listOf(2_050_000L, 6_200_000L, s.cashBalanceRupiah, s.receivableRupiah, s.payableRupiah), listOf(m.moneyInRupiah, m.moneyOutRupiah, m.cashOnHandRupiah, m.receivableRupiah, m.payableRupiah))
        assertEquals(m.profitRupiah, card.profitRupiah)
        assertEquals(m, exported.month)
        assertEquals(listOf(9, 10), october2026.monthlySummaries(bookId).first().map { it.month.monthValue })
        assertEquals(m, october2026.monthlySummaries(bookId).first().last())

        // F2-8: transport is variable but not a production cost, so it reduces profit directly.
        event(FinancialEventType.CASH_EXPENSE, 150_000, LocalDate.of(2026, 10, 15), categories.single { it.name == "Transportasi" })
        val withTransport = repo.profitInputs(bookId).first().months().single { it.month == java.time.YearMonth.of(2026, 10) }
        assertEquals(-460_000L, withTransport.profitRupiah)
        assertEquals(5_200_000L, withTransport.productionSpendRupiah)
        assertEquals(-410_000L - 150_000L, october2026.monthlySummaries(bookId).first().sumOf { it.profitRupiah })
    }
}
