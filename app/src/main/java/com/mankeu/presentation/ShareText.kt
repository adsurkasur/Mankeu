package com.mankeu.presentation

import java.time.LocalDate

/** The words of a shared note, from string resources (ID / EN); kept apart so the text is testable on the JVM. */
data class ShareWords(
    val noteTitle: String,
    val customer: String,
    val date: String,
    val total: String,
    val paid: String,
    val left: String,
    val due: String,
    val reminderTitle: String,
    val reminderTotal: String,
    val thanks: String,
)

data class ShareLine(val product: String, val quantity: String, val unit: String, val unitPriceRupiah: Long, val totalRupiah: Long)

data class ShareNote(val date: LocalDate, val lines: List<ShareLine>, val total: Long, val paid: Long, val due: LocalDate?) {
    val outstanding: Long get() = (total - paid).coerceAtLeast(0)
}

/**
 * Kirim (U-3, DEC-030 §8): a note or a customer's open bills as plain text for the Android share
 * sheet, and the WhatsApp link when the contact has a number. Nothing leaves the phone without the
 * owner choosing an app.
 */
object ShareText {
    fun note(business: String, customer: String?, note: ShareNote, w: ShareWords): String = buildList {
        add(w.noteTitle.format(business))
        customer?.let { add(w.customer.format(it)) }
        add(w.date.format(note.date.toDisplayDate()))
        add("")
        note.lines.forEach { add("${it.quantity} ${it.unit} ${it.product} × ${it.unitPriceRupiah.toRupiah()} = ${it.totalRupiah.toRupiah()}") }
        add("")
        add(w.total.format(note.total.toRupiah()))
        if (note.paid > 0) add(w.paid.format(note.paid.toRupiah()))
        if (note.outstanding > 0) {
            add(w.left.format(note.outstanding.toRupiah()))
            note.due?.let { add(w.due.format(it.toDisplayDate())) }
        }
        add("")
        add(w.thanks)
    }.joinToString("\n")

    fun reminder(business: String, customer: String, notes: List<ShareNote>, w: ShareWords): String {
        val open = notes.filter { it.outstanding > 0 }
        return buildList {
            add(w.reminderTitle.format(customer, business))
            add("")
            open.forEach { n ->
                add(listOfNotNull(n.date.toDisplayDate(), w.left.format(n.outstanding.toRupiah()), n.due?.let { w.due.format(it.toDisplayDate()) }).joinToString(" · "))
            }
            add("")
            add(w.reminderTotal.format(open.sumOf { it.outstanding }.toRupiah()))
            add("")
            add(w.thanks)
        }.joinToString("\n")
    }

    /** `https://wa.me/62…?text=…` for an Indonesian number (08… → 628…), null when there is no usable number. */
    fun whatsAppLink(phone: String?, text: String): String? {
        val digits = phone.orEmpty().filter(Char::isDigit)
        val international = when {
            digits.startsWith("0") -> "62" + digits.drop(1)
            else -> digits
        }
        if (international.length < 8) return null
        return "https://wa.me/$international?text=" + java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20")
    }
}
