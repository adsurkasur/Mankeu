package com.mankeu.presentation

import com.mankeu.data.ContactEntity
import com.mankeu.data.SaleEntity
import com.mankeu.data.SaleLineEntity
import com.mankeu.data.SaleRecord
import com.mankeu.presentation.screens.CustomerBill
import com.mankeu.presentation.screens.customerBills
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Beranda's Tagihan card (U4): who owes what, grouped per customer, largest first. */
class CustomerBillsTest {
    private val today = LocalDate.of(2026, 10, 31)

    private fun contact(name: String) = ContactEntity(name, "b", name, name.lowercase(), createdAtEpochMillis = 0)

    private fun note(id: String, who: ContactEntity?, day: Int, total: Long) = SaleRecord(
        sale = SaleEntity(id, "b", "p", who?.id, LocalDate.of(2026, 10, day).toEpochDay(), createdAtEpochMillis = 0),
        lines = listOf(SaleLineEntity("$id-1", id, "k", "dus", "40", "1", total, total, 0)),
        payments = emptyList(),
        contact = who,
    )

    @Test
    fun groupsOpenNotesPerCustomerWithTheOldestNoteAge() {
        val a = contact("Toko A")
        val b = contact("Toko B")
        val bills = customerBills(
            listOf(
                note("1", a, 5, 300_000),
                note("2", b, 20, 250_000),
                note("3", b, 9, 250_000),
                note("4", null, 30, 40_000),
                note("5", a, 1, 0), // a zero note is not a bill
            ),
            today,
        )
        assertEquals(
            listOf(CustomerBill("Toko B", 500_000, 22), CustomerBill("Toko A", 300_000, 26), CustomerBill(null, 40_000, 1)),
            bills,
        )
    }
}
