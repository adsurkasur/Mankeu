package com.mankeu.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URLDecoder
import java.time.LocalDate

/** U5 / U-3: the plain text of Kirim nota and Kirim pengingat, and the WhatsApp link only for a real number. */
class ShareTextTest {
    private val words = ShareWords(
        noteTitle = "Nota dari %1\$s",
        customer = "Untuk %1\$s",
        date = "Tanggal %1\$s",
        total = "Total %1\$s",
        paid = "Sudah dibayar %1\$s",
        left = "Sisa %1\$s",
        due = "Jatuh tempo %1\$s",
        reminderTitle = "Halo %1\$s, ini tagihan dari %2\$s yang belum dibayar:",
        reminderTotal = "Total belum dibayar %1\$s",
        thanks = "Terima kasih.",
    )
    private val day = LocalDate.of(2026, 10, 30)

    private fun note(total: Long, paid: Long, due: LocalDate? = null) = ShareNote(
        date = day,
        lines = listOf(ShareLine("Keripik", "2", "dus", 300_000, 600_000)),
        total = total,
        paid = paid,
        due = due,
    )

    @Test
    fun anUnpaidNoteNamesItsLinesTotalWhatIsLeftAndTheDueDate() {
        val text = ShareText.note("Keripik Bu Sri", "Toko A", note(600_000, 200_000, day.plusDays(14)), words)
        assertEquals(
            listOf(
                "Nota dari Keripik Bu Sri",
                "Untuk Toko A",
                "Tanggal ${day.toDisplayDate()}",
                "",
                "2 dus Keripik × ${300_000L.toRupiah()} = ${600_000L.toRupiah()}",
                "",
                "Total ${600_000L.toRupiah()}",
                "Sudah dibayar ${200_000L.toRupiah()}",
                "Sisa ${400_000L.toRupiah()}",
                "Jatuh tempo ${day.plusDays(14).toDisplayDate()}",
                "",
                "Terima kasih.",
            ).joinToString("\n"),
            text,
        )
    }

    @Test
    fun aPaidNoteWithoutCustomerHasNoLeftOrDueLine() {
        val text = ShareText.note("Keripik Bu Sri", null, note(600_000, 600_000, day.plusDays(7)), words)
        assertEquals(false, text.contains("Sisa"))
        assertEquals(false, text.contains("Jatuh tempo"))
        assertEquals(false, text.contains("Untuk"))
        assertEquals(true, text.contains("Sudah dibayar ${600_000L.toRupiah()}"))
    }

    @Test
    fun aReminderListsOnlyOpenNotesAndTheirSum() {
        val text = ShareText.reminder(
            "Keripik Bu Sri", "Toko A",
            listOf(note(600_000, 200_000, day.plusDays(14)), note(300_000, 300_000), note(80_000, 0)),
            words,
        )
        assertEquals(
            listOf(
                "Halo Toko A, ini tagihan dari Keripik Bu Sri yang belum dibayar:",
                "",
                "${day.toDisplayDate()} · Sisa ${400_000L.toRupiah()} · Jatuh tempo ${day.plusDays(14).toDisplayDate()}",
                "${day.toDisplayDate()} · Sisa ${80_000L.toRupiah()}",
                "",
                "Total belum dibayar ${480_000L.toRupiah()}",
                "",
                "Terima kasih.",
            ).joinToString("\n"),
            text,
        )
    }

    @Test
    fun whatsAppLinkOnlyWhenAPhoneNumberExists() {
        assertNull(ShareText.whatsAppLink(null, "x"))
        assertNull(ShareText.whatsAppLink("", "x"))
        assertNull(ShareText.whatsAppLink("  -  ", "x"))
        assertNull(ShareText.whatsAppLink("123", "x"))
        // A local 08… number becomes 628…; +62 and 62 stay; spaces and dashes go.
        val link = ShareText.whatsAppLink("0812-3456 789", "Halo Toko A\nSisa Rp400.000")!!
        assertEquals("https://wa.me/628123456789?text=", link.substringBefore("?") + "?text=")
        assertEquals("Halo Toko A\nSisa Rp400.000", URLDecoder.decode(link.substringAfter("text="), "UTF-8"))
        assertEquals("https://wa.me/628123456789", ShareText.whatsAppLink("+62 812 3456 789", "a")!!.substringBefore("?"))
        assertEquals("https://wa.me/628123456789", ShareText.whatsAppLink("628123456789", "a")!!.substringBefore("?"))
    }
}
