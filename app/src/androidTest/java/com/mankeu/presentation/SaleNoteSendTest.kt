package com.mankeu.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mankeu.R
import com.mankeu.data.ContactEntity
import com.mankeu.data.ProductEntity
import com.mankeu.data.ProductRecord
import com.mankeu.data.SaleEntity
import com.mankeu.data.SaleLineEntity
import com.mankeu.data.SaleRecord
import com.mankeu.presentation.screens.SaleNoteDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * U5 / U-3: Kirim on a note hands the right text to the share seam ([SaleNoteDetailScreen]'s
 * `onSend`), and offers WhatsApp only when the customer has a phone number.
 */
@RunWith(AndroidJUnit4::class)
class SaleNoteSendTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun t(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val keripik = ProductRecord(ProductEntity(id = "k", projectId = "p", name = "Keripik", unit = "bungkus", batchYieldText = "400", createdAtEpochMillis = 0), emptyList(), emptyList())

    private fun note(phone: String?): SaleRecord {
        val contact = ContactEntity(id = "c", businessId = "b", name = "Toko A", normalizedName = "toko a", phone = phone, createdAtEpochMillis = 0)
        return SaleRecord(
            sale = SaleEntity("s", "b", "p", "c", LocalDate.now().toEpochDay(), LocalDate.now().plusDays(14).toEpochDay(), createdAtEpochMillis = 0),
            lines = listOf(SaleLineEntity("l", "s", "k", "dus", "40", "2", 300_000, 600_000, 480_000)),
            payments = emptyList(),
            contact = contact,
        )
    }

    private fun show(record: SaleRecord, onSend: (String, String?) -> Unit) {
        compose.setContent {
            MankeuTheme {
                SaleNoteDetailScreen(record = record, products = listOf(keripik), onPay = { _, _, _, _ -> }, onEdit = {}, onDelete = {}, businessName = "Keripik Bu Sri", onSend = onSend)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun withoutAPhoneKirimGoesStraightToTheShareSheetWithTheNoteText() {
        val sent = mutableListOf<Pair<String, String?>>()
        show(note(phone = null)) { text, link -> sent += text to link }
        compose.onNodeWithText(t(R.string.share_action)).performClick()
        compose.waitForIdle()
        assertEquals(1, sent.size)
        val (text, link) = sent.single()
        assertNull(link)
        assertTrue(text, text.contains("Keripik Bu Sri"))
        assertTrue(text, text.contains("2 dus Keripik"))
        assertTrue(text, text.contains("600.000"))
        assertTrue(text, text.contains(LocalDate.now().plusDays(14).toDisplayDate()))
        assertEquals(0, compose.onAllNodesWithText(t(R.string.share_whatsapp)).fetchSemanticsNodes().size)
    }

    @Test
    fun withAPhoneKirimOffersWhatsAppToThatNumberOrAnotherApp() {
        val sent = mutableListOf<Pair<String, String?>>()
        show(note(phone = "0812-3456-789")) { text, link -> sent += text to link }
        compose.onNodeWithText(t(R.string.share_action)).performClick()
        compose.onNodeWithText(t(R.string.share_whatsapp)).performClick()
        compose.waitForIdle()
        val (text, link) = sent.single()
        assertTrue(link!!, link.startsWith("https://wa.me/628123456789?text="))
        assertTrue(text.contains("Toko A"))

        compose.onNodeWithText(t(R.string.share_action)).performClick()
        compose.onNodeWithText(t(R.string.share_other_app)).performClick()
        compose.waitForIdle()
        assertNull(sent.last().second)
    }
}
